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

import java.util.HashMap;
import java.util.Map;

/**
 * This is a singleton that maps the player's UUID, to the currently active ClientState for that UUID.  
 * 
 * Currently: 
 * - Written to by StartAgentServlet and ClientStandaloneMain
 * - Read from by LibertyWsBrowserEndpoint
 * 
 * For internal server-side use only */
public class ClientMappingSingleton {
	
	private static final ClientMappingSingleton instance = new ClientMappingSingleton(); 
	
	private ClientMappingSingleton() {
	}
	
	public static ClientMappingSingleton getInstance() {
		return instance;
	}
	// -----------------------------
	
	private final Object lock = new Object();
	
	private final Map<String /* player uuid (lowercase) */, ClientState> stateMap_synch_lock = new HashMap<>(); 

	public void putClientState(String uuid, ClientState state) {
		synchronized(lock) {
			stateMap_synch_lock.put(uuid.toLowerCase(), state);
		}
	}
	
	public void removeClientState(String uuid) {
		synchronized(lock) {
			stateMap_synch_lock.remove(uuid.toLowerCase());
		}
	}
	
	public ClientState getClientState(String uuid) {
		synchronized(lock) {
			return stateMap_synch_lock.get(uuid.toLowerCase());
		}
	}
	
}
