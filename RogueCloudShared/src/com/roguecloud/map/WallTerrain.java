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

/** Walls are impassable terrain. */
public class WallTerrain implements ITerrain {

	private TileType tileType;
	
	public WallTerrain(TileType tileType) {
		this.tileType =  tileType;
	}

	@Override
	public TileType getTileType() {
		return tileType;
	}

	@Override
	public boolean isPresentlyPassable() {
		return false;
	}

}
