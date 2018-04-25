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

import java.util.ArrayList;
import java.util.List;

import com.roguecloud.map.DoorTerrain;
import com.roguecloud.map.DoorTileProperty;
import com.roguecloud.map.IMap;
import com.roguecloud.map.IMutableMap;
import com.roguecloud.map.ITerrain;
import com.roguecloud.map.ImmutableImpassableTerrain;
import com.roguecloud.map.ImmutablePassableTerrain;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;
import com.roguecloud.map.TileTypeList;
import com.roguecloud.map.WallTerrain;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.RoomList.Assignment;
import com.roguecloud.utils.RoomList.GridItem;
import com.roguecloud.utils.RoomList.GridItem.Type;
import com.roguecloud.utils.RoomList.Room;
import com.roguecloud.utils.RoomList.TilePair;

/** Various utility methods used for world generation. */
public class WorldGenerationUtil {
	
	private final static Logger log = Logger.getInstance();
	
	
	public static void drawBrickFence(IMutableMap m) {
		
		int X_SIZE = m.getXSize();
		int Y_SIZE = m.getYSize();
		
		drawBox(m, new Tile(false, new WallTerrain(TileTypeList.BRICK_FENCE_STRAIGHT_HORIZ_TOP)), 
				new Tile(false, new WallTerrain(TileTypeList.BRICK_FENCE_STRAIGHT_HORIZ_BOT)),
				new Tile(false, new WallTerrain(TileTypeList.BRICK_FENCE_STRAIGHT_VERT_LEFT)),
				new Tile(false, new WallTerrain(TileTypeList.BRICK_FENCE_STRAIGHT_VERT_RIGHT)), 
				0, 0, X_SIZE, Y_SIZE);
		
		m.putTile(0, 0, new Tile(false, new WallTerrain(TileTypeList.BRICK_FENCE_CORNER_TOP_LEFT)));
		m.putTile(X_SIZE-1, 0, new Tile(false, new WallTerrain(TileTypeList.BRICK_FENCE_CORNER_TOP_RIGHT)));
		m.putTile(0, Y_SIZE-1, new Tile(false, new WallTerrain(TileTypeList.BRICK_FENCE_CORNER_BOT_LEFT)));
		m.putTile(X_SIZE-1, Y_SIZE-1, new Tile(false, new WallTerrain(TileTypeList.BRICK_FENCE_CORNER_BOT_RIGHT)));
		
	}


	public static DrawRoomResult drawRoom(Room r, int destX, int destY, int rotationInDegrees, IMutableMap map, boolean validate) {
		DrawRoomResult result = new DrawRoomResult();
		result.setX(destX);
		result.setY(destY);
		result.setHeight(r.getHeight());
		result.setWidth(r.getWidth());
		
//		System.out.println("Drawing room: "+r.getName()+"  width: "+r.getWidth()+"  height: "+r.getHeight());
		
		if(validate) {
			// Validate that the room can be placed.
			for(int y = destY; y <= destX + r.getHeight(); y++) {
				for(int x = destX; x <= destX + r.getWidth(); x++) {
					
					Position p = new Position(x, y);
					if(!p.isValid(map)) {
						log.err("Invalid map room coord: "+p, null);
						result.setValid(false);
						return result;
					}
					
					if(!map.getTile(p).isPresentlyPassable()) {
						
						log.err("Unpassable position: "+p+" "+map.getTile(p), null);
						result.setValid(false);
						return result;
					}
				}
			}
		}

//		for(int y = destY; y < destY + r.getHeight(); y++) {
//			
//			String str = "";
//			for(int x = destX; x < destX + r.getWidth(); x++) {
//
//				int currX = x-destX;
//				int currY = y-destY;
//				
//				GridItem gi;
//				
//				gi = r.getGrid()[ currX ][ currY  ];
//				if(gi == null) { continue; }
//				
//				Assignment a = gi.getAssignment();
//
//				str += a.getLetter();
//			}
//			System.out.println(str);
//		}
		
		
		for(int y = destY; y < destY + r.getHeight(); y++) {
			for(int x = destX; x < destX + r.getWidth(); x++) {

				Position p = new Position(x, y);
				
				// Tile that is already at the given position (may be null)
				Tile currTile = map.getTile(p);
				
				// If the tile is not null, then get the background terrain
				ITerrain terrainCurrentlyOnTile = null; 
				if(currTile != null) {
					TileType[] ttArr = currTile.getTileTypeLayers();
					terrainCurrentlyOnTile = new ImmutablePassableTerrain(ttArr[ttArr.length-1]);
				}
				
				int currX = x - destX;
				int currY = y - destY;
				
				GridItem gi = null;
				if(rotationInDegrees == 0) {
					gi = r.getGrid()[currX][currY];
					
				} else if(rotationInDegrees == 90) {
					int newX = (currY);
					int newY = (currX);
					
					gi = r.getGrid()[ newX ][ newY  ];
					
				} else if(rotationInDegrees == 180) {
					int newX = r.getWidth() - (currX) - 1;
					int newY = r.getHeight() - (currY) - 1;
					gi = r.getGrid()[ newX ][ newY  ];
					
				} else if(rotationInDegrees == 270) {
					int newX = r.getWidth() - (currY) - 1;
					int newY = currX;
					
					gi = r.getGrid()[newX][newY];
					
				} else {
					log.severe("Unrecognized degree rotation: "+rotationInDegrees, null);
					result.setValid(false);
					return result;
				}
				
				if(gi == null) { continue; }
				
				if(gi.getType() == Type.ITEM_SPAWN) {
					result.getItemSpawns().add(p);
				} else if(gi.getType() == Type.MONSTER_SPAWN) {
					result.getMonsterSpawns().add(p);
				}
				
				Assignment a = gi.getAssignment();
				if(a != null) {
										
					Tile newTile = convertToTile(a, gi, terrainCurrentlyOnTile);
					
					map.putTile(p, newTile);
					
//					doThing(a, gi, currTileBgTerrain, map);
				}
			}
		}
		
		
		result.setValid(true);
		return result;
		
	}

	private static Tile convertToTile(Assignment a, GridItem gi, ITerrain currTileBgTerrain) {
		
		boolean impassableTerrain = false;
		
		if(a.getAnnotations().contains("Passable") || a.getAnnotations().contains("Door")) {
			impassableTerrain = false;
		} else {
			impassableTerrain = true;
		}

		List<ITerrain> terrainList = new ArrayList<>();
		
		for(TilePair tp : a.getTilePair()) {
			ITerrain terrain;
			if(tp.getTileNumber() == -1) {
				terrain = currTileBgTerrain;
			} else {
				terrain = convertTilePair(tp, impassableTerrain);
			}
			
			terrainList.add(terrain);
		}
		
		boolean hasBgAssignment = false;

		if(gi.getBgAssignment() != null) {

			hasBgAssignment = true;
			
			for(TilePair tp : gi.getBgAssignment().getTilePair()) {
				ITerrain terrain = convertTilePair(tp, impassableTerrain);
				terrainList.add(terrain);				
			}

		}

		if(currTileBgTerrain != null && !hasBgAssignment) {
			terrainList.add(currTileBgTerrain);
		}
		
		
		Tile newTile = new Tile(!impassableTerrain, terrainList);
		
		return newTile;
	}
	
	private static ITerrain convertTilePair(TilePair tp, boolean impassableTerrain) {
		ITerrain terrain;
		
		TileType tt = new TileType(tp.getTileNumber(), tp.getRotation());
		
		if(impassableTerrain) {
			terrain = new ImmutableImpassableTerrain(tt);
		} else {
			terrain = new ImmutablePassableTerrain(tt);
		}
		
		return terrain;
		
	}
	
//	private static void doThing(Assignment a, GridItem gi, ITerrain currTileBgTerrain, IMutableMap map) {
//		ITerrain bgTerrain = null;
//		{
//			// If the assignment has a background tile, then use it
//			if(a.getTilePair().length > 1) {
//				TilePair bgTilePair = a.getTilePair()[1];
////				bgTerrain = new ImmutablePassableTerrain(new TileType(bgAssignment.getTileNumber(), bgAssignment.getRotation()));
//				if(bgTilePair.getTileNumber() == -1) {
//					if(currTileBgTerrain != null) {
//						bgTerrain = currTileBgTerrain;
//					} else {
//						// TOOD: SEVERE - log me.
//					}
//				} else {
//					bgTerrain = new ImmutablePassableTerrain(new TileType(bgTilePair.getTileNumber(), bgTilePair.getRotation() ));	
//				}
//				
//			} else {
//				// Otherwise the assignment has no background tile, so use the default background.
//				Assignment bgAssignment = gi.getBgAssignment();
//				if(bgAssignment != null) {
//					TilePair bgTilePair = bgAssignment.getTilePair()[0];
//					if(bgTilePair.getTileNumber() == -1) {
//						bgTerrain = currTileBgTerrain;
//					} else {
//						bgTerrain = new ImmutablePassableTerrain(new TileType(bgTilePair.getTileNumber(), bgTilePair.getRotation() ));	
//					}
//					
//				}
//			}
//			
//		}
//		
//		Tile newTile;
//		TileType fgTileType = null;
//		{
//			TilePair fgTilePair = a.getTilePair()[0];
//			if(fgTilePair.getTileNumber() == -1) {
//				if(currTileBgTerrain != null) {
//					// Set both the bg and fg to -1
//					fgTileType = currTileBgTerrain.getTileType();
//					bgTerrain = new ImmutablePassableTerrain(fgTileType);
//				} else {
//					log.severe("Background tile pair is null: " + a.getLetter()+" "+a.getName(), null);
//				}
//				
//			} else {
//				fgTileType = new TileType(fgTilePair.getTileNumber(), fgTilePair.getRotation());
//			}
//		}
//		
//		if(fgTileType != null) {
//			
//			if(a.getAnnotations().contains("Door")) {
//				newTile = new Tile(true, new DoorTerrain(fgTileType, true), bgTerrain);
//				newTile.getTilePropertiesForModification().add(new DoorTileProperty(false));
//				
//			} else if(a.getAnnotations().contains("Passable")) {
//				newTile = new Tile(true, new ImmutablePassableTerrain(fgTileType), bgTerrain);
//				
//			} else {
//				newTile = new Tile(false, new ImmutableImpassableTerrain(fgTileType), bgTerrain);
//			}
//									
//			map.putTile(p, newTile);
//			
//		}
//
//	}
//	

	public static void drawBox(IMutableMap m, Tile topHorizTile, Tile botHorizTile, 
			Tile topVertTile, Tile botVertTile, int startX, int startY, int width, int height) {
		
		// top and bottom
		for(int x = startX; x < startX+width; x++) {
			m.putTile(x, startY, topHorizTile);
			m.putTile(x, startY+height-1, botHorizTile);
		}
		
		// left and right
		for(int y = startY; y < startY+height; y++) {
			m.putTile(startX, y, topVertTile);
			m.putTile(startX+width-1, y, botVertTile);
		}
		
	}

	
	
	public static Position findValidOpenPosition(IMap map) {
		
		while(true) {
			
			int x = (int)(Math.random() * map.getXSize());
			int y = (int)(Math.random() * map.getYSize());
			
			Position p = new Position(x , y);
			if(!p.isValid(map)) { continue; }
			
			Tile t = map.getTile(p);
			if(t.isPresentlyPassable()) {
				return p;
			}
			
		}
		
	}

	public static void drawFill(IMutableMap m, ITerrain terrain, int startx, int starty, int w, int h) {
		
		for(int x = startx; x < startx+w; x++) {
			for(int y = starty; y < starty+h; y++) {
				m.putTile(x, y, new Tile(false, terrain));
			}
		}
		
	}
	
	@SuppressWarnings("unused")
	public static void drawGreyBox(IMutableMap m, int x, int y, int w, int h) {
		
		int X_SIZE = w;
		int Y_SIZE = h;
		
		
		
		drawBox(m, new Tile(false, new WallTerrain(TileTypeList.ROAD_WALL_STRAIGHT_HORIZ_TOP)), 
				new Tile(false, new WallTerrain(TileTypeList.ROAD_WALL_STRAIGHT_HORIZ_BOT)),
				new Tile(false, new WallTerrain(TileTypeList.ROAD_WALL_STRAIGHT_VERT_LEFT)),
				new Tile(false, new WallTerrain(TileTypeList.ROAD_WALL_STRAIGHT_VERT_RIGHT)), 
				x, y, X_SIZE, Y_SIZE);
		
		m.putTile(x, y, new Tile(false, new WallTerrain(TileTypeList.ROAD_WALL_CORNER_TOP_LEFT)));
		m.putTile(x+X_SIZE-1, y+h, new Tile(false, new WallTerrain(TileTypeList.ROAD_WALL_CORNER_TOP_RIGHT)));
		m.putTile(x, y+Y_SIZE-1, new Tile(false, new WallTerrain(TileTypeList.ROAD_WALL_CORNER_BOT_LEFT)));
		m.putTile(x+X_SIZE-1, y+Y_SIZE-1, new Tile(false, new WallTerrain(TileTypeList.ROAD_WALL_CORNER_BOT_RIGHT)));
		
		
//			m.putTile(0, 0, t);

	}

	
	@SuppressWarnings("unused")
	public static void drawWoodBox(IMutableMap m) {
		
		int X_SIZE = m.getXSize();
		int Y_SIZE = m.getYSize();
		
		Tile horizTile = new Tile(false, new WallTerrain(TileTypeList.WOOD_WALL_STRAIGHT_HORIZ));
		
		Tile vertTile = new Tile(false, new WallTerrain(TileTypeList.WOOD_WALL_STRAIGHT_VERT));
		
		
		drawBox(m, horizTile, horizTile, vertTile, vertTile, 0, 0, X_SIZE, Y_SIZE);
		
		m.putTile(0, 0, new Tile(false, new WallTerrain(TileTypeList.WOOD_WALL_CORNER_TOP_LEFT)));
		m.putTile(X_SIZE-1, 0, new Tile(false, new WallTerrain(TileTypeList.WOOD_WALL_CORNER_TOP_RIGHT)));
		m.putTile(0, Y_SIZE-1, new Tile(false, new WallTerrain(TileTypeList.WOOD_WALL_CORNER_BOT_LEFT)));
		m.putTile(X_SIZE-1, Y_SIZE-1, new Tile(false, new WallTerrain(TileTypeList.WOOD_WALL_CORNER_BOT_RIGHT)));
		
	}

	
	/** Return value of drawRoom(...) method */
	public static class DrawRoomResult {
		
		/** Coordinate of any monster spawn tiles that were written to the map */
		private final List<Position> monsterSpawns = new ArrayList<>();
		
		/** Coordinate of any item spawn tiles that were written to the map */
		private final List<Position> itemSpawns = new ArrayList<>();
		
		/** Whether or not the draw succeeded; depends on if validation is enable, and if there were any conflicts */
		boolean isValid = false;
		
		int x;
		int y;
		
		int width;
		int height;
		
		public DrawRoomResult() {
		}

		public boolean isValid() {
			return isValid;
		}

		public void setValid(boolean isValid) {
			this.isValid = isValid;
		}

		public List<Position> getMonsterSpawns() {
			return monsterSpawns;
		}

		public List<Position> getItemSpawns() {
			return itemSpawns;
		}

		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}

		public int getY() {
			return y;
		}

		public void setY(int y) {
			this.y = y;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		
	}
}
