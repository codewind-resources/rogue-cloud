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

import com.roguecloud.RCRuntime;
import com.roguecloud.Position;
import com.roguecloud.utils.Coord;
import com.roguecloud.utils.Logger;

/**
 *
 * In addition to implementing the IMap and IMutableMap interface, this class maintains the following properties:
 * - The contents of the map should only be mutated from the game thread.
 * - To detect the above, getTile(...) methods should only be used for read, and getTileForWrite(...) should be used for write.
 * - cloneForRead() will produce a read-only map, which will not mutate and is safe to share across multiple threads. 
 *
 * The map is backed by two layers:
 * - Mutable 'top' layer: A hash map, representing tiles that have been written to (mutated) since the map was created.
 * - Immutable 'bottom' layer: A 2d graph, representing an (x, y) grid of map titles.  
 * 
 * All writes to the map, using putTile(...), or getTileForWrite(...) will write to the top layer, leaving the bottom layer
 * untouched. This is used to allow us to efficiently clone (using cloneForRead()) and that share that clone across mutiple
 * threads, while still allowing us to write to the map after the cloned object has been shared.
 * 
 * However, the 'top' layer is significantly less efficient than the bottom layer, so it is necessary to periodically 
 * copy ("collapse") the top layer into the bottom layer, and to clear the top layer contents. The method
 * to collapse the map is collapseOverlayIntoNewMap(...)
 * 
 * For internal use - see IMap for the public API of map.
 */
public final class RCArrayMap implements IMap, IMutableMap {
	
	private static final Logger log = Logger.getInstance();

	private final int xSize, ySize;

	private final Tile[] tileArray;
	
	private HashMap<Coord, Tile> overlay;
	
	private boolean clonedForRead = false;
	
	public RCArrayMap(int xSize, int ySize) {
		this.xSize = xSize;
		this.ySize = ySize;
		tileArray = new Tile[xSize*ySize];
	}
	
	private RCArrayMap(int xSize, int ySize, Tile[] tileArray) {
		this.xSize = xSize;
		this.ySize = ySize;
		this.tileArray = tileArray;
	}

	public void setClonedForRead(boolean clonedForRead) {
		this.clonedForRead = clonedForRead;
	}
	
	public boolean isClonedForRead() {
		return clonedForRead;
	}
	
	public final void putTile(Position p, Tile t) {
		putTile(p.getX(), p.getY(), t);
	}
	
	public final void putTile(int x, int y, Tile t) {
		tileArray[x*ySize+y] = t;
	}
	
	public final Tile getTile(Position p) {
		return getTile(p.getX(), p.getY());
	}
	
	public final Tile getTile(int x, int y) {
		Coord imc = new Coord(x, y);
		
		if(overlay != null) {
			Tile overlayTile = overlay.get(imc);
			
			if(overlayTile != null) {
				overlayTile.setTileForRead(true);
				return overlayTile;
			}
		}
		
		int index = x*ySize+y;
		
		if(tileArray.length <= index || index < 0) {
			return null;
		}
		
		Tile tile = tileArray[index];
		if(tile != null) {
			tile.setTileForRead(true);
		}
		return tile;
	}
	
	
	public final Tile getTileForWriteUnchecked(Position p) {
		return this.getTileForWrite(p.getX(), p.getY(), true);
	}

	
	public final Tile getTileForWrite(Position p) {
		return this.getTileForWrite(p.getX(), p.getY());
	}
	
	
	public final Tile getTileForWrite(int x, int y) {
		return getTileForWrite(x, y, false);
	}
	
	@SuppressWarnings("unused")
	private final Tile getTileForWrite(int x, int y, boolean ignoreAssert) {
		if(!ignoreAssert) {
			RCRuntime.assertGameThread();
		}
		
		if(RCRuntime.CHECK && isClonedForRead()) {
			log.severe("Attempting to write to map that was cloned for read", null);
			throw new RuntimeException("Attempting to write to map that was cloned for read.");
		}
		
		Coord imc = new Coord(x, y); // TODO: LOW - Move IMC to it's own class, optimize around it above.

		Tile t = getTile(x, y).shallowCloneUnchecked();
		if(overlay == null) {
			overlay = new HashMap<>();
		}
		overlay.put(imc, t);
				
		return t;
	}
	
	
	public final int getXSize() {
		return xSize;
	}
	
	public final int getYSize() {
		return ySize;
	}
	
	public final RCArrayMap cloneForRead() {
		RCArrayMap result = new RCArrayMap(xSize, ySize, tileArray);
		result.setClonedForRead(true);
		
		if(overlay != null) {
			// Shallow copy of the existing overlay
			HashMap<Coord, Tile> newOverlay = new HashMap<>(overlay.size());
			newOverlay.putAll(overlay);
			result.overlay = newOverlay;
		}
		
		return result;
	}	
	
	
	public RCArrayMap collapseOverlayIntoNewMap() {
		final Tile[] tile = new Tile[this.tileArray.length];
		
		System.arraycopy(this.tileArray, 0, tile, 0, this.tileArray.length);
		if(this.overlay != null) {
			this.overlay.entrySet().forEach( e -> {
				
				tile[e.getKey().getX()*ySize+e.getKey().getY()] = e.getValue().shallowClone();
				
			});
		}
		
		return new RCArrayMap(this.xSize, this.ySize, tile);
		
	}
	
}
