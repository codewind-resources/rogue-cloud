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

public class ActiveWSClientSession {
	
	private final static Logger log = Logger.getInstance();
	
	public static enum Type { BROWSER, CLIENT };

	private final Session session_synch;
	
	private final Long connectTimeInNanos;

	private final LogContext logContext;
	
	private final Type type;

	private final ViewType viewType;
	
	private final String uuid;
	
	private final List<String> stringsToSend_synch = new ArrayList<>();
	
	private final RoundScope roundScope;
	
	private final AWSClientSessionSender senderThread;
	
	private final Object lock = new Object();
	
	private boolean isFullClientReset_synch_lock = false;
	
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
//		synchronized(session_synch) {
//			return session_synch.hashCode();
//		}
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
						
						stringsToSend_synch.wait(10000);
						
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
				
				try {
					// TODO: EASY - Add this to new thread.
					session_synch.close(); // intentional non-synch
				} catch (IOException e) {
					/* ignore*/
				}

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
