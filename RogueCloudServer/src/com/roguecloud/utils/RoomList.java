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

package com.roguecloud.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.roguecloud.resources.Resources;
import com.roguecloud.utils.RoomList.GridItem.Type;

public class RoomList {
	
	private static final Logger log = Logger.getInstance();
	
	private final List<Room> rooms;
	
	public RoomList(LogContext lc) throws IOException {
		
		this(UniverseParserUtil.class.getClassLoader().getResourceAsStream("/universe/rooms.txt"), lc);
		
	}
	
//	public static void main(String[] args) {
//		try {
//			new RoomList(new FileInputStream(new File("c:\\delme\\rooms.txt")), null);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	public RoomList(InputStream inputStream, LogContext lc) throws IOException {
		
		String[] lines = RCUtils.readIntoString(inputStream).split("\\r?\\n");
		
		String roomName = null;
		
		HashMap<InnerMapCoord, GridItem> grid = new HashMap<>();
		
		List<Room> roomResult = new ArrayList<Room>();
		
		this.rooms = roomResult;
		
		HashMap<String, Assignment> currAssignments = new HashMap<>();
		
		boolean parsingGrid = false;
		
		int currGridRow = -1;
		
		for(String line : lines) {
			
			line = line.trim();
			
			try {
			
				if(line.endsWith(":")) {
					roomName = line.substring(0, line.indexOf(":"));
				}
				
				if(roomName == null) { continue; }
				
				if(line.equals("{")) {
					parsingGrid = true;
					
				} else if(parsingGrid) {
	
					if(line.endsWith("}")) {
						
						int maxX = 0;
						int maxY = 0;
						for(InnerMapCoord imc : grid.keySet()) {
							if(imc.x > maxX) {
								maxX = imc.x;
							}					
							if(imc.y > maxY) {
								maxY = imc.y;
							}
						}
						
						final GridItem[][] gridResult = new GridItem[maxX+1][maxY+1];
						grid.entrySet().stream().forEach( e -> {
							gridResult[e.getKey().x][e.getKey().y] = e.getValue();
						});
						
						ArrayList<Assignment> assignmentList = new ArrayList<>();
						assignmentList.addAll(currAssignments.values());
						
						Room r = new Room(roomName, gridResult, assignmentList, maxX+1, maxY+1);
						roomResult.add(r);

						grid.clear();
						parsingGrid = false;
						roomName = null;
						currAssignments.clear();
						currGridRow = -1;
						
					} else {

						// Parse grid
						
						currGridRow++;
						
						final Assignment bgAssignment = currAssignments.get("*");
						
						for(int x = 0; x < line.length(); x++) {
							String letter = line.substring(x, x+1);
							
							Assignment currBgAssignment = bgAssignment;
							
							// @ = monster spawn
							// ! = item spawn
							Assignment a = null;
							if(!letter.equals(" ") && !letter.equals("!") && !letter.equals("@")) { 
								a = currAssignments.get(letter);
								if(a == null) {
									log.err("WARNING: Assignment '"+letter+"' not found on line "+line, lc);
								}
							} else {
								a = currAssignments.get("*");
							}
							
							if(a.getAnnotations().contains("Bg")) {
								currBgAssignment = a;
							}
							
							Type gridItemType = GridItem.Type.NONE;
							
							if(letter.equals("@")) {
								gridItemType = GridItem.Type.MONSTER_SPAWN;
							} else if(letter.equals("!")) {
								gridItemType = GridItem.Type.ITEM_SPAWN;
							}
							
							GridItem gi = new GridItem(a, currBgAssignment, gridItemType);

							grid.put(new InnerMapCoord(x, currGridRow), gi);
						}
						
					}
					
					
				} else if(line.contains("=")) {
					
					parseLineNew(line, currAssignments, lc);
										
				}
			
			} catch(Throwable t) {
				log.severe("Unable to parse line: "+line, lc);
				throw t;
			}
			
			
		}		
		
	}
	
//	private static void parseLine(String line, HashMap<String, Assignment> currAssignments, LogContext lc) {
//		String[] splitArr = line.split("\\s+");
//		
//		String letter = splitArr[0];
//		
//		String roomNumStr = splitArr[2];
//		boolean containsRotation = false;
//		if(roomNumStr.endsWith(",")) {
//			containsRotation = true;
//			roomNumStr = roomNumStr.substring(0, roomNumStr.length()-1);
//		}
//		int roomNum = Integer.parseInt(roomNumStr);
//		
//		int remainingTextTokenIndex = 0;
//		
//		int rotationNum = 0;
//		if(containsRotation) {
//			rotationNum = Integer.parseInt(splitArr[3]);
//			remainingTextTokenIndex = 4;
//		} else {
//			remainingTextTokenIndex = 3;
//		}
//		
//		
//		List<String> annotations = new ArrayList<>();
//		
//		String name = "";
//		for(int x = remainingTextTokenIndex; x < splitArr.length; x++) {
//			String currToken = splitArr[x];
//			if(currToken.startsWith("@")) {
//				annotations.add(currToken.substring(1));
//			
//			} else {
//				name += currToken+" ";	
//			}
//		}
//		
//		name = name.trim();
//		
//		Assignment a = new Assignment(letter, roomNum, rotationNum, name, annotations );
//		if(currAssignments.containsKey(letter)) {
//			log.err("WARNING: Assignment parsing error: A letter is repeated on line - "+line, lc);
//		}
//		currAssignments.put(letter, a);
//
//	}
	
	private static void parseLineNew(String line, HashMap<String, Assignment> currAssignments, LogContext lc) {
		String[] splitArr = line.split("\\s+");
				
		String letter = splitArr[0];
		
		int roomNum1 = -9;
		int rotationNum1 = 0;
		
		int roomNum2 = -9;
		int rotationNum2 = 0;
		
		int remainingTextTokenIndex = 0;

		if(line.contains("/") ) {
			
//			int equalsPos = Arrays.asList(splitArr).indexOf("=");
//			int slashIndex = Arrays.asList(splitArr).indexOf("/");
			
			int currTok = 2;
			
			String roomNumStr = splitArr[currTok++];
			boolean containsRotation = false;
			if(roomNumStr.endsWith(",")) {
				containsRotation = true;
				roomNumStr = roomNumStr.substring(0, roomNumStr.length()-1); // Remove the comma
			}
			roomNum1 = Integer.parseInt(roomNumStr);
			
			
			rotationNum1 = 0;
			if(containsRotation) {
				rotationNum1 = Integer.parseInt(splitArr[currTok++]);
//				remainingTextTokenIndex = 4;
			} else {
//				remainingTextTokenIndex = 3;
			}

			currTok++; // Skip the slash
			
			roomNumStr = splitArr[currTok++];
			containsRotation = false;
			if(roomNumStr.endsWith(",")) {
				containsRotation = true;
				roomNumStr = roomNumStr.substring(0, roomNumStr.length()-1); // Remove the comma
			}
			roomNum2 = Integer.parseInt(roomNumStr);
			
			
			rotationNum2 = 0;
			if(containsRotation) {
				rotationNum2 = Integer.parseInt(splitArr[currTok++]);
//				remainingTextTokenIndex = 4;
			} else {
//				remainingTextTokenIndex = 3;
			}

			remainingTextTokenIndex = currTok;
			
//			if(slashIndex == -1 || equalsPos == -1) {
//				return;
//			}
			
			
		} else {
			String roomNumStr = splitArr[2];
			boolean containsRotation = false;
			if(roomNumStr.endsWith(",")) {
				containsRotation = true;
				roomNumStr = roomNumStr.substring(0, roomNumStr.length()-1);
			}
			roomNum1 = Integer.parseInt(roomNumStr);
			
			rotationNum1 = 0;
			if(containsRotation) {
				rotationNum1 = Integer.parseInt(splitArr[3]);
				remainingTextTokenIndex = 4;
			} else {
				remainingTextTokenIndex = 3;
			}			
		}
		
		// Q = 1093, 0 / 1962 	
		
		List<String> annotations = new ArrayList<>();
		
		String name = "";
		for(int x = remainingTextTokenIndex; x < splitArr.length; x++) {
			String currToken = splitArr[x];
			if(currToken.startsWith("@")) {
				annotations.add(currToken.substring(1));
			
			} else {
				name += currToken+" ";	
			}
		}
		
		name = name.trim();
		
		TilePair[] tilePairs = null;
		if(roomNum1 != -9 && roomNum2 != -9) {
			tilePairs = new TilePair[] { new TilePair(roomNum1, rotationNum1), new TilePair(roomNum2, rotationNum2)  };
		} else if(roomNum1 != -9) {
			tilePairs = new TilePair[] { new TilePair(roomNum1, rotationNum1) };
		}
		
		if(tilePairs == null) {
			log.severe("tilePairs is null", null);
			return;
		}
		
		Assignment a = new Assignment(letter, tilePairs, name, annotations );
		if(currAssignments.containsKey(letter)) {
			log.err("WARNING: Assignment parsing error: A letter is repeated on line - "+line, lc);
		}
		currAssignments.put(letter, a);

	}

	public Room getRoomByName(String name) {
		return rooms.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	public List<Room> getRooms() {
		return rooms;
	}
	
	public static class Room {
		
		private final String name;
		
		private final GridItem[ /*width*/ ][ /*height*/] grid;
		
		private final List<Assignment> assignments;

		private final int width;
		
		private final int height;
		
		public Room(String name, GridItem[][] grid, List<Assignment> assignments, int width, int height) {
			this.name = name;
			this.grid = grid;
			this.assignments = Collections.unmodifiableList(assignments);
			this.width = width;
			this.height = height;
		}

		public String getName() {
			return name;
		}

		public GridItem[][] getGrid() {
			return grid;
		}

		public List<Assignment> getAssignments() {
			return assignments;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}
		
		
		@Override
		public String toString() {
			String CR = System.lineSeparator();
			
			String str = "Name: "+name+" ("+width+" x "+height+")"+CR+CR;
			
			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {
					
					GridItem gi = grid[x][y];
					if(gi.getAssignment() == null) {
						str += " ";
					} else {
						str += gi.getAssignment().getLetter();
					}
					
				}
				str += CR;
			}

			return str;
		}
		
	}
	
	
	public static class GridItem {
		private final Assignment a;
		
		private final Assignment bg;
		
		public static enum Type { NONE, MONSTER_SPAWN, ITEM_SPAWN };
		
		private final Type type;
		
		public GridItem(Assignment a, Assignment bg, Type t) {
			this.a  = a;
			this.bg = bg;
			this.type = t;
		}
		
		public Assignment getAssignment() {
			return a;
		}
		
		public Assignment getBgAssignment() {
			return bg;
		}
		
		public Type getType() {
			return type;
		}
	}
	
	
	
	public static class Assignment {
		final String letter;
		
//		final int tileNumber;
//		final int rotation;

		final String name;
		
		final List<String> annotations;
		
		final TilePair[] tilePair;
		
		public Assignment(String letter, TilePair[] tilePair, String name, List<String> annotations) {
			if(tilePair == null || tilePair.length == 0) { throw new IllegalArgumentException("Invalid tilePair value"); }
			
			for(TilePair tp : tilePair) {
				if(tp.getTileNumber() != -1 && !Resources.getInstance().isValidTile(tp.getTileNumber())) {
					throw new IllegalArgumentException("Invalid tile pair value: " + tp.getTileNumber());
				}
			}
			
			this.letter = letter;
//			this.tileNumber = tileNumber;
//			this.rotation = rotation;
			this.tilePair = tilePair;
			
			this.name = name;
			this.annotations = annotations;
		}
		
		public TilePair[] getTilePair() {
			return tilePair;
		}

		public String getLetter() {
			return letter;
		}

//		public int getTileNumber() {
//			return tileNumber;
//		}
//
//		public int getRotation() {
//			return rotation;
//		}

		public String getName() {
			return name;
		}

		public List<String> getAnnotations() {
			return annotations;
		}
		
	}
	
	public static class TilePair {
		final int tileNumber;
		final int rotation;

		
		public TilePair(int tileNumber) {
			this.tileNumber = tileNumber;
			this.rotation = 0;
		}

		public TilePair(int tileNumber, int rotation) {
			this.tileNumber = tileNumber;
			this.rotation = rotation;
		}

		public int getTileNumber() {
			return tileNumber;
		}

		public int getRotation() {
			return rotation;
		}
		
	}
	
	private static class InnerMapCoord {
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
