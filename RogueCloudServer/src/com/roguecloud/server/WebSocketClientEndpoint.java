/*
 * Copyright 2018 IBM Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
*/

package com.roguecloud.server;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roguecloud.RCRuntime;
import com.roguecloud.RCSharedConstants;
import com.roguecloud.RoundScope;
import com.roguecloud.ServerInstance;
import com.roguecloud.ServerInstanceList;
import com.roguecloud.db.DatabaseInstance;
import com.roguecloud.db.DbUser;
import com.roguecloud.json.JsonClientConnect;
import com.roguecloud.json.JsonClientConnectResponse;
import com.roguecloud.json.JsonClientConnectResponse.ConnectResult;
import com.roguecloud.server.ActiveWSClient.ViewType;
import com.roguecloud.server.ActiveWSClientSession.Type;
import com.roguecloud.utils.CompressionUtils;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.ResourceLifecycleUtil;

/** 
 * Messages received on this WebSocket endpoint are received as byte[]. This byte array is a deflate-compressed text string,
 * which is first inflated, then further processed by handleMessageInner(...).
 **/
@ServerEndpoint("/api/client")
public class WebSocketClientEndpoint {

	private static final Logger log = Logger.getInstance();

	private final RCServerUtilLatencySim latencySim;
	
	public WebSocketClientEndpoint() {
		
		if(RCRuntime.ENABLE_LATENCY_SIM) {
			latencySim = new RCServerUtilLatencySim();
		} else {
			latencySim = null;
		}
	}
	
	@OnOpen
	public void open(Session session) {
		System.out.println("open.");
		
		session.setMaxIdleTimeout(2 * TimeUnit.MILLISECONDS.convert(RCSharedConstants.MAX_ROUND_LENGTH_IN_NANOS, TimeUnit.NANOSECONDS));
		
		// Convert the session to a 'managed resource', so that it will automatically be disposed of once it expires.
		ResourceLifecycleUtil.getInstance().addNewSession(ServerWsClientUtil.convertSessionToManagedResource(session));
	}

	@OnClose
	public void close(Session session) {
		System.out.println("close.");
		
		// If the websocket was added as a managed resource, it can now be removed. 
		ResourceLifecycleUtil.getInstance().removeSession(ServerWsClientUtil.convertSessionToManagedResource(session));
	}

	@OnError
	public void onError(Throwable error) {
		System.err.println("Web Socket on error:");
		error.printStackTrace();
	}

	@OnMessage
	public void handleMessage(String message, Session session) {
		try {
			handleMessageInner(message, session);
		} catch (Exception e) {
			log.severe("Unexpected error in handleMessage",  e,  null);
		}
	}

	@OnMessage
	public void handleMessage(byte[] message, Session session) {
		try {
			handleMessageInner(CompressionUtils.decompressToString(message), session);
		} catch (Exception e) {
			log.severe("Unexpected error in handleMessage",  e,  null);
		}
	}

	private void handleMessageInner(String message, Session session) throws JsonParseException, JsonMappingException, IOException {

		ServerInstance si = ServerInstanceList.getInstance().getServerInstance();
		
		RoundScope roundScope = ServerInstanceList.getInstance().getServerInstance().getCurrentRound();
		
		ActiveWSClientList clients = roundScope.getActiveClients();
		
		ActiveWSClient client = clients.getClientBySession(session);
		
		// If this session is not associated with a client object yet, then perform the connection protocol
		if(client == null) {
			ObjectMapper om = new ObjectMapper();
			
			String username;
			Long userId;
			
			JsonClientConnect jcc = om.readValue(message, JsonClientConnect.class);
		
			// The client API version must match the server API version
			if(jcc.getClientVersion() == null || !jcc.getClientVersion().trim().equals(RCSharedConstants.CLIENT_API_VERSION)) {
				JsonClientConnectResponse response = new JsonClientConnectResponse(ConnectResult.FAIL_INVALID_CLIENT_API_VERSION.name(), null); 
				session.getBasicRemote().sendBinary(CompressionUtils.compressToByteBuffer(om.writeValueAsString(response)));
//				session.getBasicRemote().sendText(om.writeValueAsString(response));
				session.close();
				return;				
			}
			
			if(jcc.getUuid() == null) {
				JsonClientConnectResponse response = new JsonClientConnectResponse(ConnectResult.FAIL_OTHER.name(), null); 
				session.getBasicRemote().sendBinary(CompressionUtils.compressToByteBuffer(om.writeValueAsString(response)));
//				session.getBasicRemote().sendText(om.writeValueAsString(response));
				session.close();
				return;
			}
			
			// Verify the user credentials
			if(!DatabaseInstance.get().isValidPasswordForUser(jcc.getUsername(), jcc.getPassword())) {
				// Invalid 
				JsonClientConnectResponse response = new JsonClientConnectResponse(ConnectResult.FAIL_INVALID_CREDENTIALS.name(), null);	
				session.getBasicRemote().sendBinary(CompressionUtils.compressToByteBuffer(om.writeValueAsString(response)));
//				session.getBasicRemote().sendText(om.writeValueAsString(response));
				session.close();
				return;
			}
			
			username = jcc.getUsername();
			
			DbUser userDb = DatabaseInstance.get().getUserByUsername(username);
			if(userDb != null) {
				userId = userDb.getUserId();
			} else {
				log.severe("Unable to find user '"+username+"' in user database", null);
				return;
			}
			
			
			RoundScope scope = si.getCurrentRound();
			if(scope == null || scope.isRoundComplete()) {
				// Round itself is not ready
				JsonClientConnectResponse response = new JsonClientConnectResponse(ConnectResult.FAIL_ROUND_NOT_STARTED.name(), null);	
				session.getBasicRemote().sendBinary(CompressionUtils.compressToByteBuffer(om.writeValueAsString(response)));
//				session.getBasicRemote().sendText(om.writeValueAsString(response));
				session.close();
				return;
				
			} else if(jcc.getRoundToEnter() != null) {
				// A round is running, and the user has specified a specific round to enter
				
				if(jcc.getRoundToEnter() != scope.getRoundId() ) {
					// The user has specified a round which is over.
					JsonClientConnectResponse response = new JsonClientConnectResponse(ConnectResult.FAIL_ROUND_OVER.name(), null);	
					session.getBasicRemote().sendBinary(CompressionUtils.compressToByteBuffer(om.writeValueAsString(response)));
//					session.getBasicRemote().sendText(om.writeValueAsString(response));
					session.close();
					return;						
				} else {
					// The current round matches the requested round, so they may enter.
				}
				
			} else {
				// User has not specified a round to enter, so they may enter the current round.
			}
			
			ActiveWSClientSession acs = new ActiveWSClientSession(Type.CLIENT, ViewType.CLIENT_VIEW, jcc.getUuid(), session, 
					si.getCurrentRound(), System.nanoTime(), jcc.isInitialConnect(), LogContext.serverInstance(si.getId()));
			
			client = clients.addSession(username, userId, acs);
			
			if(client != null) {  
			
				if(jcc.getLastActionResponseReceived() != null && jcc.getLastActionResponseReceived() >= 0) {
					client.setLastActionMessageIdReceived(jcc.getLastActionResponseReceived());
				}
				
				JsonClientConnectResponse response = new JsonClientConnectResponse(JsonClientConnectResponse.ConnectResult.SUCCESS.name(), scope.getRoundId());
				acs.writeToClientAsync(om.writeValueAsString(response));
			} else {
				// TODO: LOW - Dispose after some period of time, in the failing case.
				JsonClientConnectResponse response = new JsonClientConnectResponse(JsonClientConnectResponse.ConnectResult.FAIL_OTHER.name(), scope.getRoundId());
				acs.writeToClientAsync(om.writeValueAsString(response));				
			}
				
		} else {
			ActiveWSClientSession acs = client.getAWSClientSession(session);			
			
			if(latencySim != null) {
				latencySim.addMessage(acs, client, message);
			} else {
				client.getMessageReceiver().receiveMessage(message, acs, client);
			}
		}

	}
}
