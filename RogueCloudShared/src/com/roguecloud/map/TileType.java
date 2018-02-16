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

package com.roguecloud.map;

import java.util.HashMap;
import java.util.Map;

/** For internal server use only: the graphical representation of an item or tile */
public final class TileType {
	
	/** The numerical name of the .png file that contains the tile data. */
	private final int number;
	
	/** Rotation of the tile: 0, 90, 180, 270. */
	private final int rotation;
	
	private static final HashMap<Integer, String> numberMap_synch = new HashMap<>();
	
    public TileType(int number) {
    	if(number <= 0) { throw new IllegalArgumentException(); }
    	
    	this.number = number;
    	this.rotation = 0;
    	synchronized(numberMap_synch) {
    		numberMap_synch.put(number, null);
    	}
    }	 

	
    public TileType(int number, int rotation) {
    	this.number = number;
    	this.rotation = rotation;
    	synchronized(numberMap_synch) {
    		numberMap_synch.put(number, null);
    	}
    }	 

    public TileType(int number, int rotation, String name) {
    	this.number = number;
    	this.rotation = rotation;
    	synchronized(numberMap_synch) {
    		numberMap_synch.put(number, name);
    	}
    }	 

    
    public int getNumber() {
		return number;
	}
    
    public int getRotation() {
		return rotation;
	}
    
    public static Map<Integer, String> getValues() {
    	synchronized(numberMap_synch) {
    		HashMap<Integer, String> result = new HashMap<>();
    		
    		result.putAll(numberMap_synch);
//    		ArrayList<Integer> result = new ArrayList<>();
//    		result.addAll(numberMap_synch.keySet());
    		return result;
    	}
	}
	// TODO: LOW - Find a better way to handle this map.

}
