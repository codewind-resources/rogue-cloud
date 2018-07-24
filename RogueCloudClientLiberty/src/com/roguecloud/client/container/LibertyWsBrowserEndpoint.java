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

package com.roguecloud.client.container;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roguecloud.BrowserWebSocketClientShared;
import com.roguecloud.client.ClientMappingSingleton;
import com.roguecloud.client.ClientState;
import com.roguecloud.client.LibertyClientInstance;
import com.roguecloud.client.WorldStateListeners;
import com.roguecloud.client.ClientWorldState.ClientWorldStateListener;
import com.roguecloud.client.utils.ClientUtil;
import com.roguecloud.json.browser.JsonBrowserConnect;
import com.roguecloud.json.browser.JsonUpdateBrowserUI;
import com.roguecloud.map.IMap;
import com.roguecloud.utils.RCUtils;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.ResourceLifecycleUtil;

/** 
 * WebSocket endpoint for the client browser UI. The only message that this endpoint expects to receive from the client is JsonBrowserConnect, at which
 * point LibertyWSClientWorldStateListener will write to the WebSocket whenever the world state changes.
 **/
@ServerEndpoint("/api/browser")
public class LibertyWsBrowserEndpoint {

	private static final Logger log = Logger.getInstance();
	
	@OnOpen
	public void open(Session session) {
		
		// If the client instance is disposed, then immediately close all opened Sessions 
		if(LibertyClientInstance.getInstance().isDisposed()) {
			log.interesting("Ignoring onOpen on an endpoint with a closed LibertyClientInstance", null);
			try { session.close(); } catch (IOException e) {  /*ignore*/ }
			return;
		}

		System.out.println("WebSocket "+session.getId()+" opened for Web browser on client instance "+LibertyClientInstance.getInstance().getUuid());
		ResourceLifecycleUtil.getInstance().addNewSession(
				ClientUtil.convertSessionToManagedResource(session));
		
		LibertyClientInstance.getInstance().add(session);
	}

	@OnClose
	public void close(Session session) {
		System.out.println("WebSocket "+session.getId()+" closed for Web browser on client instance "+LibertyClientInstance.getInstance().getUuid());
		ResourceLifecycleUtil.getInstance().removeSession(
				ClientUtil.convertSessionToManagedResource(session));
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
				
				LibertyClientBrowserSessionWrapper wrapper = new LibertyClientBrowserSessionWrapper(session); 
						//LibertyClientBrowserSessions.getInstance().addSession(jbc.getUuid(), session);
				
				ClientState cs = ClientMappingSingleton.getInstance().getClientState(jbc.getUuid());
				if(cs == null || cs.getCurrentRound() == null) {
					log.info("Closing browser web socket since round is currently not set.", null);
					// If current round is null, wait 2 seconds then close the session immediately.
					new Thread() {
						public void run() {
							RCUtils.sleep(2000);
							try { session.close(); } catch (IOException e) {  /* ignore */ }
						}
					}.start();
					return;
				} else {
					LibertyWSClientWorldStateListener l = new LibertyWSClientWorldStateListener(wrapper, session);
//					wrapper.setListener(l);
					
					WorldStateListeners.getInstance().addListener(l);
					
					
//					cs.getClientWorldState().addListener( new LibertyWSClientWorldStateListener(wrapper, session) );	
				}
								
			} else {
				log.severe("Unrecognized message type: "+messageType+" msg: "+message, null);
				return;
			}
			

		} catch (Exception e) {
			e.printStackTrace();
			log.severe("Unexpected exception in WebSocketBrowserEndpoint", e, null);
		}
		
		
	}
	

	/** This class is informed whenever the Rogue Cloud server sends the client API new world state data. This class 
	 * does minimal processing, and passes it along to the LiberyClientBrowserSessionWrapper, who is responsible
	 * for sending that to the client's web browser (to be displayed in the Rogue Client Web UI). */
	public static class LibertyWSClientWorldStateListener implements ClientWorldStateListener {

		private final LibertyClientBrowserSessionWrapper wrapper;
		private final Session session;
		
		private final Object lock = new Object();
		
		private boolean threadStarted_synch_lock = false;
		
		public LibertyWSClientWorldStateListener(LibertyClientBrowserSessionWrapper wrapper, Session session) {
			this.wrapper = wrapper;
			this.session = session;
		}
		
		@Override
		public void worldStateUpdated(int currClientWorldX, int currClientWorldY, int newWorldPosX, int newWorldPosY,
				int newWidth, int newHeight, IMap map, long ticks) {

			// Every time ClientWorldState informs us that the world state has been updated, then convert the world state to JSON 
			// and send it to the client browser connection.
			if(session.isOpen()) {

				String str = BrowserWebSocketClientShared.generateBrowserJson(currClientWorldX, currClientWorldY, newWorldPosX, newWorldPosY, 
						newWidth, newHeight, map, Collections.emptyList(), null, ticks, Json.createBuilderFactory(Collections.emptyMap()));
				wrapper.sendMessage(str);
			}
			
		}

		@Override
		public void receiveBrowserUIUpdate(JsonUpdateBrowserUI u) {
			if(session.isOpen()) {
				ObjectMapper om = new ObjectMapper();
				String str;
				try { str = om.writeValueAsString(u); } catch (JsonProcessingException e1) { log.severe("Unable to parse", e1, null);  return; }

				wrapper.sendMessage(str);
			}
		}

		@Override
		public void roundComplete(int nextRoundInXSeconds) {
			
			synchronized (lock) {
				if(threadStarted_synch_lock) {
					return;
				}
				threadStarted_synch_lock = true;
			}
			
			log.info("Round complete signaled to client browser", null);
			
			new Thread() {
				public void run() {
					// TODO: LOW - Reconsider the logic behind this.
					try { TimeUnit.SECONDS.sleep(nextRoundInXSeconds+2); } catch (InterruptedException e) { /* ignore*/ }
					wrapper.dispose();		
				}
			}.start();
		}

		@Override
		public boolean isClientOpen() {
			return session.isOpen();
		}
		
	}
}
