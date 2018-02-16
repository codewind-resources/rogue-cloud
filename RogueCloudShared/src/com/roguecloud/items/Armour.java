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

import java.util.Arrays;
import java.util.Optional;

import com.roguecloud.json.JsonArmour;
import com.roguecloud.map.TileType;
import com.roguecloud.utils.Logger;

/** Armour is a type of item that reduces damage received in combat. It may be equipped by a character in a variety of different
 * armour slots, such as head, chest, legs, feet, and so on.
 * 
 * The main stat of armour is defense: each item contributes a certain amount of defense to an overall defense stat, which is used in  
 * combat calculations to reduce overall damage received.
 * 
 * See the Weapon class for a description of the combat system.
 * 
 * Armour may be equipped by a character by using the EquipAction class.
 * 
 **/
public class Armour implements IObject {

	private static final Logger log = Logger.getInstance();
	
	private final ArmourType type;
	
	private final String name;
	
	private final int defense;
	
	private final TileType tileType;
	
	private final long id;
	
	public Armour(JsonArmour json) {
		this.type = ArmourType.getByName(json.getType());
		this.name = json.getName();
		this.defense = json.getDefense();
		this.tileType = new TileType(json.getTile());
		this.id = json.getId();
	}
	
	public Armour(long id, String name, int defense, ArmourType type, TileType tileType) {
		this.id = id;
		this.defense = defense;
		this.name = name;
		this.type = type;
		this.tileType = tileType;
	}

	/** Name of the item */
	public String getName() {
		return name;
	}

	/** The defense stat of the item: during combat, the defense stats of all equipped items are aggregated and used to reduce incoming damage. */
	public int getDefense() {
		return defense;
	}
	
	@Override
	public ObjectType getObjectType() {
		return ObjectType.ARMOUR;
	}
	
	/** What type of armous is it? Head, Chest, Legs, Feet, etc? */
	public ArmourType getType() {
		return type;
	}
	
	/** A unique ID for this particular item. */
	public long getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return "("+id+") name: "+name+" defense: "+defense;
	}

	/** A character may wear multiple pieces of armour at time, one for each type or slot. */
	public static enum ArmourType {
		
		HEAD("Head"), CHEST("Chest"), LEGS("Legs"), FEET("Feet"), SHIELD("Shield");

		private String name;
		
		ArmourType(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public static ArmourType getByName(String name) { 
			Optional<ArmourType> o = Arrays.stream(values()).filter( e -> e.getName().equals(name)).findFirst();
			if(o.isPresent()) {
				return o.get();
			} else {
				log.severe("Invalid armour name: "+name, null);
				return null;
			}
		}
		
	}

	// Internal methods only ----------------------------------------------------

	
	@Override
	public TileType getTileType() {
		return tileType;
	}
	

	public JsonArmour toJson() {
		JsonArmour a = new JsonArmour();
		a.setType(type.getName());
		a.setName(name);
		a.setDefense(defense);
		a.setId(id);
		a.setTile(tileType.getNumber());
		return a;
		
	}

}
