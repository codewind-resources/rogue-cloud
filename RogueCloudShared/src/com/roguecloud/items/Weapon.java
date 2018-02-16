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

import com.roguecloud.json.JsonWeapon;
import com.roguecloud.map.TileType;
import com.roguecloud.utils.Logger;

/**
 * A weapon is an object that can be equipped by a player, and that is used offensively in combat to deal damage to other creatures.
 * You can use the calculateWeaponRating(...) method to determine how good a weapon (the higher the weapon rating, the better). 
 * 
 * Weapon damage is calculated by simulating dice rolls (ie a similar system to a dice-based table-top RPG like D&D, or a CRPG like Baldur's Gate):
 * 
 * Average damage done by a creature's weapon per turn:
 *  
 *   * = (number of attack dice) * ((attackDiceSize+1)/2) + attackPlus
 * 
 * This can be represented using tradition table-top RPG notation:
 * 
 *   * (number of attack dice)d(attackDizeSize) + attackPlus
 *   * For example: the traditional notation of 6d4+3, would be a 4-sided dice, rolled six times and all the values totaled, then 3 added to that total.  
 *  
 * Hit rating determines how likely you are to hit the creature you are attacking:
 * 
 *   * Hit chance % = hitRating of attacker's weapon / (sum of defense value for all defender's armour)  
 * 
 * 
 */
public class Weapon implements IObject {
	
	private static final Logger log = Logger.getInstance();

	private final String name;
	private final int numAttackDice;
	private final int attackDiceSize;
	private final int attackPlus;
	
	private final int hitRating;
	
	private final WeaponType type;
	
	private final TileType tileType;

	private final long id;
	
	public Weapon(long id, String name, int numAttackDice, int attackDiceSize, int attackPlus, int hitRating, WeaponType type, TileType tileType) {
		this.id = id;
		this.name = name;
		this.numAttackDice = numAttackDice;
		this.attackDiceSize = attackDiceSize;
		this.hitRating = hitRating;
		this.attackPlus = attackPlus;
		this.type = type;
		this.tileType = tileType;
	}
	
	public Weapon(JsonWeapon json) {
		this.id = json.getId();
		this.name = json.getName();
		this.numAttackDice = json.getNumAttackDice();
		this.attackDiceSize = json.getAttackDiceSize();
		this.hitRating = json.getHitRating();
		this.attackPlus = json.getAttackPlus();
		this.type = WeaponType.getByName(json.getType());
		this.tileType = new TileType(json.getTile());
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.WEAPON;
	}

	@Override
	public String getName() {
		return name;
	}

	/** The number of rolls of the attack dice (see class description for details.) */
	public int getNumAttackDice() {
		return numAttackDice;
	}

	/** The size of the attack dice (see class description for details.) */
	public int getAttackDiceSize() {
		return attackDiceSize;
	}

	/** The damage addition after the attack dice rolls (see class description for details.) */
	public int getAttackPlus() {
		return attackPlus;
	}

	/** How likely a weapon is to hit armour (see class description for details.)  */
	public int getHitRating() {
		return hitRating;
	}

	/** A weapon may be one-handed or two-handed. */
	public WeaponType getType() {
		return type;
	}
	

	public final long getId() {
		return id;
	}

	/** May be used to determine how good a weapon is (the higher the better). */
	public int calculateWeaponRating() {
		return (getAttackDiceSize()*getNumAttackDice())+getAttackPlus();
	}
	
	@Override
	public String toString() {
		return "("+id+") name: "+name;
	}
	
	/** Weapons may require one or both hands -- this is not currently used by combat calculations. */
	public static enum WeaponType {
		ONE_HANDED("One-handed"), TWO_HANDED("Two-handed");
		
		private String typeName;
		
		private WeaponType(String typeName) {
			this.typeName = typeName;
		}
		
		public String getName() {
			return typeName;
		}
		
		public static WeaponType getByName(String name) {
			
			Optional<WeaponType> o = Arrays.stream(values()).filter( e -> e.getName().equals(name)).findFirst();
			if(o.isPresent()) {
				return o.get();
			} else {
				log.severe("Invalid weapon name: "+name, null);
				return null;
			}

		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Weapon)) {
			return false;
		}
		Weapon other = (Weapon)obj;
		
		return other.getId() == getId();
		
	}

	// Internal methods ----------------------------------------------------------------------------------
	
	public JsonWeapon toJson() {
		JsonWeapon jw = new JsonWeapon();

		jw.setName(name);
		jw.setNumAttackDice(numAttackDice);
		jw.setAttackDiceSize(attackDiceSize);
		jw.setAttackPlus(attackPlus);
		jw.setHitRating(hitRating);
		jw.setType(type.getName());
		jw.setId(id);
		jw.setTile(tileType.getNumber());
		return jw;
	}

	@Override
	public TileType getTileType() {
		return tileType;
	}

}
