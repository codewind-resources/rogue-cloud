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

public class JsonWeapon {

	private String name;
	private int numAttackDice;
	private int attackDiceSize;
	private int attackPlus;

	private int hitRating;

	private String type;

	private long id;

	private int tile;

	public JsonWeapon() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getNumAttackDice() {
		return numAttackDice;
	}

	public void setNumAttackDice(int numAttackDice) {
		this.numAttackDice = numAttackDice;
	}

	public int getAttackDiceSize() {
		return attackDiceSize;
	}

	public void setAttackDiceSize(int attackDiceSize) {
		this.attackDiceSize = attackDiceSize;
	}

	public int getAttackPlus() {
		return attackPlus;
	}

	public void setAttackPlus(int attackPlus) {
		this.attackPlus = attackPlus;
	}

	public int getHitRating() {
		return hitRating;
	}

	public void setHitRating(int hitRating) {
		this.hitRating = hitRating;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getTile() {
		return tile;
	}

	public void setTile(int tile) {
		this.tile = tile;
	}

}
