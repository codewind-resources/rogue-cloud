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

import com.roguecloud.map.TileType;

/** Parsed monster entries from the monsters.txt file, see MonsterTemplateList. */
public class MonsterTemplate {

	private final String name;
	private final int minLevelRange;
	private final int maxLevelRange;
	private final TileType tileType;
	private final int randomnessWeight;
	
	public MonsterTemplate(String name, int minLevelRange, int maxLevelRange, TileType tileType, int randomnessWeight) {
		this.name = name;
		this.minLevelRange = minLevelRange;
		this.maxLevelRange = maxLevelRange;
		this.tileType = tileType;
		this.randomnessWeight = randomnessWeight;
	}

	public String getName() {
		return name;
	}

	public int getMinLevelRange() {
		return minLevelRange;
	}

	public int getMaxLevelRange() {
		return maxLevelRange;
	}
	
	public TileType getTileType() {
		return tileType;
	}
	
	public int getRandomnessWeight() {
		return randomnessWeight;
	}
}
