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

import java.util.HashMap;

import com.roguecloud.Position;

/** For internal use - see IMap for the public API of map. */
public class RCCloneMap implements IMap {

	private final int xSize, ySize;
	
	private final HashMap<InnerMapCoord, Tile> map = new HashMap<>();
	
	public RCCloneMap(int xSize, int ySize) {
		this.xSize = xSize;
		this.ySize = ySize;
	}
	
	
	public final RCCloneMap cloneMap() {
		
		RCCloneMap map = new RCCloneMap(xSize, ySize);
		
		this.map.entrySet().stream().forEach( e -> {
			
			Tile t = e.getValue().shallowCloneUnchecked();
//			Tile t = e.getValue().fullClone();
			
			map.putTile(e.getKey().x, e.getKey().y, t);
			
		});
		
		return map;
		
	}
	
	
	public final void putTile(Position p, Tile t) {
		putTile(p.getX(), p.getY(), t);
	}
	
	public final void putTile(int x, int y, Tile t) {
		InnerMapCoord imc = new InnerMapCoord(x, y);
		
		map.put(imc, t);
	}
	
	public final Tile getTile(Position p) {
		return getTile(p.getX(), p.getY());
	}
	
	public final Tile getTile(int x, int y) {
		InnerMapCoord imc = new InnerMapCoord(x, y);
		
		return map.get(imc);
	}
	
	public int getXSize() {
		return xSize;
	}
	
	public int getYSize() {
		return ySize;
	}
	
	public void dispose() {
		map.clear();
	}
	
	private final static class InnerMapCoord {
		final int x;
		final int y;
		
		public InnerMapCoord(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		@Override
		public int hashCode() {
			return 32768*x + y;
		}
		
		@Override
		public boolean equals(Object obj) {
			InnerMapCoord imc = (InnerMapCoord)obj;
			
			if(x != imc.x) { return false; }
			if(y != imc.y) { return false; }
			
			return true;
			
		}
		
	}
}
