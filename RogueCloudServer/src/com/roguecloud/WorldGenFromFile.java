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

package com.roguecloud;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.roguecloud.WorldGenerationUtil.DrawRoomResult;
import com.roguecloud.map.IMap;
import com.roguecloud.map.ITerrain;
import com.roguecloud.map.ImmutableImpassableTerrain;
import com.roguecloud.map.ImmutablePassableTerrain;
import com.roguecloud.map.RCArrayMap;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;
import com.roguecloud.map.TileTypeList;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.RCUtils;
import com.roguecloud.utils.RoomList;
import com.roguecloud.utils.RoomList.Room;
import com.roguecloud.utils.SimpleMap;

/** 
 * Generates a new map object (world) using the contents of a world text file. The world text file is generated
 * from a .png file, using the PngMain utility. 
 * 
 * In the world text file, each character of each line correspond to a tile in the world; which character corresponds to which
 * room is defined in the WorldGenFromFile.Entry class.
 * 
 * The top left coordinate of the world gen map is (0,0), the bottom right coordinate is (width, height).
 **/
public class WorldGenFromFile {
	
	private static final Logger log = Logger.getInstance();
	
	public static WorldGenFromFileResult generateMapFromInputStream(RoomList roomList, InputStream mapContentsStream) throws IOException {

		List<String> fileContents = RCUtils.readIntoStringListAndClose(mapContentsStream);
		
		// Parse the file contents into a simple 2d map
		SimpleMap<String> charMap;
		{
			int width = 0, height = 0;
			int lineNumber = 1;
			for(String line : fileContents) {
				
				if(width == 0) {
					width = line.length();
				} else if(width != line.length()) {
					throw new RuntimeException("Inconsistent width at line "+lineNumber);
				}
				height++;
				lineNumber++;
			}
			
			charMap = new SimpleMap<>(width, height);
			
			int y = 0;
			for(String line : fileContents) {
				for(int x = 0; x < width; x++) {
					
					String ch = line.substring(x,  x+1);
					charMap.putTile(x, y, ch);
				}
				
				y++;
			}
		}
		

		Map<String /*char*/, Entry.Type> mapping = new HashMap<>();
		
		Arrays.asList(Entry.Type.values()).forEach( e -> {
			mapping.put(e.letter, e);
		});
		
		// Convert the file contents to a map of Entry
		SimpleMap<Entry> eMap = new SimpleMap<>(charMap.getXSize(), charMap.getYSize());
		{
			for(int y = 0; y < charMap.getYSize(); y++) {
				for(int x = 0; x < charMap.getXSize(); x++) {
					
					Entry e = null;
					
					String ch = charMap.getTile(x, y);
					if(ch.equals("r")) {
						e = new Entry(Entry.Type.ROAD);
					} else if(ch.equals("w")) {
						e = new Entry(Entry.Type.WATER);
					
					} else if(mapping.keySet().contains(ch)) {
						Entry.Type t = mapping.get(ch);
						
						String east = charMap.getTile(x+1, y);
						String south = charMap.getTile(x, y+1);
						
						if(east != null && south != null && east.equals(ch) && south.equals(ch)) {
							e = new Entry(t);
						}
					} else if(ch.equals(".")) {
						/* ignore */
					} else {
						log.severe("Map contains unrecognized character: "+ch+" at "+x+" "+y, null);
					}
					
					eMap.putTile(x, y, e);
					
				}
			}
		}
		

		// Create the result map from the entries 
		RCArrayMap aMap = new RCArrayMap(charMap.getXSize(), charMap.getYSize());
		List<RoomSpawn> spawns = new ArrayList<>();
		List<DrawRoomResult> drawRoomResults = new ArrayList<>();
		{
			ImmutablePassableTerrain road = new ImmutablePassableTerrain(TileTypeList.ROAD);
			
			ImmutablePassableTerrain grass_100 = new ImmutablePassableTerrain(TileTypeList.GRASS_100);
			ImmutablePassableTerrain grass_75 = new ImmutablePassableTerrain(TileTypeList.GRASS_75);
			ImmutablePassableTerrain grass_50 = new ImmutablePassableTerrain(TileTypeList.GRASS_50);

			Random rand = new Random();
			
			for(int y = 0; y < charMap.getYSize(); y++) {
				for(int x = 0; x < charMap.getXSize(); x++) {
					
					ImmutablePassableTerrain terrain;
					
					int val = rand.nextInt(40);
					if(val <= 0) {
						terrain = grass_100;
					} else if(val <= 8) {
						terrain = grass_50;
					} else {
						terrain = grass_75;
					}
					
					Tile t = new Tile(true, terrain);
					aMap.putTile(x, y, t);
				}
			}

			for(int y = 0; y < charMap.getYSize(); y++) {
				for(int x = 0; x < charMap.getXSize(); x++) {
					
					Tile t = null;
					
					DrawRoomResult drawRoomResult = null;
					Entry e = eMap.getTile(x, y);
					if(e == null) {
						
						if(aMap.getTile(x, y) == null) {
							t = new Tile(true, grass_75);	
						}
					} else if(e.type == Entry.Type.ROAD) {
						t = new Tile(true, road);
					} else if(e.type == Entry.Type.WATER) {
						/* ignore */
					} else {
						Room r = roomList.getRoomByName(e.type.name);
						drawRoomResult = WorldGenerationUtil.drawRoom(r, x, y, e.type.rotation, aMap, false);
					}

					if(t != null) {
						aMap.putTile(x, y, t);
					}

					if(drawRoomResult  != null) {
						RoomSpawn rs = new RoomSpawn();
						rs.getItemSpawnsInRoom().addAll(drawRoomResult.getItemSpawns());
						rs.getMonsterSpawnsInRoom().addAll(drawRoomResult.getMonsterSpawns());
						spawns.add(rs);
						drawRoomResults.add(drawRoomResult);
					}
				}
			} // end for
			
//			for(int y = 0; y < charMap.getYSize()-40; y += 50) {
//				for(int x = 0; x < charMap.getXSize()- 40; x+= 50) { 
//					WorldGenerationUtil.drawRoom(roomList.getRoomByName("Library"), x, y, 0, aMap, false);
//
//				}
//			}
			
		}
		
		

		// Draw water tiles
		{
			for(int y = 0; y < charMap.getYSize(); y++) {
				for(int x = 0; x < charMap.getXSize(); x++) {
					Entry e = eMap.getTile(x, y);
					
					if(e != null && e.type == Entry.Type.WATER) {
						
						boolean north = isValidWaterTile(x, y-1, eMap);
						boolean south= isValidWaterTile(x, y+1, eMap);
						boolean west = isValidWaterTile(x-1, y, eMap);
						boolean east = isValidWaterTile(x+1, y, eMap);

						Integer tileNum = null;
						
						// NSWE
						// 0000 = none -> not supported
						// 0001 = east -> not supported
						// 0010 = west -> not supported
						// 0011 = east, west -> not supported
						// 0100 = north -> not supported
						// 0101 = south, east -> 1
						if(!north && south && !west && east ) { tileNum = 230; }
						// 0110 = south, west -> 3
						if(!north && south && west && !east) { tileNum = 232; }
						// 0111 = south, west, east -> 2
						if(!north && south && west && east) { tileNum = 231; }
						// 1000 = north -> not supported
						// 1001 = north, east ->  7
						if(north && !south && !west && east) { tileNum = 236; }
						// 1010 = north, west ->  9
						if(north && !south && west && !east) { tileNum  = 238; }
						// 1011 = north, west, east -> 8
						if(north && !south && west && east) { tileNum = 237; }
						// 1100 = noth, south -> not supported
						// 1101 = north, south, east -> 4
						if(north && south && !west && east) { tileNum = 233; }
						// 1110 = north, south, west -> 6
						if(north && south && west && !east) { tileNum = 235; }
						// 1111 = north, south, east, west -> 5
						if(north && south && east && west) {
							
							boolean ne = isValidWaterTile(x+1, y-1, eMap);
							boolean se = isValidWaterTile(x+1, y+1, eMap);
							boolean nw = isValidWaterTile(x-1, y-1, eMap);
							boolean sw = isValidWaterTile(x-1, y+1, eMap);
							
							int count = (ne ? 1 : 0) + (se ? 1 : 0) + (nw ? 1 : 0) + (sw ? 1 : 0);
							
							if(count == 3) {
								if(!ne) {
									tileNum = 246;
								} else if(!se) {
									tileNum = 240;
								} else if(!nw) {
									tileNum = 248;
								} else  { // sw
									tileNum = 242;
								}								
							} else {
								tileNum = 234;	
							}
						}
						
						if(tileNum != null) {
							// TODO: EASY - Add this to tile list
							Tile t = new Tile(false, new ImmutableImpassableTerrain(new TileType(tileNum, 0)));
							aMap.putTile(x, y, t);
						}
						
					}
					
				}
			}
			
		}
		
		
		// Draw road tiles from the door of a house, to the nearest road, if possible.
		{
			final int[][] DIRECTIONS = new int[/* index*/][/*x delta, y delta*/] {
					{1, 0},
					{0, 1},
					{-1, 0},
					{0, -1}
			};
			
			List<Position> doors = new ArrayList<>();
			
			for(DrawRoomResult drr : drawRoomResults) {
				
				for(int x = drr.getX(); x < drr.getX()+drr.getWidth(); x++) {
					
					for(int y = drr.getY(); y < drr.getY()+drr.getHeight(); y++) {
						
						Tile tile = aMap.getTile(x, y);
						boolean isDoorTile = Arrays.asList(tile.getTileTypeLayers()).stream().anyMatch(  e -> Arrays.asList(TileTypeList.DOOR_TILES ).contains(e));
						if(isDoorTile) {
							doors.add(new Position(x, y));
						}
					}
				}
			}
			
			
			ImmutablePassableTerrain newRoadTerrain = new ImmutablePassableTerrain(TileTypeList.ROAD);
			
			for(Position door : doors) {
				
				inner: for(int[] DIRECTION : DIRECTIONS) {
					List<Position> path = canWeGetToARoad(door.getX(), door.getY(), DIRECTION[0], DIRECTION[1], aMap);
					
					if(path.size() > 0) {
						path.forEach( e -> {
							Tile t = new Tile(true, newRoadTerrain);
							aMap.putTile(e, t);						
						});
						break inner;
					} 
				}
				
			}
						
		}
		
		// Convert room floor tile to Cobblestone
		{
			Random r = new Random();
			List<ImmutablePassableTerrain> cobblestoneTiles = new ArrayList<>();
			{
				for(TileType tt : TileTypeList.ALL_COBBLESTONE) {
					cobblestoneTiles.add(new ImmutablePassableTerrain(tt));
				}
				
			}
			
			for(DrawRoomResult drr : drawRoomResults) {
				for(int x = drr.getX(); x < drr.getX()+drr.getWidth(); x++) {
					
					for(int y = drr.getY(); y < drr.getY()+drr.getHeight(); y++) {
						
						Tile existingTile = aMap.getTile(x, y);
						List<ITerrain> terrainListCopy = existingTile.internalGetTerrainListCopy();
						
						boolean match = false;
						for(int index = 0; index < terrainListCopy.size(); index++)  {
							ITerrain curr = terrainListCopy.get(index);
							if(curr.getTileType().getNumber() == 300) {
								terrainListCopy.set(index, cobblestoneTiles.get(r.nextInt(cobblestoneTiles.size())));
								match = true;
							}
							
						}
						
						if(match) {
							Tile newTile = existingTile.shallowCloneWithNewTerrainList(terrainListCopy);
							aMap.putTile(x,  y, newTile);
						}
						
					}
					
				}
				
			}
		}

		return new WorldGenFromFileResult(aMap, spawns);
	}
	
	private static boolean isValidWaterTile(int x, int y, SimpleMap<Entry> eMap) {
		if(x < 0 || y < 0) { return false; }
		if(x >= eMap.getXSize() || y >= eMap.getYSize()) { return false; }
		
		Entry e = eMap.getTile(x, y);
		if(e == null) { return false; }
		return e.type == Entry.Type.WATER;
		
	}
	
	
	/** Whether or not we can get to a road from this position, by only crossing grass.  
	 * Returns a non-empty list if a path is available, otherwise an empty list is returned.*/
	private static List<Position> canWeGetToARoad(int initialX, int initialY, int xDelta, int yDelta, IMap aMap)  {
		
		if(Math.abs(xDelta) > 1 || Math.abs(yDelta) > 1) { throw new IllegalArgumentException("xDelta and yDelta must be 0, 1, or -1");  }
		
		List<Position> result = new ArrayList<>();
		
		int x = initialX, y = initialY;
		
		do {
			x += xDelta;
			y += yDelta;

			Position currPos = new Position(x, y);
			if(!currPos.isValid(aMap)) { return Collections.emptyList(); }
			
			Tile t = aMap.getTile(currPos);
			result.add(currPos);
			
			if(!t.isPresentlyPassable()) {
				// Invalid tile: Any tile that we replace must be already passable.
				return Collections.emptyList();
			}
			
			if(t.getCreatures().size() > 0 || t.getGroundObjects().size() > 0) {
				// Invalid tile: There shouldn't be any creatures or objects on the tile at this stage in the generation process, but
				// if there are then the tile is invalid.
				return Collections.emptyList();
			}
			
			if(t.getTileProperties().size() > 0) {
				// Invalid tile: When we are creating new road tiles in the calling method, we are not cloning tile properties,
				// therefore we can't allow any tile properties in any of the path tiles at this stage.
				return Collections.emptyList();
			}
			
			if(AcontainsAtLeastOneB(t.getTileTypeLayers(), TileTypeList.GRASS_TILES)) {
				// Valid tile: grass
			} else if(AcontainsAtLeastOneB(t.getTileTypeLayers(), TileTypeList.ROAD)) {
				// Valid tile: we have made it to a road by crossing only grass... success!
				break;
			} else {
				// Invalid tile: there is no valid path, so return fail
				return Collections.emptyList();
			}
			
			if(result.size() > 12) {
				// Path is too long, so return fail
				return Collections.emptyList();
			}
			
		} while(true);
		
		return result;
	}
	
	private static boolean AcontainsAtLeastOneB(TileType[] A_tileTypes, TileType... B_list) {
		List<TileType> tileTypesList = Arrays.asList(A_tileTypes);
		
		for(TileType curr : B_list) {
			
			if(tileTypesList.contains(curr)) {
				return true;
			}
			
		}
		
		return false;
	}
	
	
	/** 
	 * Return value of generateMapFromInputStream(...): a newly generated map, and a list of
	 * all the room spawns for all the rooms.
	 */
	public static class WorldGenFromFileResult {
		final RCArrayMap map;

		final List<RoomSpawn> spawns;
		
		public WorldGenFromFileResult(RCArrayMap map, List<RoomSpawn> spawns) {
			this.map = map;
			this.spawns = spawns;
		}

		public RCArrayMap getMap() {
			return map;
		}

		public List<RoomSpawn> getSpawns() {
			return spawns;
		}
				
	}
	
	/** A room can contains specific tiles that are designated as spots that monsters
	 * or items can spawn. This class contains a list of those for a specific room. */
	public static class RoomSpawn {
		private final List<Position> itemSpawnsInRoom = new ArrayList<>();
		private final List<Position> monsterSpawnsInRoom = new ArrayList<>();
		
		public RoomSpawn() {
		}

		public List<Position> getItemSpawnsInRoom() {
			return itemSpawnsInRoom;
		}

		public List<Position> getMonsterSpawnsInRoom() {
			return monsterSpawnsInRoom;
		}
		
	}
	
	/** Each alphanumeric character in the world file corresponds to a specific type of room (or other structure). */
	private static class Entry {
		
		private static final String SMALL_HOUSE_NW = "New House NW";
		private static final String SMALL_HOUSE_SE = "New House SE";

		/** Each alphanumeric character in the world file corresponds to a specific type of room (or other structure). */
		public static enum Type { 
			ROAD("r", null), 
			SMALL_HOUSE_SOUTH_DOOR("a", SMALL_HOUSE_SE, 0), 
			SMALL_HOUSE_WEST_DOOR("b", SMALL_HOUSE_NW, 0), 
			SMALL_HOUSE_EAST_DOOR("c", SMALL_HOUSE_SE), 
			SMALL_HOUSE_NORTH_DOOR("d", SMALL_HOUSE_NW, 0), 
			BASKETBALL_COURT("e", "Basketball Court"), 
			LIBRARY("f", "Library"),
			GRAVEYARD("g", "Graveyard"),
			GAS_STATION("h", "Gas Station"),
			WATER("w", null)
			;
			
			final String letter;
			final String name;
			final int rotation;
			
			Type(String str, String name) {
				this.letter = str;
				this.name = name;
				this.rotation = 0;
			}
			
			Type(String str, String name, int rotation) {
				this.letter = str;
				this.name = name;
				this.rotation = rotation;
			}

		}
		
		final Type type;
		
		public Entry(Type type) {
			this.type = type;
		}
		
		@Override
		public String toString() {
			return type.letter;
		}
	}
	
}
