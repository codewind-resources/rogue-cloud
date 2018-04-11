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
import java.util.ArrayList;
import java.util.List;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import com.roguecloud.RCRuntime;
import com.roguecloud.NG;
import com.roguecloud.RoundScope;
import com.roguecloud.server.ActiveWSClient.ViewType;
import com.roguecloud.utils.CompressionUtils;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.ServerUtil;

/** 
 * Container object for the WebSocket API's Session object. This class has the same lifecycle as the Session it contains. 
 * The parent class of this class is ActiveWSClient. 
 * 
 * To write asynchronously write an outbound message to the session contained in this object, call the writeToClientAsynch(...) method.
 * The message will be added to the outbound message queue, and written by the AWSClientSessionSender thread. 
 **/
public class ActiveWSClientSession {
	
	private final static Logger log = Logger.getInstance();
	
	public static enum Type { BROWSER, CLIENT };

	/** The contained WebSocket Session */
	private final Session session_synch;
	
	/** When the connection was first established */
	private final Long connectTimeInNanos;

	private final LogContext logContext;
	
	/** Will this session be used for the browser API or the agent AI API */
	private final Type type;

	/** Specifies what the WebSoocket will be used for. See the javadoc comment on ViewType for specifics.  */
	private final ViewType viewType;
	
	private final String uuid;
	
	/** A buffer of Strings (usually JSON) that the AWSClientSessionSender thread will write to the WebSocket at
	 * the next opportunity. */
	private final List<String> stringsToSend_synch = new ArrayList<>();
	
	/** The round that the session was created in */
	private final RoundScope roundScope;
	
	/** This is the thread responsible for writing to the WebSocket session */
	private final AWSClientSessionSender senderThread;
	
	private final Object lock = new Object();

	/**
	 * A "full client reset" means that the connecting client (in this case, the agent API) does not have any information 
	 * about the previous state of connection. This occurs either when the client first connects during a round, or
	 * when the client's process is restarted (for example, the application server restarted).  
	 * 
	 * The client connection will inform us (the server) if a full client reset is required. The client will inform us
	 * when it first connects, in JsonClientConnect.
	 * 
	 * On the server side, the previous state of the connection is stored in EngineWebSocketState. If the client 
	 * informs us that a full client reset is required, then the following connection context fields will 
	 * be cleared from EngineWebSocketState: objectSeenByPlayer, mapReceivedMessageIds, mapResponseToMessage.
	 *  
	 */
	private boolean isFullClientReset_synch_lock = false;
	
	/** ID from Session.getId() */
	private final String sessionId;
	
	/** Browser only field - whether or not the browser needs to have the full frame sent to it (as it is the first time connecting.) */
	private boolean browserFirstConnect = true;
	
	public ActiveWSClientSession(Type type, ViewType viewType, String uuid, Session s, RoundScope roundScope, Long connectTimeInNanos, boolean isFullClientReset, LogContext logContext) {
		this.type = type;
		this.session_synch = s;
		this.logContext = logContext;
		this.connectTimeInNanos = connectTimeInNanos;
		this.roundScope  = roundScope;
		this.uuid = uuid;
		this.viewType = viewType;
		
		this.sessionId = session_synch.getId();
		
		this.isFullClientReset_synch_lock = isFullClientReset;
		
		senderThread = new AWSClientSessionSender();
		senderThread.start();
	}
	
	public ViewType getViewType() {
		return viewType;
	}
	
	public boolean isBrowserFirstConnect() {
		return browserFirstConnect;
	}
	
	public void setBrowserFirstConnect(boolean browserFirstConnect) {
		this.browserFirstConnect = browserFirstConnect;
	}

	public boolean isSessionOpenUnsynchronized() {
		// Intentionally unsynchronized
		return session_synch.isOpen();
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public boolean getAndResetFullClientResetState() {
		synchronized (lock) {
			if(isFullClientReset_synch_lock) {
				isFullClientReset_synch_lock = false;
				return true;
			}
			
			return false;
		}
		
	}

	public Long getConnectTimeInNanos() {
		return connectTimeInNanos;
	}
	
	public void writeToClientAsync(String msg) {
		
		synchronized (stringsToSend_synch) {
			stringsToSend_synch.add(msg);
			stringsToSend_synch.notify();
		}
	}

	public Type getType() {
		return type;
	}

	public String getUuid() {
		return uuid;
	}
	
	@Override
	public int hashCode() {
		return this.sessionId.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ActiveWSClientSession)) {
			return false;
		}
		
		ActiveWSClientSession other = (ActiveWSClientSession)obj;
		
		return this.sessionId.equals(other.sessionId);
		
//		synchronized(this.session_synch) {
//			return this.session_synch.equals(  ((AWSClientSession)obj).getSession()  );
//		}
	}

	/** This class is responsible for writing messages from the stringsToSend message queue to the 
	 * WebSocket endpoint. The thread ends if an exception occurs, or if the session is closed. */
	private class AWSClientSessionSender extends Thread{
		
		public AWSClientSessionSender() {
			setName(AWSClientSessionSender.class.getName()+" "+sessionId);
			setDaemon(true);
		}
		
		@Override
		public void run() {

			log.info("AWSClientSessionSender started for "+session_synch, logContext);
			
			try {
				boolean continueLoop = true;
				
				Basic b;
				synchronized(session_synch) {
					b = session_synch.getBasicRemote();
				}
	
				List<String> localStringsToSend = new ArrayList<String>();
	
				while(continueLoop) {
					
					if(roundScope.isRoundComplete()) {
//						if(roundExpireTimeInNanos == -1) {
////							ObjectMapper om = new ObjectMapper();
////							if(type == Type.BROWSER) {
////								b.sendText(om.writeValueAsString(new JsonRoundComplete()));	
////							} else {
////								b.sendBinary(CompressionUtils.compressToByteBuffer(om.writeValueAsString(new JsonRoundComplete())));								
////							}
////							log.interesting("Sending round complete to client", logContext);
//							roundExpireTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(2, TimeUnit.MINUTES);	
//						} else if(System.nanoTime() > roundExpireTimeInNanos) {
							log.interesting("Exiting loop on round expiration.", logContext);
							continueLoop = false;
//						}						
					}
					
					synchronized(stringsToSend_synch) {
						
						stringsToSend_synch.wait(10000); // TODO: Uhh?
						
						localStringsToSend.addAll(stringsToSend_synch);
						stringsToSend_synch.clear();
						
					}
	
					synchronized(session_synch) {
						
						if(session_synch.isOpen()) {

							for(String str : localStringsToSend) {
								if(NG.ENABLED) { NG.log(RCRuntime.GAME_TICKS.get(), "Writing to client in AWSClientSession: "+str); }
								log.info("Writing out ["+type.name()+" ("+str.getBytes().length+")]: "+str, logContext);
								if(type == Type.BROWSER) {
									b.sendText(str);
								} else {
									b.sendBinary(CompressionUtils.compressToByteBuffer(str));	
								}
								
							}
							
							localStringsToSend.clear();
							
						} else {
							continueLoop = false;
						}
						
					}
					
				}
			} catch(InterruptedException ie) {
				/* ignore*/
			} catch(Throwable t) {
				log.severe("Error occured on session sender", t, logContext);
				t.printStackTrace();
			
			} finally {
				log.info("AWSClientSessionSender ended for "+session_synch, logContext);
				
				ServerUtil.runInAnonymousThread( () -> {
					try {
						session_synch.close(); // intentional non-synch
					} catch (IOException e) {
						/* ignore*/
					}
				});

			}
			
		} // end run
	}

	public void dispose() {
		RCRuntime.assertNotGameThread();
		
		// Close the session in a separate thread.
		Thread t = new Thread() {
			public void run() {
				try {
					// This is blocking call.
					session_synch.close(); // intentional non-synch
				} catch(Exception e) {
					/* ignore */
				}
			}
		};
		t.setName(ActiveWSClientSession.class.getName()+" - dispose() thread for "+sessionId);
		t.setDaemon(true);
		t.start();
		
		try {
			senderThread.interrupt();
		} catch(Exception e) { /* ignore*/ }
		
		synchronized (stringsToSend_synch) {
			stringsToSend_synch.clear();
		}
	};

}
