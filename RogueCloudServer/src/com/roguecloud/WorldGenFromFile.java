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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.roguecloud.WorldGenerationUtil.DrawRoomResult;
import com.roguecloud.map.ImmutablePassableTerrain;
import com.roguecloud.map.RCArrayMap;
import com.roguecloud.map.Tile;
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
					}
					
					else if(mapping.keySet().contains(ch)) {
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
		{
			
			ImmutablePassableTerrain road = new ImmutablePassableTerrain(TileTypeList.ROAD);
			ImmutablePassableTerrain grass = new ImmutablePassableTerrain(TileTypeList.GRASS);

			for(int y = 0; y < charMap.getYSize(); y++) {
				for(int x = 0; x < charMap.getXSize(); x++) {
					Tile t = new Tile(true, null, grass);
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
							t = new Tile(true, null, grass);	
						}						
					} else if(e.type == Entry.Type.ROAD) {
						t = new Tile(true, null, road);
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
					}
				}
			} // end for
			
			
			
//			for(int y = 0; y < charMap.getYSize()-40; y += 50) {
//				for(int x = 0; x < charMap.getXSize()- 40; x+= 50) { 
//					WorldGenerationUtil.drawRoom(roomList.getRoomByName("Gas Station"), x, y, 0, aMap, false);
//
//				}
//			}

			
		}

		return new WorldGenFromFileResult(aMap, spawns);
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
		
		private static final String SMALL_HOUSE = "Small House";

		/** Each alphanumeric character in the world file corresponds to a specific type of room (or other structure). */
		public static enum Type { 
			ROAD("r", null), 
			SMALL_HOUSE_SOUTH_DOOR("a", SMALL_HOUSE, 90), 
			SMALL_HOUSE_WEST_DOOR("b", SMALL_HOUSE, 180), 
			SMALL_HOUSE_EAST_DOOR("c", SMALL_HOUSE), 
			SMALL_HOUSE_NORTH_DOOR("d", SMALL_HOUSE, 270), 
			BASKETBALL_COURT("e", "Basketball Court"), 
			LIBRARY("f", "Library"),
			GRAVEYARD("g", "Graveyard"),
			GAS_STATION("h", "Gas Station")
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
