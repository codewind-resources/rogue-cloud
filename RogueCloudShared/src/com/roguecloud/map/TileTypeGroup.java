/*
 * Copyright 2019 IBM Corporation
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

import java.util.ArrayList;
import java.util.List;

/** A group of related tiles, used to create a larger sprite that is composed of multiple tiles */
public final class TileTypeGroup {

	// number of (32x32) tiles wide
	private final int width;

	// number of (32x32) tiles tall
	private final int height;
	
	// 1 dimensional array of tiles, in the (x, y) form of: [ (0, 0), (1, 0), (0, 1), (1, 1) ]
	private final TileType[] tileTypes;
	
	private final Boolean[] passable;
	
	public TileTypeGroup(int width, int height, TileType... tileTypes) {
		this.width = width;
		this.height = height;
		this.tileTypes = tileTypes;
		passable = new Boolean[tileTypes.length];
		
		// Default to passable of true, otherwise use the other constructor
		for(int x = 0; x < passable.length; x++) {
			passable[x] = true;
		}
	}
	
	public TileTypeGroup(int width, int height, Object... tileTypes) {
		this.width = width;
		this.height = height;
		
		List<TileType> ttList = new ArrayList<>();
		List<Boolean> bList = new ArrayList<>();
		
		for(int x = 0; x < tileTypes.length; ) {
			TileType tt = (TileType) tileTypes[x];
			ttList.add(tt);
			Boolean b = (Boolean) tileTypes[x+1];
			bList.add(b);
			x += 2;
		}
		
		this.tileTypes = ttList.toArray(new TileType[ttList.size()]);
		this.passable = bList.toArray(new Boolean[bList.size()]);
	}


	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public TileType[] getTileTypes() {
		return tileTypes;
	}
		
	
	public TileType getTypeAt0Coord(int x, int y) {
		int index = y*width+x;
		return tileTypes[index];
	}
	
	public boolean isPassableAtCoord(int x, int y) {
		int index = y*width+x;
		return passable[index];		
	}

	
	/** Create a new copy of this TileTypeGroup, but in the result replace the given tileType 
	 * with the new given type */
	public TileTypeGroup cloneAndReplace(TileType oldType, TileType newType) {
		TileType[] newArray = new TileType[this.tileTypes.length];
		
		for(int x = 0; x < newArray.length; x++) {
			newArray[x] = this.tileTypes[x];
			
			if(newArray[x].equals(oldType)) {
				newArray[x] = newType;
			}
		}
		
		return new TileTypeGroup(this.width, this.height, newArray);
	}
	
}
