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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.websocket.Session;

import com.roguecloud.RCRuntime;
import com.roguecloud.ServerInstance;
import com.roguecloud.server.ActiveWSClient.ViewType;
import com.roguecloud.server.ActiveWSClientSession.Type;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;

public final class ActiveWSClientList {
	
	private static final Logger log = Logger.getInstance();
	
	private final ServerInstance parent;
	
	private final LogContext lc;
	
	private final Object lock = new Object();
	
	private long nextClientId_synch_lock = 0;
	
	private final List<ActiveWSClient> clientList_synch_lock = new ArrayList<>();

	/** The map contains the user ids of any users that have connected this round; this is used to prevent users from connecting multiple times. */
	private final HashMap<Long /* user id*/, Boolean> userClientConnected_synch_lock = new HashMap<>();
	
	private final HashMap<String /* unique session id*/, SessionToClientValue> sessionToClientMap_sync_lock = new HashMap<>();
	private final HashMap<String /* client uuid */, ActiveWSClient> uuidToClientMap_sync_lock = new HashMap<>();

	private boolean isDisposed = false;
	
	public ActiveWSClientList(ServerInstance si) {
		this.parent = si;
		lc = LogContext.serverInstance(si.getId());
	}

		
	public ActiveWSClient addSession(String username, long userId, ActiveWSClientSession clientSession) {
		if(username == null || username.trim().length() == 0 || userId < 0 || clientSession == null) { throw new IllegalArgumentException(); }
		
		if(isDisposed) {
			log.err("Attempt to add session on disposed list", lc);
			return null;
		}
		
//		List<Session> sessionsToRemove = new ArrayList<>();
		
		ActiveWSClient activeClient;
		synchronized(lock) {
			activeClient = uuidToClientMap_sync_lock.get(clientSession.getUuid());

			// If we have seen this uuid before
			if(activeClient != null) {
				
				if(clientSession.getViewType() == activeClient.getViewType()) {
					// Associate a new Websocket session with an existing client
					sessionToClientMap_sync_lock.put(clientSession.getSessionId(), new SessionToClientValue(activeClient/*, clientSession.getSession()*/ ));
					activeClient.addSession(clientSession);
				} else {
					log.severe("Attempt to add a session to a client but the view-only bit does not match: "+clientSession.getViewType()+" "+activeClient.getViewType(), lc);
					return null;
				}
				
			} else {
				// If we have not seen this UUID before...
				
				// If the user has already connected w/ a different UUID and client connection this round, then don't allow them to open another.
				if(clientSession.getViewType() == ViewType.CLIENT_VIEW) {
					if(userClientConnected_synch_lock.containsKey(userId)) {
						log.err("User attempted to connect with more than one UUID: "+userId+" "+username, null);
						return null;
					}
				}
				
				long clientId = nextClientId_synch_lock++;
				activeClient = new ActiveWSClient(clientSession.getUuid(), username, userId, clientId, this, clientSession.getViewType());
				
				clientList_synch_lock.add(activeClient);
				sessionToClientMap_sync_lock.put(clientSession.getSessionId(), new SessionToClientValue(activeClient/*, clientSession.getSession()*/));
				uuidToClientMap_sync_lock.put(clientSession.getUuid(), activeClient);
				
				if(clientSession.getType() == Type.CLIENT) {
					userClientConnected_synch_lock.put(userId, true);
				}
				
				activeClient.addSession(clientSession);
				
			}
			
//			sessionsToRemove.addAll(sessionToClientMap_sync_lock.values().stream().map(e -> e.session).collect(Collectors.toList()));
		}
		
//		// Go through the session list and remove all sessions which are still open, leaving only those that are closed.
//		for(Iterator<Session> it = sessionsToRemove.iterator(); it.hasNext();) {
//			Session s = it.next();
//			if(s.isOpen()) {
//				it.remove();
//			}
//		}
//		
//		synchronized (lock) {
//			
//			sessionsToRemove.forEach( e -> {
//				sessionToClientMap_sync_lock.remove(e.getId());
//			});
//			
//		}
		
		return activeClient;					
	}
	
	
	public List<ActiveWSClient> getList() {
		
		ArrayList<ActiveWSClient> result = new ArrayList<>();
		synchronized(lock) {
			result.addAll(clientList_synch_lock);
		}
		
		return result;
	}
	
	public ActiveWSClient getClientBySession(Session s) {
		if(s == null) { throw new IllegalArgumentException(); }
		synchronized (lock) {
			SessionToClientValue stcv = sessionToClientMap_sync_lock.get(s.getId());
			if(stcv == null) { return null; }
			return stcv.client;
		}
	}
	
	ServerInstance getServerInstance() {
		return parent;
	}
	
	
	public void dispose() {
		
		RCRuntime.assertNotGameThread();

		List<ActiveWSClient> toDispose = new ArrayList<>();
		
		synchronized(lock) {
			isDisposed = true;

			toDispose.addAll(clientList_synch_lock);
			toDispose.addAll(sessionToClientMap_sync_lock.values().stream().map(e -> e.client).distinct().collect(Collectors.toList()));
			toDispose.addAll(uuidToClientMap_sync_lock.values().stream().distinct().collect(Collectors.toList()));

			toDispose = toDispose.stream().distinct().collect(Collectors.toList());
			
			clientList_synch_lock.clear();
			sessionToClientMap_sync_lock.clear();
			uuidToClientMap_sync_lock.clear();
		}
		
		toDispose.forEach( e -> {
			try {
				e.dispose();
			} catch(Exception ex) {
				/* ignore*/
			}
		});
				
	}
	
	private static class SessionToClientValue {
		ActiveWSClient client;
//		Session session;
		
		public SessionToClientValue(ActiveWSClient client/*, Session session*/) {
			this.client = client;
//			this.session = session;
		}
		
	}
}
