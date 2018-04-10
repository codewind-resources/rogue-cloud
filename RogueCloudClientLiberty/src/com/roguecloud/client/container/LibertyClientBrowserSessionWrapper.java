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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import com.roguecloud.client.container.LibertyWsBrowserEndpoint.LibertyWSClientWorldStateListener;
import com.roguecloud.utils.Logger;

public class LibertyClientBrowserSessionWrapper {
	
	private static final boolean SIMULATE_INTERMITTENT_FAILURES = false;
	
	private static final Logger log = Logger.getInstance();

	private final Object lock = new Object();
	
	/** Synchronize on me when accessing, and when writing to */
	private final Session session_synch;
	
	private final ClientSessionThread thread;
	
	private boolean disposed_synch_lock = false;
	
	private LibertyWSClientWorldStateListener listener;
	
	public LibertyClientBrowserSessionWrapper(Session session) {
		if(session == null) { throw new IllegalArgumentException(); }
		
		this.session_synch = session;
		thread = new ClientSessionThread();
		thread.start();
	}
	
	public boolean isSessionOpenUnsynchronized() {
		return session_synch.isOpen();		
	}
	
	public void sendMessage(String message) {
		thread.addMessage(message);
	}

	public void setListener(LibertyWSClientWorldStateListener listener) {
		this.listener = listener;
	}
	
	public LibertyWSClientWorldStateListener getListener() {
		return listener;
	}
	
	
	public void dispose() {
		thread.interrupt();
		
		synchronized(lock) {
			
			if(disposed_synch_lock) { return; }
			disposed_synch_lock = true;			
		}
		
		// Dispose of session on a separate thread.
		new Thread() {
			public void run() {
				try {
					session_synch.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
		}.start();
		
		
		thread.dispose();
	}
	
	private class ClientSessionThread extends Thread {
		
		private final List<String> messagesToSend_synch = new ArrayList<>();
		
		private final AtomicBoolean isRunning_synch = new AtomicBoolean(true);
		
		public ClientSessionThread() {
			setDaemon(true);
			setName(ClientSessionThread.class.getName());
		}
		
		@Override
		public void run() {
			
			try {
				innerRun();
			} catch (IOException e) {
				log.err("Error from inner run", e, null);
				e.printStackTrace();
			} catch (InterruptedException e) {
				/* ignore, this is expected on dispose. */
			} finally {
				dispose();
				synchronized (isRunning_synch) {
					isRunning_synch.set(false);
				}
			}
			
		}
		
		@SuppressWarnings("unused")
		private void innerRun() throws IOException, InterruptedException {
			Basic b;
			synchronized(session_synch) {
				b = session_synch.getBasicRemote();
			}
			
			List<String> localMessagesToSend = new ArrayList<>();
			
			while(isSessionOpenUnsynchronized() && !isInterrupted()) {

				if(SIMULATE_INTERMITTENT_FAILURES && Math.random() < 0.1) {
					try {
						session_synch.close(); 
						System.out.println("nuking browser.");
						
					} catch(Exception e) {
						/* ignore */
					}
				}
				
				synchronized (messagesToSend_synch) {
					if(messagesToSend_synch.size() == 0) {
						messagesToSend_synch.wait(1000);
					}
					
					localMessagesToSend.addAll(messagesToSend_synch);
					messagesToSend_synch.clear();
					
				}
				
				synchronized(session_synch) {
					for(String str : localMessagesToSend) {
						b.sendText(str);
//						System.out.println("sending text: "+str);
					}
				}
				
				localMessagesToSend.clear();
			}
		}
		
		public void addMessage(String message) {
			synchronized (isRunning_synch) {
				if(!isRunning_synch.get()) { return; }
			}
			
			synchronized (messagesToSend_synch) {
				messagesToSend_synch.add(message);
				messagesToSend_synch.notify();
			}
		}
		
		
		public void dispose() {
			synchronized(messagesToSend_synch) {
				messagesToSend_synch.clear();
			}
		}
		
	}

	public boolean isDisposed() {
		synchronized (lock) {
			return disposed_synch_lock;
		}
	}
	
}
