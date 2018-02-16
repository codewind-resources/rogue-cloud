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

package com.roguecloud.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.roguecloud.items.Armour.ArmourType;
import com.roguecloud.utils.Logger;

/** 
 * This class may be called to determine what armour pieces another character (or your own character) have equipped.
 * 
 * (This may only be used to view the armour; to equip or unequip armour, use the EquipAction action.)  
 **/
public class ArmourSet {
	
	private static final Logger log = Logger.getInstance();
	
	private final HashMap<Armour.ArmourType, Armour> map_synch = new HashMap<>();

	/** Returns list of all the equppied armour, or an empty list if none is equippred. */
	public List<Armour> getAll() {
		
		List<Armour> result = new ArrayList<>();

		synchronized(map_synch) {
			result.addAll(map_synch.values());
		}
		
		return result;
	}
	
	/** Return an equipped piece of armour of the specified type, or null if no armour is equipped of that type. */
	public Armour get(ArmourType at) {
		if(at == null) { throw new IllegalArgumentException(); }
		
		synchronized(map_synch) {
			return map_synch.get(at);
		}
	}

	// Internal methods only ----------------------------------------------------------------------------------
	
	public void put(Armour a) {
		if(a == null) { throw new IllegalArgumentException(); }
		synchronized(map_synch) {
			map_synch.put(a.getType(), a);
		}
	}
	
	public boolean remove(Armour a) {
		if(a == null) { throw new IllegalArgumentException(); } 
			
		synchronized(map_synch) {
			Armour armourOfType = map_synch.remove(a.getType());
			if(a == null || !armourOfType.equals(a)) { 
				log.severe("Could not remove armour "+a +" from armour set.", null);
				return false;
			}
			
			return true;
		}
	}
	
	public ArmourSet fullClone() {
		synchronized(map_synch) {
			ArmourSet result = new ArmourSet();
			result.map_synch.putAll(map_synch);
			return result;
		}
	}
}
