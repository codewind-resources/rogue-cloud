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
import java.util.HashMap;
import java.util.Map.Entry;

import com.roguecloud.GameEngine.GameContext;
import com.roguecloud.creatures.MonsterTemplateList;
import com.roguecloud.db.DatabaseInstance;
import com.roguecloud.items.ArmourList;
import com.roguecloud.items.WeaponList;
import com.roguecloud.map.TileType;
import com.roguecloud.map.TileTypeList;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.RoomList;
import com.roguecloud.utils.RoomList.TilePair;

/** 
 * Unless multiple server instances are being run (which is architecturally possible, but not current tested/supported), a single
 * instance of this class will exist while the Rogue Cloud Server is running.
 * 
 * This class is responsible for preserving global state data (armour list, weapons list, id generator, etc), and for
 * starting new rounds and destroying old rounds.
 * 
 * Objects in this class exist between rounds; unlike RoundScope, they are not (necessarily) destroyed between rounds.
 * */
public final class ServerInstance {
	
	private final Logger log = Logger.getInstance();

	private final Object lock = new Object();
	
	private final ArmourList armourList;
	private final WeaponList weaponList;
	private final MonsterTemplateList monsterTemplateList;
	private final RoomList roomList;

	private final UniqueIdGenerator idGenerator;
	
	private final String tileListJson;
	
	private RoundScope currentRound_synch_lock;
	
	
	public ServerInstance() throws IOException {
		try {
				
			LogContext lc = LogContext.serverInstance(getId());
			
			idGenerator = new UniqueIdGenerator();	
			
			armourList = new ArmourList(idGenerator, lc);
			weaponList = new WeaponList(idGenerator, lc);
			monsterTemplateList = new MonsterTemplateList(lc);
			
			roomList = new RoomList(lc);
			
			this.tileListJson = generateTileListJson();
			
			startNewRound();
		} catch(Throwable e) {
			e.printStackTrace();
			throw e;
		}

	}
	
	public void startNewRound() throws IOException {
		LogContext lc = LogContext.serverInstance(getId());
		
		synchronized(lock) {
			
			if(currentRound_synch_lock != null) {
				currentRound_synch_lock.setRoundComplete(true);
				currentRound_synch_lock.dispose();
			}

			// TODO: CURRENT - This is a case where the main thread is waiting on the database. 
			long id = DatabaseInstance.get().getAndIncrementNextRoundId();
			
			currentRound_synch_lock = new RoundScope(id, RCConstants.ROUND_LENGTH_IN_NANOS, id+1, this);

			// Each game context gets a new id generator, based on a prototypical generator
			// which contains the armour/weapons/monsters.
			idGenerator.freeze();
			
			GameContext gc = new GameContext(currentRound_synch_lock, getArmourList(), getWeaponList(), getMonsterTemplateList(), getRoomList(), idGenerator.fullClone(), lc);
			
			synchronized(lock) {
				new GameEngine(currentRound_synch_lock, this, gc).startThread();
			}
			
			log.interesting("Starting new round #"+id, lc);
		}
	}
	
	public int getId() {
		return 0;
	}
	
	public ArmourList getArmourList() {
		return armourList;
	}
	
	public RoomList getRoomList() {
		return roomList;
	}
		
	public MonsterTemplateList getMonsterTemplateList() {
		return monsterTemplateList;
	}
	
	public WeaponList getWeaponList() {
		return weaponList;
	}
	
	public String getTileListJson() {
		return tileListJson;
	}
	
	public RoundScope getCurrentRound() {
		synchronized(lock) {
			return currentRound_synch_lock;
		}
	}

	/** The browser client needs a list of all the available .PNG images that the server may send. This method 
	 * returns a list of all tiles, as a JSON string, for use by the browser. */
	private String generateTileListJson() {
		HashMap<Integer /* tile number */, String /* tile name */> map = new HashMap<>();

		TileTypeList.init();
		
		for(Entry<Integer, String> t : TileType.getValues().entrySet()) {
//			map.put(t.getKey(), "");
			String value = t.getValue() != null ? t.getValue().trim() : ""; 
			map.put(t.getKey(), value);
		}
		
		monsterTemplateList.getList().stream().forEach( (mt) -> {map.put(mt.getTileType().getNumber(), mt.getName()); });
		
		armourList.getList().stream().forEach( (a) -> { map.put(a.getTileType().getNumber(), a.getName()); });
		
		weaponList.getList().stream().forEach( (w) -> {map.put(w.getTileType().getNumber(), w.getName()); });
		
		roomList.getRooms().stream().flatMap( (e) -> e.getAssignments().stream())
			.forEach( a -> {
				
				int index = 0;
				for(TilePair tp : a.getTilePair()) {
					if(tp == null) {
						log.severe("Foreground tile pair is null: " + a.getLetter()+" "+a.getName(), null);
						return; 
					}  

					if(tp.getTileNumber() == -1) { continue; }
					
					String name = "";
					if(index == 0) {
						name = a.getName();
					}
					
					map.put(tp.getTileNumber(), name);
					
					index++;
				}
				
		});
		
		StringBuilder tilesSb = new StringBuilder("[");
		
		map.entrySet().forEach( e -> {
			tilesSb.append("["+e.getKey()+",\""+e.getValue()+"\"],");
		});
		
		tilesSb.append("]");
		return tilesSb.toString();
		
	}
	

}
