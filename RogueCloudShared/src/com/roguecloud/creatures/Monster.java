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

package com.roguecloud.creatures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.roguecloud.Position;
import com.roguecloud.items.ArmourSet;
import com.roguecloud.items.Effect;
import com.roguecloud.items.OwnableObject;
import com.roguecloud.items.Weapon;
import com.roguecloud.json.JsonEffect;
import com.roguecloud.json.JsonVisibleCreature;
import com.roguecloud.map.IMap;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;

/** 
 * This object represents an enemy monster in the world. For a description of the public API, see the ICreature interface.
 */
public final class Monster implements ICreature, IMutableCreature {

	private final Object lock = new Object();
	
	private int currHp_synch_lock = -1;
	
	private int maxHp = -1;
	
	private Weapon weapon = null;
	
	private Position position_synch_lock = null;
	
	private final ArmourSet armourSet;
	
	private final ArrayList<OwnableObject> ownableObject_sync_lock = new ArrayList<>();
	
	private final TileType tileType;
	
	private final ArrayList<Effect> activeEffects_synch_lock = new ArrayList<>();

//	private final MonsterTemplate template;
	
//	private AIContext aiContext;
	
	private int level;
	
	private final long id;
	
	private final String name;
	
	public Monster(JsonVisibleCreature json, ArmourSet armourSet, Weapon weapon) {
		if(weapon == null) { throw new IllegalArgumentException(); }
		
		id = json.getCreatureId(); 
		level = json.getLevel();
		maxHp = json.getMaxHp();
		currHp_synch_lock = json.getCurrHp();
		position_synch_lock = json.getPosition().toPosition();
		this.armourSet = armourSet;
		this.weapon = weapon;
		this.tileType = new TileType((int)json.getTileTypeNumber());
		
		this.name = json.getName();
		
		for(JsonEffect e : json.getEffects()){ 
			activeEffects_synch_lock.add(new Effect(e));
		}
	}
	
	public Monster(String name, long id, Position pos, TileType tileType, /*MonsterTemplate template,*/ Weapon startingWeapon, int level, ArmourSet armourSet) {
		if(name == null || tileType == null || armourSet == null || pos == null || startingWeapon == null) { throw new IllegalArgumentException(); }
		
		this.name = name;
		this.id = id;
		this.position_synch_lock = pos;
		
//		this.template = template;
		this.level = level;
		this.weapon = startingWeapon;
		this.tileType = tileType;
		this.armourSet = armourSet;
	}
	
	
	public void internalUpdateFromJson(JsonVisibleCreature json, ArmourSet armourSet, Weapon weapon) {
		if(weapon == null) { throw new IllegalArgumentException(); }
		
		level = json.getLevel();
		maxHp = json.getMaxHp();
		synchronized(lock) {
			currHp_synch_lock = json.getCurrHp();
			position_synch_lock = json.getPosition().toPosition();
			
			activeEffects_synch_lock.clear();
			
			for(JsonEffect e : json.getEffects()) {
				activeEffects_synch_lock.add(new Effect(e));
			}

		}
		
		armourSet.getAll().stream().forEach( e -> {
			this.armourSet.put(e);
		});
		
		this.weapon = weapon;
	}
	
	
	@Override
	public Position getPosition() {
		synchronized(lock) {
			return position_synch_lock;
		}
	}

	@Override
	public int getMaxHp() {
		return maxHp;
	}

	@Override
	public int getHp() {
		synchronized(lock) {
			return currHp_synch_lock;
		}
	}

	@Override
	public ArmourSet getArmour() {
		return armourSet;
	}

	@Override
	public Weapon getWeapon() {
		return weapon;
	}

	@Override
	public List<OwnableObject> getInventory() {
		ArrayList<OwnableObject> copy = new ArrayList<>();
		synchronized (lock) {
			copy.addAll(ownableObject_sync_lock);
		}
		
		return Collections.unmodifiableList(copy);
	}

//	public MonsterTemplate getTemplate() {
//		return template;
//	}
	
	public int getLevel() {
		return level;
	}
	
	@Override
	public void setPosition(Position position) {
		synchronized(lock) {
			this.position_synch_lock = position;
		}
	}
	
	@Override
	public void setMaxHp(int maxHp) {
		this.maxHp = maxHp;
	}
	
	@Override
	public void setCurrHp(int currHp) {
		synchronized(lock) {
			this.currHp_synch_lock = currHp;
		}
	}
	
	@Override
	public void setWeapon(Weapon weapon) {
		if(weapon == null) { throw new IllegalArgumentException(); }
		this.weapon = weapon;
	}

	@Override
	public TileType getTileType() {
		return tileType;
	}
	
	public final long getId() {
		return id;
	}
	
	@Override
	public boolean isDead() {
		return currHp_synch_lock <= 0;
	}

	@Override
	public boolean isPlayerCreature() {
		return false;
	}

	@Override
	public IMutableCreature fullClone() {
		Monster result;
		synchronized (lock) {
			result = new Monster(name, id, position_synch_lock, this.tileType, this.weapon, this.level, this.armourSet.fullClone());			
		}

		result.setCurrHp(currHp_synch_lock);
		result.setMaxHp(maxHp);
		result.setWeapon(weapon);
		
		synchronized(lock) {
			result.ownableObject_sync_lock.addAll(this.ownableObject_sync_lock);
		}

		return result;
		
	}

	@Override
	public void addToInventory(OwnableObject o) {
		synchronized (lock) {
			if(!ownableObject_sync_lock.contains(o)) {
				ownableObject_sync_lock.add(o);
			}
		}
	}

	@Override
	public boolean removeFromInventory(OwnableObject o) {
		synchronized (lock) {
			return ownableObject_sync_lock.remove(o);
		}
	}
	
	@Override
	public void addEffect(Effect e) {
		synchronized(lock) {
			if(!activeEffects_synch_lock.contains(e)) {
				activeEffects_synch_lock.add(e);
			}
		}		
	}

	@Override
	public boolean removeEffect(Effect e) {
		synchronized(lock) {
			return activeEffects_synch_lock.remove(e);
		}
	}
	
	@Override
	public int hashCode() {
		return (int)(this.id);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Monster)) {
			return false;
		}
		Monster other = (Monster)obj;
		return other.id == id && this.getPosition().equals(other.getPosition());
	}

	@Override
	public String toString() {
		synchronized(lock) {
			return "{Monster - id: "+id+" position: "+position_synch_lock+" hp: "+currHp_synch_lock+" name: "+name+" }";
		}
	}

	@Override
	public List<Effect> getActiveEffects() {
		ArrayList<Effect> result = new ArrayList<>();
		synchronized(lock) {
			result.addAll(activeEffects_synch_lock);
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getLastTickUpdated(IMap map) {
		Position p = getPosition();
		if(p == null) { return Long.MAX_VALUE; }
		
		Tile t = map.getTile(p);
		if(t == null) {
			return Long.MAX_VALUE;
		} else {
			return t.getLastTickUpdated();
		}
	}

}
