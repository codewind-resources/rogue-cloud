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

import com.roguecloud.map.TileType;

/** Constants which correspond to existing tile images in the tile list. */
public class TileTypeList {

	public static final TileType GRASS = new TileType(667, 0);
//	public static final TileType GRASS = new TileType(1853, 0);
	
	public static final TileType WOOD_WALL_CORNER_TOP_LEFT = new TileType(1136, 0);
	public static final TileType WOOD_WALL_CORNER_TOP_RIGHT = new TileType(1136, 90);
	public static final TileType WOOD_WALL_CORNER_BOT_LEFT = new TileType(1136, 270);
	public static final TileType WOOD_WALL_CORNER_BOT_RIGHT = new TileType(1136, 180);
	
	public static final TileType WOOD_WALL_STRAIGHT_VERT = new TileType(1137, 0);
	public static final TileType WOOD_WALL_STRAIGHT_HORIZ = new TileType(1137, 90);
	

	public static final TileType ROAD = new TileType(660, 0);
	
	public static final TileType ROAD_WALL_CORNER_TOP_LEFT = new TileType(1039, 0);
	public static final TileType ROAD_WALL_CORNER_TOP_RIGHT = new TileType(1039, 90);
	public static final TileType ROAD_WALL_CORNER_BOT_RIGHT = new TileType(1039, 180);
	public static final TileType ROAD_WALL_CORNER_BOT_LEFT = new TileType(1039, 270);
	
	public static final TileType ROAD_WALL_STRAIGHT_VERT_LEFT = new TileType(1042, 270);
	public static final TileType ROAD_WALL_STRAIGHT_VERT_RIGHT = new TileType(1042, 90);
	public static final TileType ROAD_WALL_STRAIGHT_HORIZ_TOP = new TileType(1042, 0);
	public static final TileType ROAD_WALL_STRAIGHT_HORIZ_BOT = new TileType(1042, 180);
	
	public static final TileType ROAD_WALL_INNER = new TileType(1038, 0);

	public static final TileType BRICK_FENCE_CORNER_TOP_LEFT = new TileType(2115, 0);
	public static final TileType BRICK_FENCE_CORNER_TOP_RIGHT = new TileType(2115, 90);
	public static final TileType BRICK_FENCE_CORNER_BOT_RIGHT = new TileType(2115, 180);
	public static final TileType BRICK_FENCE_CORNER_BOT_LEFT = new TileType(2115, 270);
	
	public static final TileType BRICK_FENCE_STRAIGHT_VERT_LEFT = new TileType(2116, 0);
	public static final TileType BRICK_FENCE_STRAIGHT_VERT_RIGHT = new TileType(2116, 0);
	public static final TileType BRICK_FENCE_STRAIGHT_HORIZ_TOP = new TileType(2118, 0);
	public static final TileType BRICK_FENCE_STRAIGHT_HORIZ_BOT = new TileType(2118, 0);

	public static final TileType CYBER_KNIGHT = new TileType(1733);
	
	public static final TileType LARGE_FLAG = new TileType(2155);
	
	public static final TileType LARGE_BOTTLE = new TileType(1008);
	public static final TileType SMALL_BOTTLE = new TileType(1009, 0, "Vial");
	public static final TileType TINY_BOTTLE = new TileType(1007);
	
	public static final TileType DIRT_PATH = new TileType(2134);
	
	public static final TileType NULL = new TileType(645);

	
	public static final TileType SHRUB_1 = new TileType(2183, 0, "Shrub");
	public static final TileType SHRUB_2 = new TileType(2189, 0, "Shrub");
	public static final TileType SHRUB_3 = new TileType(2193, 0, "Shrub");
	public static final TileType SHRUB_4 = new TileType(2195, 0, "Shrub");
	public static final TileType SHRUB_5 = new TileType(2197, 0, "Shrub");
	public static final TileType SHRUB_6 = new TileType(2175, 0, "Shrub");
	
	
	public static void init() {
		// no-op, but is requires, so don't remove.
	}
}
