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

package com.roguecloud.json;

import java.util.ArrayList;
import java.util.List;

public class JsonVisibleCreature {

	JsonPosition position;

	long creatureId;
	int maxHp;
	int currHp;

	int level;

	long weaponId;

	boolean isPlayer = false;

	long tileTypeNumber;

	String name = "";

	List<Long> armourIds = new ArrayList<>();

	List<JsonEffect> effects = new ArrayList<>();

	public JsonVisibleCreature() {
	}

	public long getCreatureId() {
		return creatureId;
	}

	public void setCreatureId(long creatureId) {
		this.creatureId = creatureId;
	}

	public int getCurrHp() {
		return currHp;
	}

	public void setCurrHp(int currHp) {
		this.currHp = currHp;
	}

	public int getMaxHp() {
		return maxHp;
	}

	public void setMaxHp(int maxHp) {
		this.maxHp = maxHp;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public long getWeaponId() {
		return weaponId;
	}

	public void setWeaponId(long weaponId) {
		this.weaponId = weaponId;
	}

	public List<Long> getArmourIds() {
		return armourIds;
	}

	public void setArmourIds(List<Long> armourIds) {
		this.armourIds = armourIds;
	}

	public JsonPosition getPosition() {
		return position;
	}

	public void setPosition(JsonPosition position) {
		this.position = position;
	}

	public void setPlayer(boolean isPlayer) {
		this.isPlayer = isPlayer;
	}

	public boolean isPlayer() {
		return isPlayer;
	}

	public List<JsonEffect> getEffects() {
		return effects;
	}

	public void setEffects(List<JsonEffect> effects) {
		this.effects = effects;
	}

	public long getTileTypeNumber() {
		return tileTypeNumber;
	}

	public void setTileTypeNumber(long tileTypeNumber) {
		this.tileTypeNumber = tileTypeNumber;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
