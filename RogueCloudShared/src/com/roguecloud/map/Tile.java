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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.roguecloud.RCRuntime;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.creatures.IMutableCreature;
import com.roguecloud.items.GroundObject;
import com.roguecloud.items.IGroundObject;

/** 
 * A tile represents the contents of a coordinate on the world map. A tile may contain creatures, items, or structures such as buildings/walls.
 * 
 *  The most useful methods for writing an agent are:
 *  
 *  - getGroundObjects() - What objects (weapons/armour/items) are on the map tile?
 *  - getCreatures() - What creatures are currently positioned on the map tile?
 *  - isPresentlyPassable() - Is the map tile passable (can be moved onto), or is it impassable (like a wall, or building tile)?
 *  - getLastTickUpdated() - When was the last time (in game engine ticks) that we saw this tile?
 *  
 *  **Stale tiles**: A tile can be called "stale" if the agent has moved out of vision range of the tile, and since they can no longer see it,
 *  they are no longer receiving updates on what the contents of the tile is. For example, if the last time you saw a tile there was a monster on it,
 *  then the tile data will still contain the monster, even if you have long since left the area.
 *  
 *  Tile data in your agent's line of sight will never be stale, only tiles outside your vision range may be stale. To determine how stale a 
 *  tile is, call ``getLastTickUpdated(...)``. 
 *  
 **/
public final class Tile {
	
	private final List<IGroundObject> groundObjects = new ArrayList<>(0);
	
	private final List<ICreature> creatures = new ArrayList<>(0);

	/** Immutable list */
	private final List<ITerrain> terrain;

	private boolean isTileForRead = false;
	
	private final List<ITileProperty> tileProperties = new ArrayList<>();
	
	private Long lastTickUpdated;
	
	private boolean isPresentlyPassable = true;
	
	public Tile(boolean presentlyPassable, List<ITerrain> terrain) { 
		
		if(terrain == null) { throw new IllegalArgumentException("Terrain list must not be null."); };
		
		this.isPresentlyPassable = presentlyPassable;

		if(RCRuntime.CHECK) {
			this.terrain = Collections.unmodifiableList(terrain);
		} else {
			this.terrain = terrain;	
		}
		
		
	}
	
	public Tile(boolean presentlyPassable, ITerrain fgTerrain) {
		List<ITerrain> list = new ArrayList<>();
		
		if(fgTerrain != null) {
			list.add(fgTerrain);
		}

		this.isPresentlyPassable = presentlyPassable;

		if(RCRuntime.CHECK) {
			this.terrain = Collections.unmodifiableList(list);
		} else {
			this.terrain = list;			
		}
		

	}

	/** This contains the most recent game engine frame that a tile was seen.
	 * 
	 * (The current game engine frame may be retrieved from the client API, by calling getWorldState().getCurrentGameTick() on the WorldState object.) 
	 * 
	 * This method helps answer the question: 
	 * - When was the last time that we saw this tile? 
	 * - When was the last time our agent received an update on its contents? 
	 * - How stale is this tile?
	 * 
	 * See move about stale tiles in the class description. */
	public Long getLastTickUpdated() {
		return lastTickUpdated;
	}
	
	/** Is the tile passable? -- ie can we move on/off it? 
	 * This is false for tiles that are part of walls/buildings/fences/furniture, and true otherwise. */
	public boolean isPresentlyPassable() {
		return isPresentlyPassable;
	}
	
	/** Return a list of ground objects that are on the tile. */
	public List<IGroundObject> getGroundObjects() {
		if(RCRuntime.CHECK) {
			return Collections.unmodifiableList(groundObjects);
		} else {
			return groundObjects;
		}
	}
		
	/** Return a list of creatures that are on the tile. */
	public List<ICreature> getCreatures() {
		if(RCRuntime.CHECK) {
			return Collections.unmodifiableList(creatures);
		} else {
			return creatures;
		}
	}	
	
	@Override
	public String toString() {
		String result = "";
		
		for(IGroundObject go : groundObjects) {
			result += go+" ";
		}
		
		for(ICreature creature : creatures) {
			result += creature+" ";
		}
		
		result += (terrain != null ? terrain.get(0).getTileType().getNumber() : "");
		
		return result;
	}
	
	// Internal methods ------------------------------------------------------------------------------
	
	public TileType[] getTileTypeLayersForBrowserPresentation(int creatureArrayOffset) {
		
		List<TileType> result = new ArrayList<>();
		
//		TileType fg = null;
		
		if(creatures.size() > 0) {
			int index = (0+creatureArrayOffset) % creatures.size();
			
			ICreature creature = creatures.get( index  );
			if(creature != null) {
				TileType creatureFg = creature.getTileType();
				
				if(creature.isDead()) {
					creatureFg = new TileType(creatureFg.getNumber(), (creatureFg.getRotation()+270 % 360));
				}
				
				result.add(creatureFg);
//				fg = creatureFg; 
			}

		} else if(groundObjects.size() > 0) {
			TileType go = groundObjects.get( (int) (Math.random() * groundObjects.size() )).getTileType();
			result.add(go);
		} 
		
		terrain.stream().map(e -> e.getTileType()).forEach( e -> {  result.add(e);  });
		
		return result.toArray(new TileType[result.size()]);
		
//		else {
//			
//			if(terrain != null) {
//				fg = terrain.get(0).getTileType();
//			}
//		}
//		
//		if(bgTerrain != null) {
//			if(fg != null) {
//				// bg yes, fg yes
//				return new TileType[] { fg, bgTerrain.getTileType()};
//			} else {
//				// bg yes, fg no
//				return new TileType[] { bgTerrain.getTileType()};
//			}
//			
//		} else if(fg != null) {
//			// bg no, fg yes
//			return new TileType[] { fg};			
//		} else {
//			// bg no, fg no
//			return new TileType[] {};
//		}
		
	}
	
	public TileType[] getTileTypeLayers() { 
		return getTileTypeLayersForBrowserPresentation(0); // (int)(Math.random()*1024) );
	}
	
	@SuppressWarnings("unused")
	public List<ICreature> getCreaturesForModification() {
		if(RCRuntime.CHECK && isTileForRead) {
			throw new RuntimeException("Invalid thread write.");
		}
		return creatures;
	}
	
	@SuppressWarnings("unused")
	public List<IGroundObject> getGroundObjectForModification() {
		if(RCRuntime.CHECK && isTileForRead) {
			throw new RuntimeException("Invalid thread write.");
		}
		return groundObjects;
	}
	
	@SuppressWarnings("unused")
	public List<ITileProperty> getTilePropertiesForModification() {
		if(RCRuntime.CHECK && isTileForRead) {
			throw new RuntimeException("Invalid thread write.");
		}		return tileProperties;
	}
	
	public List<ITileProperty> getTileProperties() {
		if(RCRuntime.CHECK) {
			return Collections.unmodifiableList(tileProperties);
		} else {
			return tileProperties;
		}
	}

	public Tile shallowCloneUnchecked() {
		
		Tile t = new Tile(this.isPresentlyPassable, this.terrain);
		shallowCloneInternal(t);
		return t;
	}
	
	private void shallowCloneInternal(Tile t) {
		t.groundObjects.addAll(groundObjects);
		t.creatures.addAll(creatures);
		t.tileProperties.addAll(tileProperties);
	}

	
	public Tile shallowClone() {
		RCRuntime.assertGameThread();

		return shallowCloneUnchecked();
	}

	public List<ITerrain> internalGetTerrainListCopy() {
		List<ITerrain> result = new ArrayList<>();
		result.addAll(this.terrain);
		return result;
	}
	
	
	/** A shallow clone, but substitute the existing terrain list with the provided parameter */
	public Tile shallowCloneWithNewTerrainList(List<ITerrain> terrainParam) {
		List<ITerrain> newList = new ArrayList<>();
		newList.addAll(terrainParam);
		
		Tile t = new Tile(this.isPresentlyPassable, newList);
		shallowCloneInternal(t);
		return t;
		
	}

	
	public final boolean isTileForRead() {
		if(!RCRuntime.CHECK) { return false; }
		
		return isTileForRead;
	}
	
	public final void setTileForRead(boolean isTileForRead) {
		if(!RCRuntime.CHECK) { return; }
		
		this.isTileForRead = isTileForRead;
	}

	public void setLastTickUpdated(Long lastTickUpdated) {
		this.lastTickUpdated = lastTickUpdated;
	}
	
	

	// TODO: LOW - Create a player-only interface for tile, BUT only use it in the client code
	
	@SuppressWarnings("unused")
	private Tile fullClone() {
		RCRuntime.assertGameThread();
		
		Tile t = new Tile(this.isPresentlyPassable, this.terrain);
		
		t.lastTickUpdated = lastTickUpdated;
		
		groundObjects.stream().forEach( e -> {
			t.groundObjects.add (((GroundObject)e).shallowClone()  );
		});
		
		tileProperties.stream().forEach( e -> {
			t.tileProperties.add( ((ITilePropertyMutable)e).fullClone()  );
		});
		
		creatures.stream().forEach( e -> {
			t.creatures.add(  ((IMutableCreature)e).fullClone()  );
		});
		
		return t;
	}
}
