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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import com.roguecloud.RCRuntime;
import com.roguecloud.utils.Logger;

/** 
 * For internal use only */
public class RCServerUtilLatencySim {

	private static final Logger log = Logger.getInstance();
	
	private final Queue<Entry> queue_synch = new ArrayDeque<Entry>();

	private final HLLThread thread;

	private boolean disposed = false;
	
	public RCServerUtilLatencySim() {
		thread = new HLLThread();
		thread.start();
	}

	public void addMessage(ActiveWSClientSession session, ActiveWSClient client, String str) {
		if(session == null || client == null || str == null) { throw new IllegalArgumentException(); }
		if(disposed) { return; }
		
//		System.out.println("adding interface msg: "+str);
		
		long pushTime = System.nanoTime()+(long)(Math.random()*(RCRuntime.MAX_LATENCY_SIM_IN_NANOS-RCRuntime.MIN_LATENCY_SIM_IN_NANOS))+RCRuntime.MIN_LATENCY_SIM_IN_NANOS;
		
		synchronized(queue_synch) {
			queue_synch.add(new Entry(str, session, client, pushTime));
			queue_synch.notify();
		}
		
	}
		
	public void dispose() {
		synchronized(queue_synch) {
			queue_synch.clear();
		}
		this.disposed = true;
	}
	
	private class HLLThread extends Thread {

		
		public HLLThread() {
			setDaemon(true);
			setName(HLLThread.class.getName());
		}
		
		@Override
		public void run() {
			
			List<Entry> toSend = new ArrayList<>();
			
			while(!disposed) {
				
				long currTime = System.nanoTime();
				
				toSend.clear();

				synchronized (queue_synch) {
				
					while(queue_synch.size() > 0) {
						Entry e = queue_synch.peek();
						if(currTime > e.getPushTimeInNanos()) {
							queue_synch.poll();
							toSend.add(e);
						} else {
							break;
						}
					}
					
				}
				
				long sleepStartTimeInNanos = System.nanoTime();
				
				for(Entry entry : toSend) {
					try {
						
						entry.getClient().getMessageReceiver().receiveMessage(entry.getStr(), entry.getSession(), entry.getClient());
						
					} catch(Exception e) {
						e.printStackTrace();
						log.severe("Unexpected exception on message send", e, null);
					}
				}
				
				
				try {
					
					long sleepTimeInMsecs = 10 - (TimeUnit.MILLISECONDS.convert(System.nanoTime() - sleepStartTimeInNanos, TimeUnit.NANOSECONDS));
					if(sleepTimeInMsecs > 0) {
						synchronized(queue_synch) {
							queue_synch.wait(sleepTimeInMsecs);
						}
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				
			} // end while
			
		}
		
	}

	private static class Entry {

		private final String str;
		private final long pushTimeInNanos;
		private final ActiveWSClientSession session;
		private final ActiveWSClient client;

		
		public Entry(String str, ActiveWSClientSession session, ActiveWSClient client, long pushTime) {
			this.str = str;
			this.session = session;
			this.pushTimeInNanos = pushTime;
			this.client = client;
		}



		public String getStr() {
			return str;
		}
		
		public long getPushTimeInNanos() {
			return pushTimeInNanos;
		}

		public ActiveWSClientSession getSession() {
			return session;
		}

		public ActiveWSClient getClient() {
			return client;
		}

	}
	
}
