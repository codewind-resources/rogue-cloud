/*
 * Copyright 2018, 2019 IBM Corporation
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

/**
 * A simple implementation of IMap that is backed by a HashMap. While it is not thread safe, it can be fully cloned 
 * by calling cloneMap(...); the returned object will be a clone of the map (with a shallow clone of the tile contents) 
 * 
 *  For internal use only - see IMap for the public API of map. 
 *  
 * This class implements IMutableMap, but only supports a small subset of the IMutableMap methods. For 
 * unsupported methods, an UnsupportedOperationException will be thrown.
 *  
 **/
public class RCCloneMap implements IMutableMap {

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

	/** Simple (x, y) coordinate class */
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

	@Override
	public IMap cloneForRead() {
		throw new UnsupportedOperationException();
	}


	@Override
	public Tile getTileForWrite(int x, int y) {
		throw new UnsupportedOperationException();
	}


	@Override
	public Tile getTileForWrite(Position p) {
		throw new UnsupportedOperationException();
	}


	@Override
	public Tile getTileForWriteUnchecked(Position p) {
		throw new UnsupportedOperationException();
	}
}
