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

/** For internal use */
public class DoorTerrain implements ITerrain {
	
	// TODO: LOW - Remove this class once it's replacement is in place.
	
	private final TileType tileType;
	
	private boolean isDoorOpen = false;
	
	public DoorTerrain(TileType tileType, boolean isDoorOpen) {
		this.tileType = tileType;
		this.isDoorOpen = isDoorOpen;
	}
	
	@Override
	public TileType getTileType() {
		return tileType;
	}

	@Override
	public boolean isPresentlyPassable() {
		return isDoorOpen;
	}
	

}
