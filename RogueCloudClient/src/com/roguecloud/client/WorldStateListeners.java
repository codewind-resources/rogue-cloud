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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.roguecloud.client.ClientWorldState.ClientWorldStateListener;

/** 
 * See ClientWorldStateListener for a description of what world state listeners are and how they are used.
 * 
 * This class is a singleton that contains a list of all the active WorldStateListeners.
 * 
 * Listeners are added to this class when they are first created, and never removed.
 *  
 * For internal server use only 
 **/
public class WorldStateListeners {
	
	private WorldStateListeners() {
	}
	
	private static final WorldStateListeners instance = new WorldStateListeners();
	
	public static WorldStateListeners getInstance() {
		return instance;
	}
	
	// TODO: We're not removing WorldStateListeners.
	
	// ------------------------------
	
	private final List<WSLEntry> listeners_synch = new ArrayList<>();

	public void addListener(ClientWorldStateListener listener) {
		
		synchronized (listeners_synch) {
			listeners_synch.add(new WSLEntry(listener));

			// Flag listeners which are closed
			for(WSLEntry cwsl : listeners_synch) {
				if(!cwsl.listener.isClientOpen() && cwsl.clientExpireTimeInNanos == null) {
					cwsl.clientExpireTimeInNanos = System.nanoTime()+TimeUnit.NANOSECONDS.convert(60, TimeUnit.SECONDS);
				}
			}
		
			// Remove flagged listeners that have expired.
			for(Iterator<WSLEntry> it = listeners_synch.iterator(); it.hasNext(); ) {
				WSLEntry curr = it.next();
				if(curr.clientExpireTimeInNanos != null && curr.clientExpireTimeInNanos > System.nanoTime()) {
					it.remove();
				}
			}
		}
	}
	
	public List<ClientWorldStateListener> getListeners() {
		List<ClientWorldStateListener> result = new ArrayList<ClientWorldStateListener>();
		
		synchronized (listeners_synch) {
			result.addAll(listeners_synch.stream().map(e -> e.listener).collect(Collectors.toList()));
		}
		
		return result;
	}

	/** Internal struct-style class containing the listener, and when it expires as an absolute time */
	private static class WSLEntry {
		
		public WSLEntry(ClientWorldStateListener listener) {
			if(listener == null) { throw new IllegalArgumentException(); }
			
			this.listener = listener;
		}
		
		ClientWorldStateListener listener;
		Long clientExpireTimeInNanos;
	}
	
}
