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

	public static final TileType GRASS_100 = new TileType(200, 0);
	public static final TileType GRASS_75 = new TileType(201, 0);
	public static final TileType GRASS_50 = new TileType(202, 0);
//	public static final TileType GRASS = new TileType(1853, 0);
	
	public static final TileType[] GRASS_TILES = { GRASS_100, GRASS_75, GRASS_50 }; 
	
	public static final TileType WOOD_WALL_CORNER_TOP_LEFT = new TileType(1136, 0);
	public static final TileType WOOD_WALL_CORNER_TOP_RIGHT = new TileType(1136, 90);
	public static final TileType WOOD_WALL_CORNER_BOT_LEFT = new TileType(1136, 270);
	public static final TileType WOOD_WALL_CORNER_BOT_RIGHT = new TileType(1136, 180);
	
	public static final TileType WOOD_WALL_STRAIGHT_VERT = new TileType(1137, 0);
	public static final TileType WOOD_WALL_STRAIGHT_HORIZ = new TileType(1137, 90);
	

	public static final TileType DOOR_CLOSED_HORIZ = new TileType(107, 0);
	public static final TileType DOOR_CLOSED_VERT_RIGHT = new TileType(108, 0);
	public static final TileType DOOR_CLOSED_VERT_LEFT = new TileType(106, 0);
	
	public static final TileType[] DOOR_TILES = new TileType[] { DOOR_CLOSED_HORIZ, DOOR_CLOSED_VERT_LEFT, DOOR_CLOSED_VERT_RIGHT };
	
	
//	public static final TileType ROAD = new TileType(660, 0);
	public static final TileType ROAD = new TileType(301, 0);
	
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
	
	public static final TileType WATER_1_SE = new TileType(230);
	public static final TileType WATER_2_S = new TileType(231);
	public static final TileType WATER_3_SW = new TileType(232);
	public static final TileType WATER_4_E = new TileType(233);
	public static final TileType WATER_5_ALL = new TileType(234);
	public static final TileType WATER_6_W = new TileType(235);
	public static final TileType WATER_7_NE = new TileType(236);
	public static final TileType WATER_8_N = new TileType(237);
	public static final TileType WATER_9_NW = new TileType(238);
	
	public static final TileType ISLAND_1_SE = new TileType(240);
	public static final TileType ISLAND_2 = new TileType(241);
	public static final TileType ISLAND_3_SW = new TileType(242);
	public static final TileType ISLAND_4 = new TileType(243);
	public static final TileType ISLAND_5 = new TileType(244);
	public static final TileType ISLAND_6 = new TileType(245);
	public static final TileType ISLAND_7_NE = new TileType(246);
	public static final TileType ISLAND_8 = new TileType(247);
	public static final TileType ISLAND_9_NW = new TileType(248);
	
	public static final TileType COBBLESTONE_310 = new TileType(310);
	public static final TileType COBBLESTONE_311 = new TileType(311);
	public static final TileType COBBLESTONE_312 = new TileType(312);
	public static final TileType COBBLESTONE_313 = new TileType(313);
	public static final TileType COBBLESTONE_314 = new TileType(314);
	public static final TileType COBBLESTONE_315 = new TileType(315);
	public static final TileType COBBLESTONE_316 = new TileType(316);
	public static final TileType COBBLESTONE_317 = new TileType(317);
	public static final TileType COBBLESTONE_318 = new TileType(318);
	public static final TileType COBBLESTONE_319 = new TileType(319);

	public static final TileType[] ALL_COBBLESTONE = new TileType[] {
//			COBBLESTONE_310, 
//			COBBLESTONE_311, 
			COBBLESTONE_312, 
//			COBBLESTONE_313, 
//			COBBLESTONE_314,
			COBBLESTONE_315, 
//			COBBLESTONE_316, 
//			COBBLESTONE_317, 
//			COBBLESTONE_318, 
//			COBBLESTONE_319
	};
	
	
	public static void init() {
		// This method is a no-op, but is required (as it causes the static code to run), so don't remove it.
	}
}
