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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.websocket.Session;

import com.roguecloud.RCRuntime;
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;

public class ActiveWSClient {
	
	private static final Logger log = Logger.getInstance();
	
	private final Object lock = new Object();
	
	/** Client will not have a UUID in INITIAL state, and may not necessarily have one in COMPLETE.*/
	private final String uuid;
	
	private final String username;
	
	private final long userId;
	
	private final long clientId;
	
	private final ServerMessageReceiver messageReceiver;
	
	private final LogContext lc;
	
	/** This should only be accessed from the game thread*/
	private Object gameEngineInfo;
	
	private final HashMap<String /* session id */, SessionsMapValue> sessionsMap_synch_sessions = new HashMap<>();

	private final ActiveWSClientSession[/* Type id */ ] mostRecentSessionOfType_synch_sessions = new ActiveWSClientSession[ActiveWSClientSession.Type.values().length];
	
	private final ArrayList<ActiveWSClientSession> sessions_synch = new ArrayList<>();
	
	private final ArrayList<ReceivedAction> waitingActions_synch = new ArrayList<>();
	
	private Long lastActionMessageIdReceived_synch_lock = null;
	
	private final ViewType viewType;
	
	private long nextClientFrameNumber_synch_lock = 1;
	
	private boolean disposed = false;
	
	private Long activeHealthCheckId_synch_lock;
	private Long activeHealthCheckSinceInNanos_synch_lock;
	
	private static final int MAX_ACTIONS_TO_KEEP_PER_CLIENT = 500;
	
	
	public ActiveWSClient(String uuid, String username, long userId, long clientId, ActiveWSClientList parent, ViewType viewType) {
		if(uuid == null || username == null || viewType == null) { throw new IllegalArgumentException(); }
		
		this.uuid = uuid;
		this.clientId = clientId;
		this.username = username;
		this.userId = userId;
		
//		this.parent = parent;
		this.lc = LogContext.serverInstanceWithClientId(parent.getServerInstance().getId(), clientId);
		this.messageReceiver = new ServerMessageReceiver(lc);
		this.viewType = viewType;
	}
	
	public Object getGameEngineInfo() {
		return gameEngineInfo;
	}
	
	public void setGameEngineInfo(Object gameEngineInfo) {
		this.gameEngineInfo = gameEngineInfo;
	}
	
	public long getClientId() {
		return clientId;
	}
	
	public String getUuid() {
			return uuid;
	}
	
	public ServerMessageReceiver getMessageReceiver() {
		return messageReceiver;
	}
	
	public List<ActiveWSClientSession> getSessions() {
		ArrayList<ActiveWSClientSession> result = new ArrayList<>();
		synchronized (sessions_synch) {
			result.addAll(sessions_synch);
			return Collections.unmodifiableList(result);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		ActiveWSClient other = (ActiveWSClient)obj;
		
		if(other.getClientId() != getClientId()) {
			return false;
		}
		
		if(!other.getUuid().equals(getUuid())) {
			return false;
		}
		
		return true;		
	}
	
	@Override
	public int hashCode() {
		return (int)(getClientId())+getUuid().hashCode();
	}
	
	public ActiveWSClientSession getMostRecentSessionOfType(ActiveWSClientSession.Type type) {
		synchronized (sessions_synch) {
			
			ActiveWSClientSession result = mostRecentSessionOfType_synch_sessions[type.ordinal()];
			if(result != null && result.isSessionOpenUnsynchronized()) {
				return result;
			}
			
			return null;
			
//			return mostRecentSessionOfType_synch_sessions.get(type);
			
//			return sessions_synch.stream()
//				.filter(e -> e.getType() == type && e.isSessionOpen())
//				.sorted( (a, b) -> {
//					// Sort descending
//					long diff = b.getConnectTimeInNanos()-a.getConnectTimeInNanos();
//					if(diff > 0 ) { return 1; }
//					else if(diff < 0) { return -1; }
//					else { return 0; }
//				}
//			).findFirst().orElse(null); 
		}
	}
	
	public Long getActiveHealthCheckId() {
		synchronized(lock) {
			return activeHealthCheckId_synch_lock;
		}
	}
	
	public Long getActiveHealthCheckSinceInNanos() {
		synchronized(lock) {
			return activeHealthCheckSinceInNanos_synch_lock;
		}
	}
	
	public void addHealthCheckId(long id) {
		synchronized (lock) {
			activeHealthCheckId_synch_lock = id;
			activeHealthCheckSinceInNanos_synch_lock = System.nanoTime();			
		}
	}

	public void informReceivedHealthCheckResponse(long id) {
		synchronized (lock) {
			if(activeHealthCheckId_synch_lock != null && activeHealthCheckId_synch_lock == id) {
				activeHealthCheckId_synch_lock = null;
				activeHealthCheckSinceInNanos_synch_lock = null;
			}
		}
	}

	public void addAction(JsonAbstractTypedMessage action, long messageId) {
		synchronized(waitingActions_synch) {
			// Only store the last 500 actions, to prevent memory leak
			while(waitingActions_synch.size() > MAX_ACTIONS_TO_KEEP_PER_CLIENT) {
				waitingActions_synch.remove(0);
			}
			waitingActions_synch.add(new ReceivedAction(messageId, action));
		}
	}
	
	public ReceivedAction getAndRemoveWaitingAction() {
		synchronized(waitingActions_synch) {
			if(waitingActions_synch.size() > 0) {
				return waitingActions_synch.remove(0);
			} else {
				return null;
			}
		}
	}
		
	public ActiveWSClientSession getAWSClientSession(Session s) {
		synchronized(sessions_synch) {
			SessionsMapValue smv = sessionsMap_synch_sessions.get(s.getId());
			if(smv == null) { return null; }
			
			return smv.awsClientSession;		
		}
	}
	
	public void addSession(ActiveWSClientSession acs) {
		RCRuntime.assertNotGameThread();
		
		if(disposed) {
			log.err("Attempt to add session after dispose", lc);
			return;
		}
		
		List<ActiveWSClientSession> sessionsToRemove = new ArrayList<ActiveWSClientSession>();
		synchronized(sessions_synch) {
			sessions_synch.add(acs);
			sessionsMap_synch_sessions.put(acs.getSessionId(), new SessionsMapValue(acs));
			
			sessionsToRemove.addAll(sessionsMap_synch_sessions.values().stream().map( e -> e.awsClientSession).distinct().collect(Collectors.toList()));
			
//			sessionsToRemove.addAll(sessionsMap_synch_sessions.values().stream().map( e -> e.session).distinct().collect(Collectors.toList()));
			
			mostRecentSessionOfType_synch_sessions[acs.getType().ordinal()] = acs;
//			mostRecentSessionOfType_synch_sessions.put(acs.getType(), acs);
		}
		
		// Remove old sessions, after adding the new session.
		for(Iterator<ActiveWSClientSession> it = sessionsToRemove.iterator(); it.hasNext();) {
			ActiveWSClientSession s = it.next();
			if(s.isSessionOpenUnsynchronized()) {
				it.remove();
			}
		}
		synchronized(sessions_synch) {
			for(ActiveWSClientSession s : sessionsToRemove) {
				sessionsMap_synch_sessions.remove(s.getSessionId());
				
				// Clear most recent session matches
				for(int x = 0; x < mostRecentSessionOfType_synch_sessions.length; x++) {
					ActiveWSClientSession acsIndex = mostRecentSessionOfType_synch_sessions[x]; 
					if(acsIndex != null && acsIndex == s) {
						mostRecentSessionOfType_synch_sessions[x] = null;
					}
				}
				
				sessions_synch.remove(s);
			}
		}
		
		for(ActiveWSClientSession s : sessionsToRemove) {
			try { s.dispose(); } catch(Exception e) { /* ignore */ }
		}
	}
	
	public final LogContext getLogContext() {
		return lc;
	}
	
	public ViewType getViewType() {
		return viewType;
	}

	public long getUserId() {
		return userId;
	}
	
	public String getUsername() {
		return username;
	}
	
	public long getAndIncrementNextClientFrameNumber() {
		synchronized(lock) {
			return nextClientFrameNumber_synch_lock++;
		}
	}
	
	public void setLastActionMessageIdReceived(long lastActionMessageIdReceived) {
		synchronized(lock) {
			this.lastActionMessageIdReceived_synch_lock = lastActionMessageIdReceived;			
		}
	}
	
	public Long getAndResetLastActionMessageIdReceived() {
		synchronized(lock) {
			Long value = lastActionMessageIdReceived_synch_lock;
			lastActionMessageIdReceived_synch_lock = null;
			return value;
		}
	}

	public void dispose() {
	
		disposed = true;

		synchronized (waitingActions_synch) {
			waitingActions_synch.clear();
		}

		List<ActiveWSClientSession> sessionsToDispose = new ArrayList<>();
		synchronized(sessions_synch) {
			sessionsToDispose.addAll(sessionsMap_synch_sessions.values().stream().map(e -> e.awsClientSession).distinct().collect(Collectors.toList()));
			sessionsToDispose.addAll(sessions_synch);
			
			sessions_synch.clear();
			sessionsMap_synch_sessions.clear();

			for(int x = 0; x < mostRecentSessionOfType_synch_sessions.length; x++) {
				mostRecentSessionOfType_synch_sessions[x] = null;
			}
		}
				
		sessionsToDispose.stream().distinct().forEach( e -> {
			try {
				e.dispose();
			} catch(Exception ex) {
				/* ignore */
			}
		});
		
		gameEngineInfo = null;

	}

	
	public static class SessionsMapValue {
		ActiveWSClientSession awsClientSession;
		
		public SessionsMapValue(ActiveWSClientSession awsClientSession) {
			this.awsClientSession = awsClientSession;
		}
		
		
	}
	
	public static class ReceivedAction {
		
		private final long messageId;
		private final JsonAbstractTypedMessage jsonAction;
		
		public ReceivedAction(long messageId, JsonAbstractTypedMessage jsonAction) {
			this.messageId = messageId;
			this.jsonAction = jsonAction;
		}

		public long getMessageId() {
			return messageId;
		}

		public JsonAbstractTypedMessage getJsonAction() {
			return jsonAction;
		}
		
	}

	
	public static enum ViewType { CLIENT_VIEW(false), SERVER_VIEW_WORLD(true), SERVER_VIEW_FOLLOW(true); 
		
		private final boolean adminRequired;
		
		ViewType(boolean adminRequired) {
			this.adminRequired = adminRequired;
		}
		
		public boolean isAdminRequired() {
			return adminRequired;
		}
	}

}
