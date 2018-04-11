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

import com.roguecloud.Position;
import com.roguecloud.map.IMap;
import com.roguecloud.map.Tile;

/** 
 * The purpose of this class is to allow ClientWorldState to transparently update the map that the player code is running on,
 * while allowing the player code to maintain a single consistent reference to the map.
 * 
 * This allows the player to store a reference to the map from any WorldState update, and rely on the fact that it will
 * always contain the most up-to-date map data.
 * 
 * See IMap interface for details on the available methods. 
 **/
public class ClientMap implements IMap {

	private final Object lock = new Object();

	private IMap internalMap_synch_lock;
	
	public ClientMap(IMap initialMap) {
		this.internalMap_synch_lock = initialMap;
	}
	
	public void updateMap(IMap m) {
		synchronized (lock) {
			internalMap_synch_lock = m;
		}
	}
	
	@Override
	public Tile getTile(Position p) {
		synchronized(lock) {
			return internalMap_synch_lock.getTile(p);
		}
	}

	@Override
	public Tile getTile(int x, int y) {
		synchronized(lock) {
			return internalMap_synch_lock.getTile(x, y);
		}
	}

	@Override
	public int getXSize() {
		synchronized(lock) {
			return internalMap_synch_lock.getXSize();
		}
	}

	@Override
	public int getYSize() {
		synchronized(lock) {
			return internalMap_synch_lock.getYSize();
		}
	}
	
}
