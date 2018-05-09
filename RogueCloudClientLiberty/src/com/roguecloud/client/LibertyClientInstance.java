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


package com.roguecloud.client;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.websocket.Session;

import com.roguecloud.client.container.LibertyClientBrowserSessionWrapper;

/**
 * Whenever Liberty publishes a change to an existing Web application, it causes the old version of the Web application to be stopped, and 
 * the new version of the application to be started (as you can see in the Liberty console). However, thread from 
 * the old version of the application can remaining running. 
 * 
 * In this situation, the old version should detect that it is deprecated and should dispose of any lingering object (while preventing
 * new objects from being created.)
 * 
 * This class maintains weak references to all objects created for the current Liberty application instance, and disposes of them when the dispose() method is called.
 */
public class LibertyClientInstance {

	private static final LibertyClientInstance instance = new LibertyClientInstance();
	
	public static LibertyClientInstance getInstance() {
		return instance;
	}
	
	// -------------------------------------------------------------------
	
	private final String uuid;
	
	private final Object lock = new Object();
	
	private boolean disposeSignaled_synch_lock = false;
	
	private final List<WeakReference<LibertyClientBrowserSessionWrapper>> lcbswList_synch_lock = new ArrayList<>();
	
	private final List<WeakReference<LibertySessionWrapper>> lswList_synch_lock = new ArrayList<>();
	
	private final List<WeakReference<Session>> sessions_synch_lock = new ArrayList<>();
	
	private final List<WeakReference<Thread>> miscThreads_synch_lock = new ArrayList<>();
	
	private long objectsAdded_synch_lock = 0;
	
	
	public LibertyClientInstance() {
		this.uuid = UUID.randomUUID().toString();

		log("Client instance started. ");
	}
	
	
	public void informStarted() {
		log("Instance informed of start.");
		
	}
	
	public void dispose() {
		log("Dispose started.");
		synchronized (lock) {
			disposeSignaled_synch_lock = true;
			
			lcbswList_synch_lock.forEach( e -> { disposeObject(e); } ) ;
			lswList_synch_lock.forEach( e -> { disposeObject(e); } ) ;
			sessions_synch_lock.forEach( e -> { disposeObject(e); } ) ;
			miscThreads_synch_lock.forEach( e -> { disposeObject(e); } ) ;
			
		}
		log("Disposing complete.");
	}
	
	private void disposeObject(final WeakReference<?> wr) {
		if(wr.get() == null) { return; }
		
		// Start a new thread for each reference to dispose of 
		new Thread() {
			@Override
			public void run() { 
				try {
					Object o = wr.get();
					if(o == null) { return; }
					
					if(o instanceof LibertyClientBrowserSessionWrapper) {
						LibertyClientBrowserSessionWrapper x = (LibertyClientBrowserSessionWrapper)o;
						x.dispose();
					} else if(o instanceof LibertySessionWrapper) {
						LibertySessionWrapper x = (LibertySessionWrapper)o;
						x.dispose();
					} else if(o instanceof Session) {
						Session x = (Session)o;
						x.close();
					} else if(o instanceof Thread) {
						Thread t = (Thread)o;
						t.interrupt();
					} else {
						log("Unrecognized object type: "+o.getClass().getName());
					}
				} catch(Exception e) {
					/* ignore */
				}
			}
		}.start();
		
	}
	
	public void add(LibertySessionWrapper lsw) {
		synchronized(lock) {
			if(disposeSignaled_synch_lock) { return; }
			
			lswList_synch_lock.add(new WeakReference<>(lsw));
			cleanAllListsIfNeeded();
		}
	}
	
	public void add(LibertyClientBrowserSessionWrapper lcbsw) {
		synchronized (lock) {
			if(disposeSignaled_synch_lock) { return; }
			
			lcbswList_synch_lock.add(new WeakReference<>(lcbsw));
			cleanAllListsIfNeeded();
		}
	}
	
	public void add(Session session) {
		synchronized (lock) {
			if(disposeSignaled_synch_lock) { return; }
			
			sessions_synch_lock.add(new WeakReference<>(session));
			cleanAllListsIfNeeded();
		}
	}
	
	public void add(Thread thread) {
		synchronized (lock) {
			if(disposeSignaled_synch_lock) { return; }
			
			miscThreads_synch_lock.add(new WeakReference<>(thread));
			cleanAllListsIfNeeded();
		}
	}
	
	/** Remove null weak references from the various lists */
	private void cleanAllListsIfNeeded() {
		synchronized(lock) {
			objectsAdded_synch_lock++;
			if(objectsAdded_synch_lock < 0) { objectsAdded_synch_lock = 0; }
			
			if(objectsAdded_synch_lock % 500 == 0 && objectsAdded_synch_lock > 0) {
			
				for(Iterator<WeakReference<LibertyClientBrowserSessionWrapper>> it = lcbswList_synch_lock .iterator(); it.hasNext();) {
					WeakReference<LibertyClientBrowserSessionWrapper> wr = it.next();
					if(wr.get() == null) {
						it.remove();
					}
				}
				
				for(Iterator<WeakReference<LibertySessionWrapper>> it = lswList_synch_lock.iterator(); it.hasNext(); ) {
					WeakReference<LibertySessionWrapper> wr = it.next();
					if(wr.get() == null) {
						it.remove();
					}
				}
				
				for(Iterator<WeakReference<Session>> it = sessions_synch_lock.iterator(); it.hasNext();) {
					WeakReference<Session> wr = it.next();
					if(wr.get() == null) {
						it.remove();
					}
				}
				
				for(Iterator<WeakReference<Thread>> it = miscThreads_synch_lock.iterator(); it.hasNext();) {
					WeakReference<Thread> wr = it.next();
					if(wr.get() == null) {
						it.remove();
					}
				}
			}
			
			
		}
	}
	
	
	private void log(String str) {
		System.out.println("[ci:"+uuid+"] " +str);
	}
	
	
	public String getUuid() {
		return uuid;
	}

	public boolean isDisposed() {
		synchronized(lock) {
			return disposeSignaled_synch_lock;
		}
	}
}
