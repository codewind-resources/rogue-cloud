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
	

	public static final TileType GREY_BRICK_DOOR_CLOSED_HORIZ = new TileType(107, 0);
	public static final TileType GREY_BRICK_DOOR_CLOSED_VERT_RIGHT = new TileType(108, 0);
	public static final TileType GREY_BRICK_DOOR_DOOR_CLOSED_VERT_LEFT = new TileType(106, 0);
	public static final TileType GREY_BRICK_WALL = new TileType(104, 0);
	
	public static final TileType BROWN_BRICK_DOOR_CLOSED_HORIZ = new TileType(147, 0);
	public static final TileType BROWN_BRICK_DOOR_CLOSED_VERT_RIGHT = new TileType(148, 0);
	public static final TileType BROWN_BRICK_DOOR_DOOR_CLOSED_VERT_LEFT = new TileType(146, 0);
	public static final TileType BROWN_BRICK_WALL = new TileType(144, 0);
	
	
	public static final TileType[] DOOR_TILES = new TileType[] { GREY_BRICK_DOOR_CLOSED_HORIZ, GREY_BRICK_DOOR_DOOR_CLOSED_VERT_LEFT, 
			GREY_BRICK_DOOR_CLOSED_VERT_RIGHT, BROWN_BRICK_DOOR_CLOSED_HORIZ, BROWN_BRICK_DOOR_CLOSED_VERT_RIGHT, 
			BROWN_BRICK_DOOR_DOOR_CLOSED_VERT_LEFT };
	
	
//	public static final TileType ROAD = new TileType(660, 0);
	public static final TileType ROAD_50 = new TileType(301, 0);
	public static final TileType ROAD_90 = new TileType(302, 0);
	public static final TileType ROAD_30 = new TileType(303, 0);
	public static final TileType[] ROAD_TILES = new TileType[] { ROAD_30, ROAD_50, ROAD_90 };
	
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
	
	public static final TileType KNIGHT_CYAN = new TileType(1810);
	public static final TileType KNIGHT_GREEN = new TileType(1811);
	public static final TileType KNIGHT_NATURAL = new TileType(1812);
	public static final TileType KNIGHT_PURPLE = new TileType(1813);
	public static final TileType KNIGHT_RED = new TileType(1814);
	public static final TileType KNIGHT_YELLOW = new TileType(1815);
	public static final TileType NINJA_CYAN = new TileType(1816);
	public static final TileType NINJA_GREEN = new TileType(1817);
	public static final TileType NINJA_NATURAL = new TileType(1818);
	public static final TileType NINJA_PURPLE = new TileType(1819);
	public static final TileType NINJA_RED = new TileType(1820);
	public static final TileType NINJA_YELLOW = new TileType(1821);
	public static final TileType ROGUE_CYAN = new TileType(1822);
	public static final TileType ROGUE_GREEN = new TileType(1823);
	public static final TileType ROGUE_NATURAL = new TileType(1824);
	public static final TileType ROGUE_PURPLE = new TileType(1825);
	public static final TileType ROGUE_RED = new TileType(1826);
	public static final TileType ROGUE_YELLOW = new TileType(1827);
	
	public static final TileType SHADOW = new TileType(10);

	public static TileType[] CHARACTER_TILES = new TileType[] {
		KNIGHT_CYAN,
		KNIGHT_GREEN,
		KNIGHT_NATURAL,
		KNIGHT_PURPLE,
		KNIGHT_RED,
		KNIGHT_YELLOW,
		NINJA_CYAN,
		NINJA_GREEN,
		NINJA_NATURAL,
		NINJA_PURPLE,
		NINJA_RED,
		NINJA_YELLOW,
		ROGUE_CYAN,
		ROGUE_GREEN,
		ROGUE_NATURAL,
		ROGUE_PURPLE,
		ROGUE_RED,
		ROGUE_YELLOW
	};
		
	public static final TileType LARGE_FLAG = new TileType(2155);
	
	public static final TileType LARGE_BOTTLE = new TileType(1008);
	public static final TileType SMALL_BOTTLE = new TileType(1009, 0, "Vial");
	public static final TileType TINY_BOTTLE = new TileType(1007);
	
	public static final TileType DIRT_PATH = new TileType(2134);
	
	public static final TileType NULL = new TileType(645);

	
	public static final TileType SHRUB_1 = new TileType(2183, 0, "Shrub");
	public static final TileType SHRUB_2 = new TileType(2189, 0, "Shrub");
//	public static final TileType SHRUB_3 = new TileType(2193, 0, "Shrub");
	public static final TileType SHRUB_4 = new TileType(2195, 0, "Shrub");
	public static final TileType SHRUB_5 = new TileType(2197, 0, "Shrub");
//	public static final TileType SHRUB_6 = new TileType(2175, 0, "Shrub");
	public static final TileType SHRUB_7 = new TileType(2192, 0, "Shrub");
	
	public static final TileType[] SHRUB_TILES = new TileType[] {
			SHRUB_1,
			SHRUB_2,
//			SHRUB_3,
			SHRUB_4,
			SHRUB_5,
//			SHRUB_6,
			SHRUB_7
	};
	
	
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

	public static final TileType WILLOW_TREE_TOP = new TileType(400);
	public static final TileType WILLOW_TREE_BOT = new TileType(401);
	
	public static final TileType PINE_TREE_11 = new TileType(402);
	public static final TileType PINE_TREE_21 = new TileType(403);
	public static final TileType PINE_TREE_12 = new TileType(404);
	public static final TileType PINE_TREE_22 = new TileType(405);
	public static final TileType PINE_TREE_13 = new TileType(406);
	public static final TileType PINE_TREE_23 = new TileType(407);
	public static final TileType PINE_TREE_14 = new TileType(408);
	public static final TileType PINE_TREE_24 = new TileType(409);
	
	public static final TileType DEAD_PINE_TREE_11 = new TileType(410);
	public static final TileType DEAD_PINE_TREE_21 = new TileType(411);
	public static final TileType DEAD_PINE_TREE_12 = new TileType(412);
	public static final TileType DEAD_PINE_TREE_22 = new TileType(413);
	public static final TileType DEAD_PINE_TREE_13 = new TileType(414);
	public static final TileType DEAD_PINE_TREE_23 = new TileType(415);
	
	public static final TileType OAK_TREE_11 = new TileType(416);
	public static final TileType OAK_TREE_21 = new TileType(417);
	public static final TileType OAK_TREE_12 = new TileType(418);
	public static final TileType OAK_TREE_22 = new TileType(419);
	public static final TileType OAK_TREE_13 = new TileType(420);
	public static final TileType OAK_TREE_23 = new TileType(421);
	
	
	public static final TileTypeGroup OAK_TREE_GROUP = new TileTypeGroup(2, 3, OAK_TREE_11, OAK_TREE_21, OAK_TREE_12, OAK_TREE_22, 
			OAK_TREE_13, OAK_TREE_23);
	
	public static final TileTypeGroup WILLOW_TREE_GROUP = new TileTypeGroup(1, 2, WILLOW_TREE_TOP, WILLOW_TREE_BOT);
	
	public static final TileTypeGroup PINE_TREE_GROUP = new TileTypeGroup(2, 4, PINE_TREE_11, PINE_TREE_21, PINE_TREE_12, 
			PINE_TREE_22, PINE_TREE_13, PINE_TREE_23, PINE_TREE_14, PINE_TREE_24);


	public static final TileTypeGroup DEAD_PINE_TREE_GROUP = new TileTypeGroup(2, 3, DEAD_PINE_TREE_11, DEAD_PINE_TREE_21, DEAD_PINE_TREE_12,
			DEAD_PINE_TREE_22, DEAD_PINE_TREE_13, DEAD_PINE_TREE_23);
	
	public static final TileType[] MOSTLY_EMPTY_TREE_SPRITES = new TileType[] { PINE_TREE_14, PINE_TREE_24, OAK_TREE_22, OAK_TREE_13, OAK_TREE_23}; 

	
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

	public static final TileType CHAIR_FACING_LEFT = new TileType(1054);
	public static final TileType CHAIR_FACING_RIGHT = new TileType(1055);
	public static final TileType WOOD_TABLE = new TileType(1056);
	
	public static final TileType WOOD_TABLE_SILVER_LEFT_BEERMUG = new TileType(1057);
	public static final TileType WOOD_TABLE_SILVER_RIGHT_BERRMUG = new TileType(1058);
	public static final TileType TABLE_SILVER_RIGHT = new TileType(1059);
	public static final TileType TABLE_SILVER_FULL = new TileType(1060);
	
	public static final TileType[] ALL_TABLES = new TileType[] {
			WOOD_TABLE,
			WOOD_TABLE_SILVER_LEFT_BEERMUG,
			WOOD_TABLE_SILVER_RIGHT_BERRMUG,
			TABLE_SILVER_RIGHT,
			TABLE_SILVER_FULL
	};
	
	public static final TileTypeGroup TABLE_AND_CHAIRS_GROUP = new TileTypeGroup(3, 1, CHAIR_FACING_RIGHT, WOOD_TABLE, CHAIR_FACING_LEFT);
	
	public static final TileType TORCH = new TileType(34);
	
	public static final TileType CANDLE_STICK = new TileType(35);
	public static final TileType TREASURE_CHEST = new TileType(36);
	public static final TileType BOOKSHELF = new TileType(37);
	public static final TileType BOOKSHELF_CANDLE = new TileType(38);
	public static final TileType BOOKSHELF_EMPTY = new TileType(39);
	
	public static final TileType FURNACE_TOP = new TileType(1062);
	public static final TileType FURNACE_BOTTOM = new TileType(1063);
	
	
	public static final TileTypeGroup FURNACE_GROUP = new TileTypeGroup(1, 2, FURNACE_TOP, false, FURNACE_BOTTOM, true);	
	
	public static void init() {
		// This method is a no-op, but is required (as it causes the static code to run), so don't remove it.
	}
}
