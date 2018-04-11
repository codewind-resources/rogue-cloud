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
 * This class is used to simulate latency ("lag") between the Rogue Cloud client and Rogue Cloud server. When this class is enabled,
 * messages received on WebSocket client endpoint are delayed by an arbitrary amount of time. This simulates server-side receive latency.
 * See also the RCUtilLatencySim for simulating receive latency on the client side.  
 * 
 * Latency is a major factor in how Rogue Cloud is designed: it is necessary that players must still be able to interact with
 * the game world even if they are far enough away from the physical location of the server to ensure that the 
 * information received by the client is slightly old.  
 * 
 * For example, if a player from Singapore connected to a server on the North American East Coast, that player would receive
 * and send actions approximately 2-3 frames behind other players (and behind monsters in the game world). However, we still
 * need to provide a good experience for such a player.
 * 
 **/
public class RCServerUtilLatencySim {

	private static final Logger log = Logger.getInstance();
	
	private final Queue<Entry> queue_synch = new ArrayDeque<Entry>();

	private final HLLThread thread;

	private boolean disposed = false;
	
	public RCServerUtilLatencySim() {
		thread = new HLLThread();
		thread.start();
	}

	/** When a message is received by the server websocket endpoint, the endpoint will call this method
	 * with the message. */
	public void addMessage(ActiveWSClientSession session, ActiveWSClient client, String str) {
		if(session == null || client == null || str == null) { throw new IllegalArgumentException(); }
		if(disposed) { return; }
		
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

	/** When latency simulation is enabled, this class will delay the sending of received messages by the
	 * arbitrary amount specified in Entry.pushTimeInNanos. 
	 * 
	 * When latency simulation is enabled, All messages received on the endpoint will be sent by this thread. */
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
				
					// Find messages in the queue that are ready to send
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
						// Call the ServerMessageReceiver with the Entry message
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

	/** If latency simulation is enabled, the client WebSocket endpoint will call RCServerUtilLatencySim.addMessage(...) with any messages
	 * that the endpoint receives. addMessage(...) will convert that message into an Entry, where it will be delayed then sent by the HLLThread. 
	 * 
	 **/
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
