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

package com.roguecloud.client.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import javax.websocket.Session;

import com.roguecloud.RCRuntime;
import com.roguecloud.client.ISessionWrapper;
import com.roguecloud.utils.Logger;

/** 
 * This class is used to simulate latency ("lag") between the Rogue Cloud client and Rogue Cloud server. When this class is enabled,
 * messages received on client's WebSocket endpoint are delayed by an arbitrary amount of time. This simulates client-side receive latency.
 * See also the RCServerUtilLatencySim for simulating receive latency on the server side.  
 * 
 * Latency is a major factor in how Rogue Cloud is designed: it is necessary that players must still be able to interact with
 * the game world even if they are far enough away from the physical location of the server to ensure that the 
 * information received by the client is slightly old.  
 * 
 * For example, if a player from Singapore connected to a server on the North American East Coast, that player would receive
 * and send actions approximately 2-3 frames behind other players (and behind monsters in the game world). However, we still
 * need to provide a good experience for such a player.
 * 
 * For internal server use only.
 **/
public class RCUtilLatencySim {

	private static final Logger log = Logger.getInstance();
	
	private final Queue<Entry> queue_synch = new ArrayDeque<Entry>();

	private final HLLThread thread;

	private boolean disposed = false;
	
	public RCUtilLatencySim() {
		thread = new HLLThread();
		thread.start();
	}

	/** One way to add a message is to specify the destination of that message using ILatencySimReceiver. */
	public void addMessage(ILatencySimReceiver lsr, String str) {
		if(lsr == null) { throw new IllegalArgumentException(); }
		if(disposed) { return; }
		
//		System.out.println("adding interface msg: "+str);
		
		long pushTime = System.nanoTime()+(long)(Math.random()*(RCRuntime.MAX_LATENCY_SIM_IN_NANOS-RCRuntime.MIN_LATENCY_SIM_IN_NANOS))+RCRuntime.MIN_LATENCY_SIM_IN_NANOS;
		
		synchronized(queue_synch) {
			queue_synch.add(new Entry(str, lsr, pushTime));
			queue_synch.notify();
		}
		
	}

	/** The second way to add a message is to specify the session wrapper and session. */
	public void addMessage(ISessionWrapper wrapper, String str, Session session) {
		if(session == null || wrapper == null) { throw new IllegalArgumentException(); }
		if(disposed) { return; }
		
//		System.out.println("adding msg: "+str);
		
		long pushTime = System.nanoTime()+(long)(Math.random()*(RCRuntime.MAX_LATENCY_SIM_IN_NANOS-RCRuntime.MIN_LATENCY_SIM_IN_NANOS))+RCRuntime.MIN_LATENCY_SIM_IN_NANOS;
		
		synchronized(queue_synch) {
			queue_synch.add(new Entry(str, wrapper, session, pushTime));
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
				
				// Send all the messages in the queue
				for(Entry entry : toSend) {
					try {
						// If addMessage was called with an object that implements ILatencySimReceiver, then call that interface
						if(entry.getLatencySimReceiver() != null) {
							entry.getLatencySimReceiver().addMessageToSend(entry.getStr());
						} else {
							// ... otherwise, just call receiveJson on the session wrapper.
							entry.getWrapper().receiveJson(entry.getStr(), entry.getSession());	
						}
						
					} catch(Exception e) {
						e.printStackTrace();
						log.severe("Unexpected exception on message send", e, null);
					}
				}
				
				
				try {
					// Wait a maximum of 10 msecs, depending on how long it took the 'send all messages' block above to complete
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

	/** If latency simulation is enabled, the client WebSocket endpoint will call RCUtilLatencySim.addMessage(...) with any messages
	 * that the endpoint receives. addMessage(...) will convert that message into an Entry, where it will be delayed then sent by the HLLThread. 
	 *
	 * One (and only one) of the following must be true:
	 * A) wrapper/session/str are not null
	 * B) lsr/str are not null
	 * 
	 * These correspond to addMessage(...) signatures above.
	 * 
	 **/
	private static class Entry {

		/** JSON message received by the client endpoint, which the RCUtilLatencySim class will send after a delay */
		private final String str;

		/** An absolute time in nanos; the 'str' message should be sent after System.nanoTime() > pushTimeInNanos. */
		private final long pushTimeInNanos;

		private final ISessionWrapper wrapper;
		private final Session session;
		
		private final ILatencySimReceiver lsr;
		
		private Entry(String str, ILatencySimReceiver lsr, long addTimeInNanos) {
			this.str = str;
			this.lsr = lsr;
			this.wrapper = null;
			this.session = null;
			this.pushTimeInNanos = addTimeInNanos;
		}
		
		private Entry(String str, ISessionWrapper wrapper, Session session, long addTimeInNanos) {
			this.str = str;
			this.wrapper = wrapper;
			this.session = session;
			this.pushTimeInNanos = addTimeInNanos;
			this.lsr = null;
		}

		public String getStr() {
			return str;
		}
		
		public long getPushTimeInNanos() {
			return pushTimeInNanos;
		}

		public Session getSession() {
			return session;
		}
		
		public ISessionWrapper getWrapper() {
			return wrapper;
		}
		
		public ILatencySimReceiver getLatencySimReceiver() {
			return lsr;
		}

	}
	
	/** Once we have delayed a message by an arbitrary delay, we then need to pass it on to
	 * the original intended receiver. The original intended receiver needs to implements interface, and 
	 * then it can receive delayed messages from the RCUtilLatencySim class. */
	public interface ILatencySimReceiver {
		
		public void addMessageToSend(String str);
	}
}
