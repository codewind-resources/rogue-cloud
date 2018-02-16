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

import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roguecloud.ServerInstance;
import com.roguecloud.ServerInstanceList;
import com.roguecloud.db.DatabaseInstance;
import com.roguecloud.json.browser.JsonBrowserConnect;
import com.roguecloud.server.ActiveWSClient.ViewType;
import com.roguecloud.server.ActiveWSClientSession.Type;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.ResourceLifecycleUtil;
import com.roguecloud.utils.ServerUtil;

@ServerEndpoint("/api/browser")
public class WebSocketBrowserEndpoint {

	private static final Logger log = Logger.getInstance();
	
	@OnOpen
	public void open(Session session) {
		System.out.println("open.");
		
		ResourceLifecycleUtil.getInstance().addNewSession(ServerWsClientUtil.convertSessionToManagedResource(session));
		
	}

	@OnClose
	public void close(Session session) {
		System.out.println("close.");
		ResourceLifecycleUtil.getInstance().removeSession(ServerWsClientUtil.convertSessionToManagedResource(session));
	}

	@OnError
	public void onError(Throwable error) {
		System.err.println("Web Socket on error:");
		error.printStackTrace();
	}

	@OnMessage
	public void handleMessage(String message, Session session) {
		
		ObjectMapper om = new ObjectMapper();
		try {
			String messageType = (String) om.readValue(message, Map.class).get("type");

			if(messageType.equals(JsonBrowserConnect.TYPE) ) {
				JsonBrowserConnect jbc = (JsonBrowserConnect)om.readValue(message, JsonBrowserConnect.class);

				ViewType viewType = ViewType.valueOf(jbc.getViewType());
				
				if(viewType == ViewType.CLIENT_VIEW) {
					log.severe("Unsupported view on server", null);
					return;
				}

				ServerInstance si = ServerInstanceList.getInstance().getServerInstance();

				boolean isAdmin = ServerUtil.isAdminAuthenticatedAndAuthorized(jbc.getUsername(), jbc.getPassword());
				
				Long userId = 0l;
				String username;
				{
					// Verify the user credentials (not required for admin)
					if(!isAdmin && !DatabaseInstance.get().isValidPasswordForUser(jbc.getUsername(), jbc.getPassword())) {
						log.err("Invalid password provided for user: "+jbc.getUsername()+" password-hash:"+jbc.getPassword().hashCode(), null);
						return;
					}
					
					username = jbc.getUsername();
					
				}
				
				if(viewType.isAdminRequired() && !isAdmin)    {
					log.severe("Attempt to use admin functions without admin login/password", null);
					return;
				}
				
				log.info("Creating new AWSClientSession:"+jbc.getUuid(), null);
								
				ActiveWSClientSession acs = new ActiveWSClientSession(Type.BROWSER, viewType, jbc.getUuid(), session, si.getCurrentRound(), System.nanoTime(), false, LogContext.serverInstance(si.getId()));
				
				si.getCurrentRound().getActiveClients().addSession(username, userId, acs);
				
			} else {
				log.severe("Unrecognized message type: "+messageType+" msg: "+message, null);
				return;
			}
			

		} catch (Exception e) {
			e.printStackTrace();
			log.severe("Unexpected exception in WebSocketBrowserEndpoint", e, null);
		}
		
	}
}
