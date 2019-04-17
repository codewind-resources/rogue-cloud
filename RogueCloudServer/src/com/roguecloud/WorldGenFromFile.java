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

package com.roguecloud;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.roguecloud.WorldGenerationUtil.DrawRoomResult;
import com.roguecloud.map.IMap;
import com.roguecloud.map.IMutableMap;
import com.roguecloud.map.ITerrain;
import com.roguecloud.map.ImmutableImpassableTerrain;
import com.roguecloud.map.ImmutablePassableTerrain;
import com.roguecloud.map.RCArrayMap;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;
import com.roguecloud.map.TileTypeGroup;
import com.roguecloud.map.TileTypeList;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.RCUtils;
import com.roguecloud.utils.RoomList;
import com.roguecloud.utils.RoomList.Room;
import com.roguecloud.utils.ServerUtil.PerfData;
import com.roguecloud.utils.SimpleMap;
import com.roguecloud.utils.WorldGenFileMappings;
import com.roguecloud.utils.WorldGenFileMappings.WorldGenFileMappingEntry;

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
	
	public static WorldGenFromFileResult generateMapFromInputStream(RoomList roomList, InputStream mapContentsStream, WorldGenFileMappings mappings) throws IOException, InterruptedException {

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

		WorldGenFileMappingEntry roadMappingEntry = mappings.getByLetter("r");
		if(roadMappingEntry == null) { throw new RuntimeException("Could not find road tile in mappings."); }

		WorldGenFileMappingEntry waterEntry = mappings.getByLetter("w");
		if(waterEntry == null) { throw new RuntimeException("Could not find water tile in mappings."); }

		WorldGenFileMappingEntry forestEntry = mappings.getByLetter("o");
		if(waterEntry == null) { throw new RuntimeException("Could not find forest tile in mappings."); }

		
		// Convert the file contents to a map of Entry
		SimpleMap<Entry> eMap = new SimpleMap<>(charMap.getXSize(), charMap.getYSize());
		{
			for(int y = 0; y < charMap.getYSize(); y++) {
				for(int x = 0; x < charMap.getXSize(); x++) {
					
					Entry e = null;
					
					String ch = charMap.getTile(x, y);
					if(ch.equals("r")) {
						e = new Entry(roadMappingEntry);
					} else if(ch.equals("w")) {
						e = new Entry(waterEntry);
					
					} else if(mappings.getByLetter(ch) != null) {

						WorldGenFileMappingEntry t = mappings.getByLetter(ch);
						
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
		

		
		// Draw rooms, roads, grass.
		// - Create the result map from the entries 
		RCArrayMap aMap = new RCArrayMap(charMap.getXSize(), charMap.getYSize());
		List<RoomSpawn> spawns = new ArrayList<>();
		List<DrawRoomResult> drawRoomResults = new ArrayList<>();
		{
			ImmutablePassableTerrain road_30 = new ImmutablePassableTerrain(TileTypeList.ROAD_30);
			ImmutablePassableTerrain road_50 = new ImmutablePassableTerrain(TileTypeList.ROAD_50);
			ImmutablePassableTerrain road_90 = new ImmutablePassableTerrain(TileTypeList.ROAD_90);
			
			
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
					} else if(e.getMapping() == roadMappingEntry) {
						ImmutablePassableTerrain terrain;
						
						int val = rand.nextInt(10);
						if(val <= 1) {
							terrain = road_30;
						} else if(val <= 7) {
							terrain = road_50;
						} else {
							terrain = road_90;
						}

						t = new Tile(true, terrain);
					} else if(e.getMapping() == waterEntry || e.getMapping() == forestEntry) {
						/* ignore */
					} else {
						
						if(Math.random() < 0.60) {
							WorldGenFileMappingEntry entry = e.getMapping();
							
							List<String> roomNames = entry.getRoomNames();
							String roomName = roomNames.get( (int)(roomNames.size() * Math.random()) );
							
							Room r = roomList.getRoomByName(roomName);
							if(r == null) { throw new RuntimeException("Unable to find room: "+entry.getLetter()+" "+entry.getColour()+" "+entry.getRoomNames()); }
							drawRoomResult = WorldGenerationUtil.drawRoom(r, x, y, 0, aMap, false);							
						}
						
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
		}
		
		

		// Draw water tiles
		{
			for(int y = 0; y < charMap.getYSize(); y++) {
				for(int x = 0; x < charMap.getXSize(); x++) {
					Entry e = eMap.getTile(x, y);
					
					if(e != null && e.getMapping() == waterEntry) {
						
						boolean north = isValidWaterTile(x, y-1, eMap);
						boolean south= isValidWaterTile(x, y+1, eMap);
						boolean west = isValidWaterTile(x-1, y, eMap);
						boolean east = isValidWaterTile(x+1, y, eMap);

						TileType tileType = null;
						
						// NSWE
						// 0000 = none -> not supported
						// 0001 = east -> not supported
						// 0010 = west -> not supported
						// 0011 = east, west -> not supported
						// 0100 = north -> not supported
						// 0101 = south, east -> 1
						if(!north && south && !west && east ) { tileType = TileTypeList.WATER_1_SE; } // WATER_SE
						// 0110 = south, west -> 3
						if(!north && south && west && !east) { tileType = TileTypeList.WATER_3_SW; } // WATER_SW
						// 0111 = south, west, east -> 2
						if(!north && south && west && east) { tileType = TileTypeList.WATER_2_S; } // WATER_S
						// 1000 = north -> not supported
						// 1001 = north, east ->  7
						if(north && !south && !west && east) { tileType = TileTypeList.WATER_7_NE; } // WATER_NE
						// 1010 = north, west ->  9
						if(north && !south && west && !east) { tileType  = TileTypeList.WATER_9_NW; } // WATER_NW
						// 1011 = north, west, east -> 8
						if(north && !south && west && east) { tileType = TileTypeList.WATER_8_N; } // WATER_N
						// 1100 = noth, south -> not supported
						// 1101 = north, south, east -> 4
						if(north && south && !west && east) { tileType = TileTypeList.WATER_4_E; } // WATER_E
						// 1110 = north, south, west -> 6
						if(north && south && west && !east) { tileType = TileTypeList.WATER_6_W; } // WATER_W
						// 1111 = north, south, east, west -> 5
						if(north && south && east && west) {
							
							boolean ne = isValidWaterTile(x+1, y-1, eMap);
							boolean se = isValidWaterTile(x+1, y+1, eMap);
							boolean nw = isValidWaterTile(x-1, y-1, eMap);
							boolean sw = isValidWaterTile(x-1, y+1, eMap);
							
							int count = (ne ? 1 : 0) + (se ? 1 : 0) + (nw ? 1 : 0) + (sw ? 1 : 0);
							
							if(count == 3) {
								if(!ne) {
									tileType = TileTypeList.ISLAND_7_NE;
								} else if(!se) {
									tileType = TileTypeList.ISLAND_1_SE;
								} else if(!nw) {
									tileType = TileTypeList.ISLAND_9_NW;
								} else  { // sw
									tileType = TileTypeList.ISLAND_3_SW;
								}								
							} else {
								tileType = TileTypeList.WATER_5_ALL;	
							}
						}
						
						if(tileType != null) {
							Tile t = new Tile(false, new ImmutableImpassableTerrain(tileType));
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
			
			// For each of the rooms we previously drew, find the door tile inside the room (if applicable). 
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
			
			
			ImmutablePassableTerrain newRoadTerrain = new ImmutablePassableTerrain(TileTypeList.ROAD_50);
			
			// For each of the doors, go in a specific direction until we hit a road tile. 
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
		
		// Convert room floor tiles to Cobblestone
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
		
		PerfData perf = new PerfData();

		
		// Add tree tiles to random empty spots across the map
		{
			perf.reset();
			List<TileTypeGroup> forestGroups = new ArrayList<>();
			forestGroups.addAll(Arrays.asList( new TileTypeGroup[] {
					TileTypeList.WILLOW_TREE_GROUP,
					TileTypeList.PINE_TREE_GROUP,
					TileTypeList.DEAD_PINE_TREE_GROUP
			} ));
			
			Random r = new Random();
			for(int count = 0; count < 1000; count++) {
				
				TileTypeGroup ttGroup = (TileTypeGroup) returnRandomObject(new int[] { 50, 45, 5 }, forestGroups);
				
				int attempts = 0;
				boolean isValid = false;
				while(!isValid && attempts < 100) {
					attempts++;
					int xPos = r.nextInt(aMap.getXSize()-20)+10;
					int yPos = r.nextInt(aMap.getYSize()-20)+10;

					TileTypeGroup tree = ttGroup; 
					
					if(rectangleTilesContainOnlyGrass(xPos, yPos, tree, aMap)) {
						isValid = true;
						
						stampOntoMap(xPos, yPos, tree, aMap);
						
					}
										
				}
				
				if(count % 50 == 0) { assertNotInterrupted(); }
				
				// Sanity test to prevent game hang: Under no circumstances should 100 attempts in-a-row fail.
				if(attempts >= 100) { break; }
				
			}
			
			perf.output("Add tree tiles to random empty spots across the map");
		}

		List<DrawRoomResult> houses = new ArrayList<>();
		for(DrawRoomResult drr : drawRoomResults) {
			
			if(drr.getName() != null && drr.getName().contains("House")) {
				houses.add(drr);
			}			
		}

		
		// Draw table and chairs
		{
			perf.reset();
			
			if(houses.size() > 0) {

				Random r = new Random();

				// Tiles which are safe to stamp a table on top of (cobblestone and shadows)
				TileType[] validTileTypeOverlay;
				{
					List<TileType> validTileTypeOverlayList = new ArrayList<>();
					validTileTypeOverlayList.addAll(Arrays.asList(TileTypeList.ALL_COBBLESTONE));

					// Add wall shadow tiles
					// for(int x = 117; x <= 122; x++) { validTileTypeOverlayList.add(new TileType(x)); }
					// for(int x = 158; x <= 162; x++) { validTileTypeOverlayList.add(new TileType(x)); }
					
					validTileTypeOverlay = validTileTypeOverlayList.toArray(new TileType[validTileTypeOverlayList.size()]);
				}
				
			
				HashMap<DrawRoomResult, Boolean> containsTable = new HashMap<>();
				
				final int TOTAL_ROOM_ATTEMPTS = 200;
				final int MAX_STAMP_ATTEMPTS = 1000;
				
				// Look for a random spot that will fit both the tables and the chairs 
				for(int count = 0; count < TOTAL_ROOM_ATTEMPTS; count++) {
	
					DrawRoomResult drr = houses.get(r.nextInt(houses.size()));
					
					if(containsTable.containsKey(drr)) { continue; }
					else { containsTable.put(drr, Boolean.TRUE);  } 
					

					Position p = doSearch(drr, aMap, r, MAX_STAMP_ATTEMPTS, (int xPos, int yPos, IMap map) -> {
						
						return rectangleContainsOnlyTilesFromTileTypeList(xPos, yPos, validTileTypeOverlay, 
								TileTypeList.TABLE_AND_CHAIRS_GROUP, map);
					});
					
					
					if(p != null) {
						// We have found a place for the table, now swap in a random table sprite.
						TileType randomTable = TileTypeList.ALL_TABLES[ r.nextInt(TileTypeList.ALL_TABLES.length) ];
						
						TileTypeGroup newGroup = TileTypeList.TABLE_AND_CHAIRS_GROUP.cloneAndReplace(TileTypeList.WOOD_TABLE, randomTable);
						
						stampOntoMap(p.getX(), p.getY(), newGroup, aMap);
					}
					
					if(count % 20 == 0) { assertNotInterrupted(); }					
				}			
			}
			
			perf.output("Draw table and chairs");
		}
		
		// Draw torches on back walls of houses
		if(houses.size() > 0) {
			
			perf.reset();
			
			TileType[] validTorchSurfaces = new TileType[] { TileTypeList.GREY_BRICK_WALL, TileTypeList.BROWN_BRICK_WALL } ;
			
			Random r = new Random();
			
			final int TOTAL_ROOM_ATTEMPTS = 1000;
			
			HashMap<DrawRoomResult, List<Position>> roomTorches = new HashMap<>();
			
			// Draw torches 
			for(int count = 0; count < TOTAL_ROOM_ATTEMPTS; count++) {
				
				// Pick a random room
				DrawRoomResult drr = houses.get(r.nextInt(houses.size()));
				
				List<Position> previousTorches = roomTorches.computeIfAbsent(drr, k -> new ArrayList<Position>() );
				if(previousTorches.size() > 4) { continue; }
				
				// Look  for random spots to draw the torches
				Position p = doSearch(drr, aMap, r, 100, (int xPos, int yPos, IMap map) -> {
					
					if(!Position.isValid(xPos, yPos+1, map)) { return false; }

					Tile belowTile = map.getTile(xPos, yPos + 1);
					if(!AcontainsAtLeastOneB(belowTile.getTileTypeLayers(), TileTypeList.ALL_COBBLESTONE)) { return false; }

					if(AcontainsAtLeastOneB(belowTile.getTileTypeLayers(), TileTypeList.GRASS_TILES)) {  return false; }					
					
					if(shortestManhattanDistanceToPreviousPositions(xPos, yPos, previousTorches) < 5) {
						return false;
					}
					
					Tile posTile = map.getTile(xPos, yPos);
					
					// top tile must a valid torch surface
					return AcontainsAtLeastOneB(new TileType[] { posTile.getTileTypeLayers()[0] }, validTorchSurfaces );
					
				});
				
				if(p != null) {
					
					stampTileOntoMap(p.getX(), p.getY(), TileTypeList.TORCH, false, aMap);
					previousTorches.add(p);
				}
				
				if(count % 20 == 0) { assertNotInterrupted(); }
			} // end for
			
			roomTorches = null;
			
			// Draw furnaces on back walls of houses
			HashMap<DrawRoomResult, List<Position>>  roomFurnaces = new HashMap<>();
			
			for(int count = 0; count < TOTAL_ROOM_ATTEMPTS; count++) {
				
				// Pick a random house
				DrawRoomResult drr = houses.get(r.nextInt(houses.size()));
				
				List<Position> previousFurnaces = roomFurnaces.computeIfAbsent(drr, k -> new ArrayList<Position>() );
				if(previousFurnaces.size() > 0) { continue; }
				
				Position p = doSearch(drr, aMap, r, 100, (int xPos, int yPos, IMap map) -> {
					
					// Make sure the tile below the furnace (the shadow) is valid
					if(!Position.isValid(xPos, yPos+1, map)) { return false; }

					Tile belowTile = map.getTile(xPos, yPos + 1);
					if(!AcontainsAtLeastOneB(belowTile.getTileTypeLayers(), TileTypeList.ALL_COBBLESTONE)) { return false; }

					if(AcontainsAtLeastOneB(belowTile.getTileTypeLayers(), TileTypeList.GRASS_TILES)) {  return false; }					
					
					Tile posTile = map.getTile(xPos, yPos);
					
					// Top tile must a valid torch surface
					return AcontainsAtLeastOneB(new TileType[] { posTile.getTileTypeLayers()[0] }, validTorchSurfaces );
					
				});
				
				if(p != null) {
					
					stampOntoMap(p.getX(), p.getY(), TileTypeList.FURNACE_GROUP, aMap);
					
					previousFurnaces.add(p);
				}

				if(count % 20 == 0) { assertNotInterrupted(); }

			} // end furnaces for
			
			roomFurnaces = null;

			// Draw Candlesticks throughout the houses
			addCandleSticks(houses, aMap);
			
			perf.output("Draw torches and furnaces");
		}
		
		// Draw forests on map tiles marked with 'o'
		{
			perf.reset();
			SparseCoordinateUtil scu = new SparseCoordinateUtil(eMap, "o");
			Random rand = new Random();
			
			int addForestsRetryCount = (int)(scu.getTotalSize()/2500) * 13_000;
			
			TileTypeGroup[] ttg = new TileTypeGroup[] {
				TileTypeList.WILLOW_TREE_GROUP,
				TileTypeList.PINE_TREE_GROUP, 
				TileTypeList.OAK_TREE_GROUP,
				TileTypeList.DEAD_PINE_TREE_GROUP
			};
			
			int[] ttgDistribution = new int[] { 20, 40, 40, 10} ;
			
			HashMap<Position, TileTypeGroup> bestResultPosToGroupMap = null;
			int bestValue = 0;
			
			// Try X times and find the result that occupies the most space.
			for(int count = 0; count < 100; count++) {
								
				HashMap<Position, TileTypeGroup> resultPosToGroupMap = new HashMap<>();

				RCArrayMap tileMap = new RCArrayMap(aMap.getXSize(), aMap.getYSize()); // I tested here and RCArrayMap is significantly faster than RCCloneMap
				
				int totalOccupied = addForests(addForestsRetryCount, eMap, aMap, scu, resultPosToGroupMap, ttg, ttgDistribution, rand, tileMap);
								
				if(totalOccupied > bestValue) {
					bestValue = totalOccupied;
					bestResultPosToGroupMap = resultPosToGroupMap;
				}
				
				if(count % 20 == 0) { assertNotInterrupted(); }
			}
						
			bestResultPosToGroupMap.entrySet().forEach( e -> {
				Position p = e.getKey();
				TileTypeGroup newGroup = e.getValue();
				stampOntoMap(p.getX(), p.getY(), newGroup, aMap);
			});

			perf.output("Draw forests on map tiles marked with 'o'");
		}
		
		return new WorldGenFromFileResult(aMap, spawns);
	}
	
	/** Used to Draw forests on map tiles marked with 'o'. This function implements a single
	 * result of the draw algorithm, and returns the number of tiles that are occupied (which
	 * can be used to determine how good the result is). */
	private static int addForests(int numAttempts, SimpleMap<Entry> eMap, RCArrayMap aMap, SparseCoordinateUtil scu, 
			HashMap<Position, TileTypeGroup> resultPosToGroupMap,
			TileTypeGroup[] trees, int[] ttgDistribution, Random rand, IMutableMap tileMap) {
		
		List<TileTypeGroup> treesAsList = Arrays.asList(trees);
		
//		RCCloneMap tileMap = new RCCloneMap(aMap.getXSize(), aMap.getYSize()); 		
//		SimpleMap<Boolean> occupied = new SimpleMap<>(eMap.getXSize(), eMap.getYSize());
//		for(int x = 0; x < eMap.getXSize(); x++) { 
//			for(int y = 0; y < eMap.getYSize(); y++) {
//				occupied.putTile(x, y, false);
////				tileMap.putTile(x, y, new Tile(true, new ImmutablePassableTerrain(TileTypeList.GRASS_50)));
//			}
//		}
		
		int totalOccupied = 0;
	
		for(int x = 0; x < numAttempts; x++) {
			
			TileTypeGroup tree = (TileTypeGroup) returnRandomObject(ttgDistribution, treesAsList);
			
			TileType topRightTypeOfTree = tree.getTypeAt0Coord(tree.getWidth()-1, 0);
			TileType topLeftTypeOfTree = tree.getTypeAt0Coord(0, 0);

			Position p = scu.generateRandomCoordinate();
			
			if(addForests_ableToStamp(p.getX(), p.getY(), tree.getWidth(), tree.getHeight(), tileMap)) {
//			if(occupied.rectangleMatchesParam(p.getX(), p.getY(), tree.getWidth(), tree.getHeight(), Boolean.FALSE)) {
				
				// The coordinate to the left of the candidate position should not be the same sprite as this tree's top right sprite
				Position coordToTheLeft = new Position(p.getX()-1, p.getY());
				if(coordToTheLeft.isValid(tileMap)) {
					Tile t = tileMap.getTile(coordToTheLeft);
					if(t != null && t.getTileTypeLayers() != null && t.getTileTypeLayers().length > 0) {
						TileType top = t.getTileTypeLayers()[0];
						if(top.getNumber() == topRightTypeOfTree.getNumber() && rand.nextFloat() < 0.5) {
							continue;
						}
					}
				}

				
				// The coordinate to the right of the candidate position should not be the same sprite as this tree's top left sprite
				Position coordToTheRight = new Position(p.getX()+1, p.getY());
				if(coordToTheRight.isValid(tileMap)) {
					Tile t = tileMap.getTile(coordToTheRight);
					if(t != null && t.getTileTypeLayers() != null && t.getTileTypeLayers().length > 0) {
						TileType top = t.getTileTypeLayers()[0];
						if(top.getNumber() == topLeftTypeOfTree.getNumber() && rand.nextFloat() < 0.5) {
							continue;
						}
					}
				}
					
				
				// grid position is unoccupied, so use it
//				occupied.putParamToRectangle(p.getX(), p.getY(), tree.getWidth(), tree.getHeight(), Boolean.TRUE);
				totalOccupied += tree.getWidth() * tree.getHeight();
				resultPosToGroupMap.put(p, tree);
				stampOntoMap(p.getX(), p.getY(), tree, tileMap);
			}
			
		}
				
		return totalOccupied;

	}
	
	/** Check if we have been interrupted by our parent (likely for taking too long to generate) */
	private static void assertNotInterrupted() throws InterruptedException {
		if(Thread.interrupted()) { throw new InterruptedException(); } 
	}
	
	
	/** Return true if we able to draw a forest stamp on the given spot on the map, false otherwise. */
	private static boolean addForests_ableToStamp(int xParam, int yParam, int width, int height, IMutableMap map) {
		
		for(int x = xParam; x < xParam + width; x++) {
			for(int y = yParam; y < yParam + height; y++) {
				Tile t = map.getTile(x, y);
				if(t != null && t.getTileTypeLayers() != null && t.getTileTypeLayers().length > 0) {
					TileType top = t.getTileTypeLayers()[0];
					
					// If the sprite we are trying to stamp on is mostly not empty, then return false
					if(!AcontainsAtLeastOneB_single(TileTypeList.MOSTLY_EMPTY_TREE_SPRITES, top  )) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	

	/** Add random candlesticks in all the given homes. 
	 * @throws InterruptedException */
	private static void addCandleSticks(List<DrawRoomResult> houses, RCArrayMap aMap) throws InterruptedException {
		
		TileType[] validFloorSurfaces = TileTypeList.ALL_COBBLESTONE;
		
		Random r = new Random();
		
		final int TOTAL_ROOM_ATTEMPTS = 1000;
		
		HashMap<DrawRoomResult, List<Position>> roomTorches = new HashMap<>();
		
		// Draw torches 
		for(int count = 0; count < TOTAL_ROOM_ATTEMPTS; count++) {
			
			DrawRoomResult drr = houses.get(r.nextInt(houses.size()));
			
			List<Position> previousTorches = roomTorches.computeIfAbsent(drr, k -> new ArrayList<Position>() );
			if(previousTorches.size() > 4) { continue; }
			
			Position p = doSearch(drr, aMap, r, 100, (int xPos, int yPos, IMap map) -> {
				
				if(shortestManhattanDistanceToPreviousPositions(xPos, yPos, previousTorches) < 5) {
					return false;
				}
				
				Tile posTile = map.getTile(xPos, yPos);
				
				// top tile must a valid torch surface
				return AcontainsAtLeastOneB(new TileType[] { posTile.getTileTypeLayers()[0] }, validFloorSurfaces );
				
			});
			
			if(p != null) {
				
				stampTileOntoMap(p.getX(), p.getY(), TileTypeList.CANDLE_STICK, true, aMap);				
				previousTorches.add(p);
			}
			
			if(count % 20 == 0) { assertNotInterrupted(); }
		} // end for
		
		roomTorches = null;
		
	}
	
	private static int oldAddForests(int numAttempts, SimpleMap<Entry> eMap, RCArrayMap aMap, SparseCoordinateUtil scu, 
			HashMap<Position, TileTypeGroup> resultPosToGroupMap,
			TileTypeGroup[] trees, int[] ttgDistribution, Random rand, IMutableMap tileMap) {
		
		List<TileTypeGroup> treesAsList = Arrays.asList(trees);
		
//		RCCloneMap tileMap = new RCCloneMap(aMap.getXSize(), aMap.getYSize()); 
		
		
		SimpleMap<Boolean> occupied = new SimpleMap<>(eMap.getXSize(), eMap.getYSize());
		for(int x = 0; x < eMap.getXSize(); x++) { 
			for(int y = 0; y < eMap.getYSize(); y++) {
				occupied.putTile(x, y, false);
//				tileMap.putTile(x, y, new Tile(true, new ImmutablePassableTerrain(TileTypeList.GRASS_50)));
			}
		}
		
		int totalOccupied = 0;
	
		for(int x = 0; x < numAttempts; x++) {
			
			TileTypeGroup tree = (TileTypeGroup) returnRandomObject(ttgDistribution, treesAsList);
//					trees[rand.nextInt(trees.length)];
			
			
			TileType topRightTypeOfTree = tree.getTypeAt0Coord(tree.getWidth()-1, 0);
			TileType topLeftTypeOfTree = tree.getTypeAt0Coord(0, 0);

			Position p = scu.generateRandomCoordinate();
			
			if(occupied.rectangleMatchesParam(p.getX(), p.getY(), tree.getWidth(), tree.getHeight(), Boolean.FALSE)) {
				
				// The coordinate to the left of the candidate position should not be the same sprite as this tree's top right sprite
				Position coordToTheLeft = new Position(p.getX()-1, p.getY());
				if(coordToTheLeft.isValid(tileMap)) {
					Tile t = tileMap.getTile(coordToTheLeft);
					if(t != null && t.getTileTypeLayers() != null && t.getTileTypeLayers().length > 0) {
						TileType top = t.getTileTypeLayers()[0];
						if(top.getNumber() == topRightTypeOfTree.getNumber() && rand.nextFloat() < 0.5) {
							continue;
						}
					}
				}

				
				// The coordinate to the right of the candidate position should not be the same sprite as this tree's top left sprite
				Position coordToTheRight = new Position(p.getX()+1, p.getY());
				if(coordToTheRight.isValid(tileMap)) {
					Tile t = tileMap.getTile(coordToTheRight);
					if(t != null && t.getTileTypeLayers() != null && t.getTileTypeLayers().length > 0) {
						TileType top = t.getTileTypeLayers()[0];
						if(top.getNumber() == topLeftTypeOfTree.getNumber() && rand.nextFloat() < 0.5) {
							continue;
						}
					}
				}
					
				
				// grid position is unoccupied, so use it
				occupied.putParamToRectangle(p.getX(), p.getY(), tree.getWidth(), tree.getHeight(), Boolean.TRUE);
				totalOccupied += tree.getWidth() * tree.getHeight();
				resultPosToGroupMap.put(p, tree);
				stampOntoMap(p.getX(), p.getY(), tree, tileMap);
			}
			
		}
				
		return totalOccupied;

	}

	
	/** Utility class to count the number of tiles that, when moving left to right on a single row,
	 * have the same tile next to each other. */
	@SuppressWarnings("unused")
	private static int countMatchingHorizontalSprites(RCArrayMap tileMap) {
		int countSameSpriteInARow = 0;
		for(int y = 0; y < tileMap.getYSize(); y+=1) {

			Integer prevSprite = null;
			for(int x = 0; x < tileMap.getXSize(); x++ ) {
				Tile t = tileMap.getTile(x, y);
				if(t != null && t.getTileTypeLayers() != null && t.getTileTypeLayers().length > 0) {
					TileType top =  t.getTileTypeLayers()[0];
					
					if(prevSprite != null && top.getNumber() == prevSprite  )  { countSameSpriteInARow++; }
					
					prevSprite = top.getNumber();
				} else {
					prevSprite = null;
				}
				
			}
			
		}
		
		return countSameSpriteInARow;

	}
	
	/** Given an (x, y) coordinate, return the distance to the closest position in the given list. */
	private static int shortestManhattanDistanceToPreviousPositions(int x, int y, List<Position> previousPositions) {
		int shortestDistance = Integer.MAX_VALUE;
		
		for(Position p : previousPositions) {
			
			int distance = Position.manhattanDistanceBetween(x, y, p.getX(), p.getY());
			if(distance < shortestDistance) {
				shortestDistance = distance;
			}
		}
		
		return shortestDistance;
		
	}
	
	
	private static Position doSearch(DrawRoomResult drr, IMap aMap, Random r, int maxAttempts, IsValidPosition search) {
		return doSearch(drr.getX(), drr.getY(), drr.getWidth(), drr.getHeight(), aMap, r, maxAttempts, search);
	}
	
	private static Position doSearch(int startX, int startY, int width, int height, IMap aMap, Random r, int maxAttempts, IsValidPosition search) {
		SimpleMap<Boolean> tries = new SimpleMap<>(aMap.getXSize(), aMap.getYSize());
		
		int attempts = 0;
		boolean isValid = false;
		while(!isValid && attempts < maxAttempts) {
			attempts++;
			int xPos = r.nextInt(width)+startX;
			int yPos = r.nextInt(height)+startY;
			
			// Only try a specific position once.
			if(tries.getTile(xPos, yPos) != null) { continue; }
			tries.putTile(xPos, yPos, Boolean.TRUE);

			Tile t = aMap.getTile(xPos, yPos);
			if(!isEmptyTile(t) ) { continue; }
			
			if(search.isValid(xPos, yPos, aMap)) {
				return new Position(xPos, yPos);
			}
			
		}
		
		return null;
		

	}
	
	private static boolean isEmptyTile(Tile t) {
		if(t.getCreatures().size() > 0 ) { return false; }
		if(t.getGroundObjects().size() > 0 ) { return false; }
		return  true;
	}

	/** An implementation of this class is passed to doSearch(...). Implementers of this interface
	 * should return true if the given position on the map meets their needs (for example, 
	 * a tile group properly fits at the given position), or false otherwise. */
	private interface IsValidPosition {
		
		public boolean isValid(int xPos, int yPos, IMap map);
		
	}

	private static List<ITerrain> createPassableTerrainFromTileTypes(TileType[] list) {
		List<ITerrain>  result = new ArrayList<>();
		
		for(TileType tt : list) {
			result.add(new ImmutablePassableTerrain(tt));
		}
		
		return result;
	}
	
	private static List<ITerrain> createImpassableTerrainFromTileTypes(TileType[] list) {
		List<ITerrain>  result = new ArrayList<>();
		
		for(TileType tt : list) {
			result.add(new ImmutableImpassableTerrain(tt));
		}
		
		return result;
	}

	
	private static TileType[] addTileTypeToTopOfArray(TileType newTop, TileType[] existing) {
		TileType[] result = new TileType[existing.length+1];
		result[0] = newTop;
		System.arraycopy(existing, 0, result, 1, existing.length);
		
		return result;
	}
	
	
	
	/** Return an object from the given list, weighted using the given distribution. The distribution can be a percentage, 
	 * but doesn't need to be. */
	@SuppressWarnings("unchecked")
	private static Object returnRandomObject(int[] distribution, @SuppressWarnings("rawtypes") List objects) {
		List<Integer> distrib = new ArrayList<Integer>();
		for(int index = 0; index < distribution.length; index++) { distrib.add(distribution[index]); }
		
		return returnRandomObject(distrib, objects);
	}

	/** Return an object from the given list, weighted using the given distribution. The distribution can be a percentage, 
	 * but doesn't need to be. */
	private static Object returnRandomObject(List<Integer> distribution, List<Object> objects) {
		if(distribution.size() != objects.size())  { throw new RuntimeException("Object size mismatch"); }
		
		int total = 0;
		for(Integer i : distribution) {
			total += i;
		}
		
		int num = (int)(Math.random() * total);
		
		int curr = 0;
		
		for(int index = 0; index < distribution.size(); index++) {

			curr += distribution.get(index);
			
			if(num < curr) {
				return objects.get(index);
			}
			
		}
		
		return objects.get(objects.size()-1);
	}
	
	private static void stampOntoMap(int posX, int posY, TileTypeGroup group, IMutableMap map) {

		for(int deltaX = 0; deltaX < group.getWidth(); deltaX++) {
			
			for(int deltaY = 0; deltaY < group.getHeight(); deltaY++) {

				TileType typeToPutOnTop = group.getTypeAt0Coord(deltaX, deltaY);
				
				boolean passable = group.isPassableAtCoord(deltaX, deltaY);
				
				stampTileOntoMap(posX+deltaX, posY+deltaY, typeToPutOnTop, passable, map);
				
//				Tile oldTile = map.getTile(posX+deltaX, posY+deltaY);
//				
//				TileType typeToPutOnTop = group.getTypeAt0Coord(deltaX, deltaY);
//				
//				Tile newTile = new Tile(true,
//						createPassableTerrainFromTileTypes(
//								addTileTypeToTopOfArray(typeToPutOnTop, oldTile.getTileTypeLayers()))
//						);
//				
//				map.putTile(posX+deltaX, posY+deltaY, newTile);
				
			}
		}
	}
	
	private static void stampTileOntoMap(int posX, int posY, TileType typeToPutOnTop, boolean isTilePassable, IMutableMap map) {
		
		TileType[] oldTileTypeList;
		
		Tile oldTile = map.getTile(posX, posY);
		
		if(oldTile != null) {
			oldTileTypeList = oldTile.getTileTypeLayers();
		} else {
			oldTileTypeList = new TileType[] {};
		}
		
		List<ITerrain> terrain;
		if(isTilePassable) {
			terrain = createPassableTerrainFromTileTypes(
					addTileTypeToTopOfArray(typeToPutOnTop, oldTileTypeList ));
		} else {
			terrain = createImpassableTerrainFromTileTypes(
					addTileTypeToTopOfArray(typeToPutOnTop, oldTileTypeList ));			
		}
		
		
		Tile newTile = new Tile(isTilePassable, terrain );

		map.putTile(posX, posY, newTile);
	}
	
	private static boolean rectangleContainsOnlyTilesFromTileTypeList(int posX, int posY, TileType[] list, TileTypeGroup group, IMap map) {
		return rectangleContainsOnlyTilesFromTileTypeList(posX, posY, list, group.getWidth(), group.getHeight(), map);
	}
	
	private static boolean rectangleContainsOnlyTilesFromTileTypeList(int posX, int posY, TileType[] list, int width, int height, IMap map) {
		for(int deltaX = 0; deltaX < width; deltaX++) {
			
			for(int deltaY = 0; deltaY < height; deltaY++) {
	
				// Return false if out-of-bounds
				if(!Position.isValid(posX+deltaX, posY+deltaY, map)) { return false; }
				
				Tile t = map.getTile(posX+deltaX, posY+deltaY);
				
				if(t.getCreatures().size() > 0)  { return false; }
				if(t.getGroundObjects().size() > 0) { return false; }

				
				// 
//				if(!AcontainsAtLeastOneB(t.getTileTypeLayers(), list)) { return false; }
				
				// The tile may only contain tiles types from the list
				if(!AonlyContainsFromB(t.getTileTypeLayers(), list)) { return false; }
				
			}
		}
		
		return true;
	}
	
	private static boolean rectangleTilesContainOnlyGrass(int posX, int posY, TileTypeGroup group, IMap map) {
		return rectangleTilesContainOnlyGrass(posX, posY, group.getWidth(), group.getHeight(), map);
	}
	
	private static boolean rectangleTilesContainOnlyGrass(int posX, int posY, int width, int height, IMap map) {
		
		for(int deltaX = 0; deltaX < width; deltaX++) {
			
			for(int deltaY = 0; deltaY < height; deltaY++) {
				
				// Return false if out-of-bounds
				if(!Position.isValid(posX+deltaX, posY+deltaY, map)) { return false; }

				Tile t = map.getTile(posX+deltaX, posY+deltaY);
				
				if(!tileContainsOnlyGrass(t)) {
					return false;
				}
				
			}
			
		}
		
		return true;
		
	}
	
	/** Return true if a tile contains only grass (eg no creatures or objects and only grass tiles), or false otherwise. */
	private static boolean tileContainsOnlyGrass(Tile t) {
		if(t.getCreatures().size() > 0)  { return false; }
		if(t.getGroundObjects().size() > 0) { return false; }
		
		if(t.getTileTypeLayers() != null) {
			for(TileType tt : t.getTileTypeLayers()) {
			
				boolean match = false;
				for(TileType grassType : TileTypeList.GRASS_TILES) {
					if(grassType == tt) {
						match = true;
						break;
					}
				}
				
				if(!match) {
					return false;
				}
				
			}
		}
		
		return true;
	}
	
	
	private static boolean isValidWaterTile(int x, int y, SimpleMap<Entry> eMap) {
		if(x < 0 || y < 0) { return false; }
		if(x >= eMap.getXSize() || y >= eMap.getYSize()) { return false; }
		
		Entry e = eMap.getTile(x, y);
		if(e == null) { return false; }
		
		String letter = e.getMapping().getLetter();
		
		return letter != null && letter.equals("w");
		
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
			} else if(AcontainsAtLeastOneB(t.getTileTypeLayers(), TileTypeList.ROAD_TILES)) {
				// Valid tile: we have made it to a road by crossing only grass... success!
				break;
			} else {
				// Invalid tile: there is no valid path, so return fail
				return Collections.emptyList();
			}
			
			if(result.size() > 5) {
				// Path is too long, so return fail
				return Collections.emptyList();
			}
			
		} while(true);
		
		return result;
	}
	
	
	private static boolean AonlyContainsFromB(TileType[] A_tileTypes, TileType... B_list) {
		List<TileType> Alist_List = Arrays.asList(A_tileTypes);
		List<TileType> Blist_List = Arrays.asList(B_list);

		for(TileType curr : Alist_List) {
			
			if(!Blist_List.contains(curr)) {
				return false;
			}
			
		}
		
		return true;
		
	}
	
	private static boolean AcontainsAtLeastOneB_single(TileType[] A_tileTypes, TileType b) {
		for(TileType curr : A_tileTypes) {
			if(curr.getNumber() == b.getNumber()) {
				return true;
			}
		}
		return false;
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
	public static class Entry {
		
		private final WorldGenFileMappingEntry wfgm;
		
		public Entry(WorldGenFileMappingEntry wfgm) {
			this.wfgm = wfgm;
			
		}
		
		public WorldGenFileMappingEntry getMapping() {
			return wfgm;
		}
			
	}
	
}
