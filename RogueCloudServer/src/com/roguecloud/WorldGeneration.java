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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.roguecloud.UniqueIdGenerator.IdType;
import com.roguecloud.WorldGenFromFile.RoomSpawn;
import com.roguecloud.WorldGenFromFile.WorldGenFromFileResult;
import com.roguecloud.WorldGenerationUtil.DrawRoomResult;
import com.roguecloud.ai.WanderingAttackAllAI;
import com.roguecloud.client.MonsterClient;
import com.roguecloud.creatures.Monster;
import com.roguecloud.creatures.MonsterTemplate;
import com.roguecloud.creatures.MonsterTemplateList;
import com.roguecloud.items.Armour;
import com.roguecloud.items.ArmourSet;
import com.roguecloud.items.DrinkableItem;
import com.roguecloud.items.Effect;
import com.roguecloud.items.GroundObject;
import com.roguecloud.items.IObject;
import com.roguecloud.items.PotionFactory;
import com.roguecloud.items.Weapon;
import com.roguecloud.items.WeaponList;
import com.roguecloud.map.IMutableMap;
import com.roguecloud.map.ITerrain;
import com.roguecloud.map.ImmutablePassableTerrain;
import com.roguecloud.map.RCArrayMap;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;
import com.roguecloud.map.TileTypeList;
import com.roguecloud.utils.AIUtils;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.MonsterFactory;
import com.roguecloud.utils.RoadGenerationAlgorithm;
import com.roguecloud.utils.RoomList;
import com.roguecloud.utils.SimpleMap;
import com.roguecloud.utils.UniverseParserUtil;
import com.roguecloud.utils.MonsterFactory.MonsterFactoryResult;

/** Utility methods for various aspects of world generation. */
public class WorldGeneration {
	
	public static enum WorldGenAIType { WANDERING, GUARD };
	
	private static final Logger log = Logger.getInstance();

	public static GenerateWorldResult generateDefaultWorld(ServerInstance parent, UniqueIdGenerator idGen, LogContext lc) {
				
		WorldGenFromFileResult wgResult;
		
		try {
			wgResult = WorldGenFromFile.generateMapFromInputStream(parent.getRoomList(), UniverseParserUtil.class.getClassLoader().getResourceAsStream("/universe/map.txt") );
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		RCArrayMap map = wgResult.getMap();
		
		{
			
			List<GroundObject> goList = new ArrayList<>();
			List<AIContext> aiContextList = new ArrayList<>();

			List<Monster> monsters = generatePairedItemsAndMonsters(parent, idGen, map, goList, 40, 40, wgResult.getSpawns(), aiContextList);
			
			
			// Generate some wandering monsters too
			{
				for(int x = 0; x < 20; x++) {
					Position p = AIUtils.findRandomPositionOnMap(0, 0, map.getXSize(), map.getYSize(), false, map);
					if(p != null) {
						Monster m = generateSingleMonsterAtPosition(map, p, parent, idGen, aiContextList, new AIHint(WorldGenAIType.WANDERING, false));
						if(m != null) {
							monsters.add(m);
						}
					}
				}
			}
			
			List<Position> posOfShrubsToAdd = new ArrayList<>();
		
			int numberOfShrubs = (int)(map.getXSize()*map.getYSize()*0.002); 
			while(posOfShrubsToAdd.size() < numberOfShrubs) {
				
				boolean goodPosition = false;
				Position p = null;
				while(!goodPosition) {
					p = AIUtils.findRandomPositionOnMap(0, 0, map.getXSize(), map.getYSize(), true, map);
					Tile t = map.getTile(p);
					if(t != null && t.isPresentlyPassable() && t.getGroundObjects().size() == 0 && t.getCreatures().size() == 0 && t.getTileProperties().size() == 0) {
						TileType[] tt = t.getTileTypeLayers();
						if(tt != null && tt.length == 1 && tt[0].getNumber() == TileTypeList.GRASS.getNumber()) {
							final Position fP = p;
							
							if(!posOfShrubsToAdd.stream().anyMatch(e -> e.distanceBetween(fP) < 10 )) {
								goodPosition = true;	
							}
						}
					}
					
				}
				
				if(p != null) {
					posOfShrubsToAdd.add(p);
				}
			}
			
			TileType[] shrubTiles = new TileType[] {
					TileTypeList.SHRUB_1,
					TileTypeList.SHRUB_2,
					TileTypeList.SHRUB_3,
					TileTypeList.SHRUB_4,
					TileTypeList.SHRUB_5,
					TileTypeList.SHRUB_6
			};
			
			posOfShrubsToAdd.forEach( e -> {
				
				TileType newTileType = shrubTiles[(int)(Math.random()*shrubTiles.length)];
				
				Tile newTile = new Tile(true, new ImmutablePassableTerrain(newTileType), new ImmutablePassableTerrain(TileTypeList.GRASS));
				
				map.putTile(e, newTile);
				
			});
			
			System.out.println("* Verifying map integrity.");
			{
				Map<Position, Boolean> added = new HashMap<>();
				
				SimpleMap<Boolean> accessMap = new SimpleMap<Boolean>(map.getXSize(), map.getYSize());
				for(int y = 0; y < map.getYSize(); y++) {
					for(int x = 0; x < map.getXSize(); x++) {
						accessMap.putTile(x, y, false);
					}
				}
				
				Position pq = AIUtils.findRandomPositionOnMap(0, 0, map.getXSize(), map.getYSize(), false, map);
				List<Position> posList = new ArrayList<>();
				posList.add(pq);
				while(posList.size() > 0) {
					Position currPos = posList.remove(0);
					
					if(!map.getTile(currPos).isPresentlyPassable()) {
						log.severe("Position on map returned a non-passable tile, which should not happen.", lc);
					}
					
					accessMap.putTile(currPos.getX(), currPos.getY(), true);
					
					// Add neighbours
					for(Position nextPos : AIUtils.getValidNeighbouringPositions(currPos, map)) {
						
						Tile t = map.getTile(nextPos);
						
						Boolean nextBool = accessMap.getTile(nextPos.getX(), nextPos.getY());
						if( (nextBool == null || nextBool == false) && t != null && t.isPresentlyPassable() && added.get(nextPos) == null)  {
							posList.add(nextPos);
							added.put(nextPos, true);
						}
					}
					
				}
				
				for(int y = 0; y < map.getYSize(); y++) {
					for(int x = 0; x < map.getXSize(); x++) {
						Tile realMapTile = map.getTile(x, y);
						Boolean simpleMapAccessible = accessMap.getTile(x, y);
						
						if(realMapTile.isPresentlyPassable() && !simpleMapAccessible) {
							log.severe("Map integrity error at: ("+x+", "+y+")", lc);
						}
					}
				}

				
//				String dump = accessMap.dumpMap(e -> {
//					return e != null ? (e.booleanValue() ? "t" : "f") : "f";
//				});
//				
//				System.out.println(dump);
				
			}
			
			return new GenerateWorldResult(map, goList, monsters, wgResult.getSpawns(), aiContextList);
		}
				
	}

	
	public static List<Monster> generatePairedItemsAndMonsters(ServerInstance parent, UniqueIdGenerator idGen, RCArrayMap map, 
			List<GroundObject> goList, final int monstersToGenerate, final int itemsToGenerate, List<RoomSpawn> roomSpawnList,
			List<AIContext> aiContextList) {
		
		if(roomSpawnList.size() == 0) {
			throw new IllegalArgumentException("Room spawn list is empty.");
		}
		
		List<Monster> result = new ArrayList<>();
		
		List<RoomSpawn> spawnsUsed = new ArrayList<RoomSpawn>();
		{
			List<RoomSpawn> spawnsRemaining = new ArrayList<>();
			int numItemsRemaining = itemsToGenerate;
			while(numItemsRemaining > 0) {
				if(spawnsRemaining.size() == 0) {
					spawnsRemaining.addAll(roomSpawnList);
					Collections.shuffle(spawnsRemaining);
				}
				
				Position p = null;
				RoomSpawn rs = spawnsRemaining.remove(0);
				if(rs.getItemSpawnsInRoom().size() > 0) {
					p = rs.getItemSpawnsInRoom().get((int) ( rs.getItemSpawnsInRoom().size() *Math.random() ));
				}
	
				
				if(p != null) {
					generateItemAtPosition(parent, idGen, map, p, goList);
					spawnsUsed.add(rs);
					numItemsRemaining--;
				}
			}
		}
		
		int numMonstersRemaining = monstersToGenerate;
		while(numMonstersRemaining > 0) {
			
			if(spawnsUsed.size() == 0) {
				spawnsUsed.addAll(roomSpawnList);
				Collections.shuffle(spawnsUsed);
			}
			
			Position p = null;
			RoomSpawn rs = spawnsUsed.remove(0);
			if(rs.getMonsterSpawnsInRoom().size() > 0) {
				p = rs.getMonsterSpawnsInRoom().get((int) ( rs.getMonsterSpawnsInRoom().size() * Math.random() ));
			}
			
			
			if(p != null) {
				Monster m = generateSingleMonsterAtPosition(map, p, parent, idGen, aiContextList, new AIHint(WorldGenAIType.GUARD, true));
				result.add(m);
				numMonstersRemaining--;
			}
		}
		
		return result;
		
	}
	
	
	public static GroundObject generateItemAtPosition(ServerInstance parent, UniqueIdGenerator idGen, RCArrayMap map, Position newPos, List<GroundObject> goList) {
		IObject newObj = null;
		
		double randVal = Math.random();
		
		if(randVal < 0.4) {
			Weapon w = null;
			while(w == null) {
				List<Weapon> wl = parent.getWeaponList().getList();
				int weaponIndex = (int)(Math.random() * wl.size());
				w = wl.get(weaponIndex);
				if(w.getName().equals(GameEngine.BARE_HANDS_DEFAULT_WEAPON)) {
					w =  null;
				} 
			}
			newObj = w;
		} else if(randVal < 0.8) {
			Armour a = null;
			while(a == null) {
				List<Armour> al = parent.getArmourList().getList();
				int index = (int)(Math.random() * al.size());
				a = al.get(index);
			}
			newObj = a;
			
		} else {
			Effect e = PotionFactory.generate();
			DrinkableItem di = new DrinkableItem(TileTypeList.SMALL_BOTTLE, e, idGen.getNextUniqueId(IdType.OBJECT));
			newObj = di;
		}
		
		GroundObject go = new GroundObject(idGen.getNextUniqueId(IdType.OBJECT), newObj, newPos);
		Tile t = map.getTileForWriteUnchecked(newPos); 
		t.getGroundObjectForModification().add(go);
		goList.add(go);
		
		return go;
	}
	
	public static Monster generateSingleMonster(IMutableMap map, List<RoomSpawn> spawnList, ServerInstance parent, UniqueIdGenerator idGen, 
			List<AIContext> aiContextList, AIHint aiHint) {

		Position monsterPos = null;

		if(spawnList == null || spawnList.size() == 0) {
			do {
				monsterPos = new Position((int)(Math.random() * map.getXSize()), (int)(Math.random() * map.getYSize()));
			} while(!map.getTile(monsterPos).isPresentlyPassable());
		} else {
			
			boolean positionFound = false;
			while(!positionFound) {
				RoomSpawn rs = spawnList.get( (int)(Math.random() * spawnList.size()) ); // pick a random room 
				
				if(rs.getMonsterSpawnsInRoom().size() > 0) {
					monsterPos = rs.getMonsterSpawnsInRoom().get( (int)(Math.random() *  rs.getMonsterSpawnsInRoom().size() )); // Pick a random spawn in the room
					positionFound = true;
				}
			}
			
		}
		
		if(monsterPos == null) {
			log.severe("Monster position was null in generateSingleMonster which should not happen.", null);
			return null;
		}

		return generateSingleMonsterAtPosition(map, monsterPos, parent, idGen, aiContextList, aiHint);

	}

	public static Monster generateSingleMonsterAtPosition(IMutableMap map, Position position, ServerInstance parent, UniqueIdGenerator idGen, List<AIContext> aiContextList, AIHint aiHint) {
		if(position == null) { throw new IllegalArgumentException(); }

		WeaponList weaponList = parent.getWeaponList();
		MonsterTemplateList monsterTemplateList = parent.getMonsterTemplateList();

		Weapon bareHands = weaponList.getList().stream()
				.filter( e -> e.getName().equals(GameEngine.BARE_HANDS_DEFAULT_WEAPON)  )
				.findFirst()
				.orElseThrow( () -> new IllegalArgumentException("Bare Hands not found.") ); 


		MonsterTemplate mt = monsterTemplateList.getList().get((int)(monsterTemplateList.getList().size() * Math.random())    ); 
		MonsterClient ai = null;
		{
			
			if(aiHint.getType() == WorldGenAIType.GUARD) {
				Position topLeft = new Position(position.getX()-4, position.getY()-4);
				Position botRight = new Position(position.getX()+4, position.getY()+4);
				
				if(topLeft.isValid(map) && botRight.isValid(map)) {
					WanderingAttackAllAI wai = new WanderingAttackAllAI(topLeft, botRight, true);
					wai.setAttackOtherMonsters(aiHint.isAttackMonsters());
					
					ai = wai;
					
				}
				
			} else if(aiHint.getType() == WorldGenAIType.WANDERING) {
				WanderingAttackAllAI  wai = new WanderingAttackAllAI();
				wai.setAttackOtherMonsters(aiHint.isAttackMonsters());
				ai = wai;
			} else {
				log.severe("Unrecognize AI hint type: "+aiHint.getType(), null);
				return null;
			}
			
		}
		
		MonsterFactory mf = new MonsterFactory();
		MonsterFactoryResult mfr = mf.generate(mt.getMinLevelRange(), mt.getMaxLevelRange());
		int level = mfr.getLevel();
		
		int maxHp = 15+level*2;
//		if(level >= 0 && level <= 6) {
//			maxHp = 20;
//		} else if(level >= 7 && level <= 11) {
//			maxHp = 30;
//		} else if(level > 11) {
//			maxHp = 40;
//		}

		Monster m = new Monster(mt.getName(), idGen.getNextUniqueId(IdType.CREATURE),
				position,
				mt.getTileType(),
				bareHands, 
				level, new ArmourSet());
		
		
		if(ai != null) {
		
			AIContext context = new AIContext(m.getId(), ai);
			ai.setAIContext(context);
			aiContextList.add(context);

//			m.setWeapon(weaponList.getList().get(0)); 
			m.setMaxHp(maxHp);
			m.setCurrHp(m.getMaxHp());
			
			
			map.getTileForWriteUnchecked(position).getCreaturesForModification().add(m);
		}
		
		return m;

	}
	
	/** Return value of generateDefaultWorld(...); see private fields of this class for what is returned. */
	public static class GenerateWorldResult {
		
		private final RCArrayMap newMap;
		
		private final List<GroundObject> newGroundObjects;
		private final List<Monster> newMonsters;
		
		private final List<AIContext> aiContextList;
		
		private final List<RoomSpawn> roomSpawns; 
		
		public GenerateWorldResult(RCArrayMap newMap, List<GroundObject> newGroundObjects, List<Monster> newMonsters, List<RoomSpawn> roomSpawns, List<AIContext> aiContextList) {

			this.newMap = newMap;
			this.newGroundObjects = newGroundObjects;
			this.newMonsters = newMonsters;
			this.aiContextList = aiContextList;
			this.roomSpawns = roomSpawns;
		}


		public RCArrayMap getNewMap() {
			return newMap;
		}

		public List<GroundObject> getNewGroundObjects() {
			return newGroundObjects;
		}

		public List<Monster> getNewMonsters() {
			return newMonsters;
		}
		
		public List<AIContext> getAIContextList() {
			return aiContextList;
		}
		
		public List<RoomSpawn> getRoomSpawns() {
			return roomSpawns;
		}
	}
	
	@SuppressWarnings("unused")
	private static void generateWorld(IMutableMap m, RoomList rooms) {
		int X_SIZE = m.getXSize();
		int Y_SIZE = m.getYSize();
		
		
		ITerrain grass = new ImmutablePassableTerrain(TileTypeList.GRASS);
		for(int x = 0; x < X_SIZE; x++) {
			
			for(int y = 0; y < Y_SIZE; y++) {
				Tile t = new Tile(true, null, grass);
				m.putTile(x, y, t);
				
			}
		}
		
		WorldGenerationUtil.drawBrickFence(m);
		
		{
			RoadGenerationAlgorithm rga = new RoadGenerationAlgorithm(X_SIZE, Y_SIZE, 20, 10);
			rga.runAlgorithm();
			int[][] result = rga.getResultMap();
			
			for(int x = 0; x < X_SIZE; x++) {
				for(int y = 0; y < Y_SIZE; y++) {
					if(result[x][y] != 0) {
						
						Tile t = new Tile(true, null, new ImmutablePassableTerrain(TileTypeList.DIRT_PATH));
						m.putTile(x,  y, t);
						
					}
				}
			}
			
			System.out.println("-------------------------------------------------");
			
			
//			Room room = rooms.getRooms().get(1);
//			
//			for(DirectedEdge d : rga.getResultEdges().stream().filter( e -> !e.isHorizontal()).collect(Collectors.toList())) {
//
//				System.out.println(d);
//				
//				m.putTile(d.getSrc().getX(), d.getSrc().getY(), new Tile(new ImmutablePassableTerrain(TileTypeList.CYBER_KNIGHT), null));
//				m.putTile(d.getDest().getX(), d.getDest().getY(), new Tile(new ImmutablePassableTerrain(TileTypeList.WOOD_WALL_STRAIGHT_HORIZ), null));
//				
//				
//				int startX = d.getSrc().getX()+3;
//				int startY = d.getSrc().getY()+2;
//				
//				while(startY < m.getYSize()) {
//	
//					drawRoom(room, startX, startY, m);
//					
//					startY += room.getHeight()+1;
//				}
//				
//				
//				
//				
//			}
//						
		}
		
		DrawRoomResult result = WorldGenerationUtil.drawRoom(rooms.getRooms().get(1), 4, 4, 0, m, false);
		
	}
	
	
	/** When calling the generateMonster methods, you can pass in an AIHint, which indicates to the method what type of 
	 * AI to generate (if applicable). The specific AI implementation that corresponds to each 
	 * of these AIHint values is up to the generateMonster method. 
	 * 
	 **/
	public static class AIHint {
		
		/** Whether to wander the world, or guard a specific defined area */
		private final WorldGenAIType type;
		
		/** Whether to attack other monster (if false, will only attack players) */
		private final boolean attackMonsters;
		
		public AIHint(WorldGenAIType type, boolean attackMonsters) {
			if(type == null) { throw new IllegalArgumentException(); }
			this.type = type;
			this.attackMonsters = attackMonsters;
		}
		
		public WorldGenAIType getType() {
			return type;
		}
		
		public boolean isAttackMonsters() {
			return attackMonsters;
		}
		
	}
}
