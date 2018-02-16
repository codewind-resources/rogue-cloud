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
 * This object represents a player creature in the world. For a description of the public API, see the ICreature interface.
 */
public final class PlayerCreature  implements ICreature, IMutableCreature {

	private final Object lock = new Object();
	
	private int currHp_synch_lock = -1;
	
	private int maxHp = -1;
	
	private int level = 1;
	
	private Weapon weapon = null;
	
	private Position position_synch_lock = null;
	
	private final ArmourSet armourSet;
	
	private final ArrayList<OwnableObject> ownableObject_synch_lock = new ArrayList<>();

	private final ArrayList<Effect> activeEffects_synch_lock = new ArrayList<>();
	
	private final long creatureId;
	
	private final TileType tileType;
	
	private final String name;
	
	public PlayerCreature(String name, long creatureId, Position p, TileType tileType, ArmourSet armourSet) {
		if(name == null || tileType == null || armourSet == null || p == null) { throw new IllegalArgumentException(); }
		
		this.tileType = tileType;
		this.creatureId = creatureId;
		this.armourSet = armourSet;
		this.position_synch_lock = p;
		this.name = name;
	}
	
	public PlayerCreature(JsonVisibleCreature json, ArmourSet armourSet, Weapon weapon) {
		if(weapon == null) { throw new IllegalArgumentException(); }
		
		this.name = json.getName();
		this.currHp_synch_lock = json.getCurrHp();
		this.maxHp = json.getMaxHp();
		this.level = json.getLevel();
		this.weapon = weapon;
		this.position_synch_lock = json.getPosition().toPosition();
		this.armourSet = armourSet;
		this.creatureId = json.getCreatureId();
		this.tileType = new TileType((int)json.getTileTypeNumber());

		
		for(JsonEffect e : json.getEffects()){ 
			activeEffects_synch_lock.add(new Effect(e));
		}
	}
	
	public void internalUpdateFromJson(JsonVisibleCreature json, ArmourSet armourSet, Weapon weapon) {
		if(weapon == null) { throw new IllegalArgumentException(); }
		
		synchronized(lock) {
			this.currHp_synch_lock = json.getCurrHp();
			this.position_synch_lock = json.getPosition().toPosition();
			
			activeEffects_synch_lock.clear();
			
			for(JsonEffect e : json.getEffects()) {
				activeEffects_synch_lock.add(new Effect(e));
			}
		}
		
		this.maxHp = json.getMaxHp();
		this.level = json.getLevel();
		this.weapon = weapon;
		
		
		armourSet.getAll().stream().forEach( e -> {
			this.armourSet.put(e);
		});
	}
	
	@Override
	public Position getPosition() {
		synchronized(lock) {
			return position_synch_lock;
		}
	}

	@Override
	public TileType getTileType() {
		return tileType;
	}

	@Override
	public void setPosition(Position p) {
		if(p == null) { throw new IllegalArgumentException(); }
		synchronized (lock) {
			this.position_synch_lock = p;	
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
		ArrayList<OwnableObject> result = new ArrayList<>();
		synchronized (lock) {
			result.addAll(ownableObject_synch_lock);
		}
		
		return Collections.unmodifiableList(result);
	}

	@Override
	public long getId() {
		return creatureId;
	}

	@Override
	public int getLevel() {
		return level;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}

	public int getCurrHp() {
		synchronized(lock) {
			return currHp_synch_lock;
		}
	}
	
	@Override
	public boolean isDead() {
		synchronized(lock) {
			return currHp_synch_lock <= 0;
		}
	}

	@Override
	public boolean isPlayerCreature() {
		return true;
	}

	@Override
	public IMutableCreature fullClone() {
		PlayerCreature result;
		synchronized(lock) {
			result = new PlayerCreature(this.name, this.creatureId, this.position_synch_lock, this.tileType, this.armourSet.fullClone());
			result.setCurrHp(currHp_synch_lock);
		}
		result.setLevel(level);
		result.setMaxHp(maxHp);
		
		result.setWeapon(weapon);
		
		synchronized(lock) {
			result.ownableObject_synch_lock.addAll(this.ownableObject_synch_lock);
		}
		
		return result;
		
	}

	@Override
	public void addToInventory(OwnableObject o) {
		synchronized (lock) {
			if(!ownableObject_synch_lock.contains(o)) {
				ownableObject_synch_lock.add(o);
			}
		}
	}

	@Override
	public boolean removeFromInventory(OwnableObject o) {
		synchronized (lock) {
			return ownableObject_synch_lock.remove(o);
		}
	}
	
	
	@Override
	public int hashCode() {
		return (int) creatureId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof PlayerCreature)) {
			return false;
		}
		PlayerCreature other = (PlayerCreature)obj;
		return other.creatureId == creatureId && this.getPosition().equals(other.getPosition());
	}
	
	@Override
	public String toString() {
		synchronized(lock) {
			return "PlayerCreature - id: "+creatureId+" position: "+position_synch_lock;
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
