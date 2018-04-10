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

package com.roguecloud.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.roguecloud.RCRuntime;
import com.roguecloud.actions.CombatActionResponse;
import com.roguecloud.actions.DrinkItemActionResponse;
import com.roguecloud.actions.EquipActionResponse;
import com.roguecloud.actions.IActionResponse;
import com.roguecloud.actions.MoveInventoryItemActionResponse;
import com.roguecloud.actions.NullActionResponse;
import com.roguecloud.actions.StepActionResponse;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.creatures.Monster;
import com.roguecloud.creatures.PlayerCreature;
import com.roguecloud.events.CombatActionEvent;
import com.roguecloud.events.DrinkItemActionEvent;
import com.roguecloud.events.EquipActionEvent;
import com.roguecloud.events.IEvent;
import com.roguecloud.events.MoveInventoryItemActionEvent;
import com.roguecloud.events.StepActionEvent;
import com.roguecloud.items.Armour;
import com.roguecloud.items.ArmourSet;
import com.roguecloud.items.DrinkableItem;
import com.roguecloud.items.Effect;
import com.roguecloud.items.GroundObject;
import com.roguecloud.items.IObject;
import com.roguecloud.items.IObject.ObjectType;
import com.roguecloud.items.OwnableObject;
import com.roguecloud.items.Weapon;
import com.roguecloud.json.JsonActionMessageResponse;
import com.roguecloud.json.JsonArmour;
import com.roguecloud.json.JsonDoorProperty;
import com.roguecloud.json.JsonDrinkableItem;
import com.roguecloud.json.JsonFrameUpdate;
import com.roguecloud.json.JsonVisibleCreature;
import com.roguecloud.json.JsonVisibleObject;
import com.roguecloud.json.JsonWeapon;
import com.roguecloud.json.JsonWorldState;
import com.roguecloud.json.JsonWorldState.JsonViewFrame;
import com.roguecloud.json.actions.JsonCombatActionResponse;
import com.roguecloud.json.actions.JsonDrinkItemActionResponse;
import com.roguecloud.json.actions.JsonEquipActionResponse;
import com.roguecloud.json.actions.JsonMoveInventoryItemActionResponse;
import com.roguecloud.json.actions.JsonNullActionResponse;
import com.roguecloud.json.actions.JsonStepActionResponse;
import com.roguecloud.json.browser.JsonUpdateBrowserUI;
import com.roguecloud.json.events.JsonCombatActionEvent;
import com.roguecloud.json.events.JsonDrinkItemActionEvent;
import com.roguecloud.json.events.JsonEquipActionEvent;
import com.roguecloud.json.events.JsonMoveInventoryItemActionEvent;
import com.roguecloud.json.events.JsonStepActionEvent;
import com.roguecloud.map.DoorTileProperty;
import com.roguecloud.map.IMap;
import com.roguecloud.map.ITerrain;
import com.roguecloud.map.ImmutableImpassableTerrain;
import com.roguecloud.map.ImmutablePassableTerrain;
import com.roguecloud.map.RCCloneMap;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;

/** 
 * Contains the current state of the world for the current round; the data in this class are used to provide up-to-date data on
 * the current world state to the IClient/RemoteClient interface of the player's AI code.
 *
 * The data in this class are predominantly updated by JsonFrameUpdates received from the server, which contains everything
 * the player can see in their current view, as well as any observed events during the frame. JsonFrameUpdates are processed in
 * receiveFrameUpdate(...). 
 * 
 * This class also receives JsonActionMessageResponse responses from the server, which are passed to the relevant
 * ActionResponseFuture.
 * 
 * This class also receives BrowserUIUpdates, which are passed to the world state listeners.
 * 
 * This class is for internal server use only.
 **/
public class ClientWorldState {
	
	private static final Logger log = Logger.getInstance();

	private final Object lock = new Object();
	
	private final LogContext lc;
	
	/** Our parent class */
	private final ClientState clientState;

	/** The most recently received SelfState from the server */
	private SelfState selfState_synch_lock;
	
	/** Frame updates received from the server that we have not yet processed. */
	private final HashMap<Long /* frame id*/, JsonFrameUpdate> unprocessedFrames_synch_lock = new HashMap<>();

	/** The id of the next frame that we _expect_ to received from the server. */
	private long nextFrame_synch_lock = 1;
	
	private RCCloneMap map_synch_lock = null;
	
	/** Players that our character has seen */
	private final HashMap<Long /* player id*/, PlayerCreature> playerDb_sync_lock = new HashMap<>();
	
	/** Monsters that our character has seen*/
	private final HashMap<Long /* creature id*/, Monster> monsterDb_synch_lock = new HashMap<>();
	
	/** Objects that our character has seen*/
	private final HashMap<Long /* armour/weapon/drinkable id*/, IObject> objectDb_synch_lock = new HashMap<>();
	
	/** The X most recent events that occurred within our character's field of view */
	private final EventLog event_log_synch_lock = new EventLog(200);
	
	/** An object that we pass to the client API, to allow the user to get the most recent map data. See that class description
	 * for details. */
	private ClientMap clientMap;
	
	/** The most recently received world state */
	private WorldState worldState;
	// TODO: CURR - Why is this WorldState reference not synchronized (and with the appropriate naming convention)? 
	
	private boolean disposed = false;
	
	public ClientWorldState(ClientState clientState, LogContext lc) {
		this.clientState = clientState;
		this.lc = lc;
	}

	/** The server has replied to one of our player's action, so process it and pass it to the future */
	public void processActionResponse(JsonActionMessageResponse o) {
		if(disposed) { return; }
		
		Map<?, ?> map = (Map<?, ?>) o.getActionResponse();
		
		String type = (String) map.get("type");
		
		IActionResponse response = null;
		if(type.equals(JsonCombatActionResponse.TYPE)) {
			JsonCombatActionResponse r = new JsonCombatActionResponse(o);
			
			ICreature creature = null;
			synchronized(lock) {
				if(r.getTargetCreatureId() != -1) {
					creature = playerDb_sync_lock.get(r.getTargetCreatureId());
					if(creature == null) {
						creature = monsterDb_synch_lock.get(r.getTargetCreatureId());
					}
				}
			}
			
			CombatActionResponse car = new CombatActionResponse(CombatActionResponse.CombatActionResult.valueOf(r.getResult()), r.getDamageDealt(), creature);
			
			response = car;
			
		} else if(type.equals(JsonNullActionResponse.TYPE)) {
			
			NullActionResponse nar = NullActionResponse.INSTANCE;
			
			response = nar;
			
			
		} else if(type.equals(JsonStepActionResponse.TYPE)) {
			JsonStepActionResponse r = new JsonStepActionResponse(o);
			StepActionResponse sar;
			if(r.isSuccess()) {
				sar = new StepActionResponse(r.getNewPosition().toPosition());	
			} else {
				sar = new StepActionResponse(StepActionResponse.StepActionFailReason.valueOf(r.getFailReason()));
			}
			
			response = sar;
		
		} else if(type.equals(JsonMoveInventoryItemActionResponse.TYPE)) {
			JsonMoveInventoryItemActionResponse jpuiar = new JsonMoveInventoryItemActionResponse(o);
			MoveInventoryItemActionResponse r = new MoveInventoryItemActionResponse(jpuiar.getObjectId(), jpuiar.isSuccess(), jpuiar.isDropItem());
			response = r;
			
		} else if(type.equals(JsonEquipActionResponse.TYPE)) {
			JsonEquipActionResponse jear = new JsonEquipActionResponse(o);
			EquipActionResponse ear = new EquipActionResponse(jear.isSuccess(), jear.getObjectId());
			response = ear;
			
		} else if(type.equals(JsonDrinkItemActionResponse.TYPE)) {
			JsonDrinkItemActionResponse jdiar = new JsonDrinkItemActionResponse(o);
			
			Effect e = null;
			if(jdiar.getEffect() != null) {
				e = new Effect(jdiar.getEffect());
			}
			
			DrinkItemActionResponse diar = new DrinkItemActionResponse(jdiar.isSuccess(), jdiar.getId(), e);
			response = diar;
			
		} else {
			log.severe("Unrecognized message response type: "+type, lc);
			return;
		}
		
		clientState.forwardActionResponseToClient(o.getMessageId(), response);
				
	}
	
	public void informRoundComplete(int nextRoundInXSeconds) {
		
		WorldStateListeners.getInstance().getListeners().forEach( e -> {
			e.roundComplete(nextRoundInXSeconds);
		});
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void receiveFrameUpdate(JsonFrameUpdate update) {
		
		if(disposed) { return; }
		
//		log.info("Processing frame: "+update.getFrame()+", game ticks: "+update.getGameTicks(), lc);
		
		RCCloneMap mapToCallAIWith = null;
	
		JsonWorldState jws;
		synchronized (lock) {
			
			if(update.isFull()) {
				// If the server has sent us a full frame, then we can throw away the delta frames.
				unprocessedFrames_synch_lock.clear();
				log.info("Throwing away delta frames, as we received a full frame: "+update.getFrame()+", with game ticks: "+update.getGameTicks(), lc);
				
				nextFrame_synch_lock = update.getFrame();
				
			} else if(update.getFrame() != nextFrame_synch_lock) {
				log.info("Adding frame "+update.getFrame()+" to unprocessed, next frame is: "+nextFrame_synch_lock+", game tick is: "+update.getGameTicks(), lc);
				unprocessedFrames_synch_lock.put(update.getFrame(), update);
				return;
			}

			jws = update.getWorldState();
						
			if(map_synch_lock == null) {
				map_synch_lock = new RCCloneMap(jws.getWorldWidth(), jws.getWorldHeight());
			}
			
			RCCloneMap localMap = map_synch_lock.cloneMap();
			
			// Clear the localMap's view tiles of objects and monsters (we will add them back in the next section)
			for(int x = jws.getClientViewPosX(); x < jws.getClientViewPosX()+jws.getClientViewWidth(); x++) {
				for(int y = jws.getClientViewPosY(); y < jws.getClientViewPosY()+jws.getClientViewHeight(); y++) {
					
					Tile t = localMap.getTile(x, y);
					if(t == null) { continue; }
					
					t.getCreaturesForModification().clear();
					t.getGroundObjectForModification().clear();
					t.getTilePropertiesForModification().clear();
					
				}
			}
			
			// Apply the tile updates to localMap
			for(JsonViewFrame jvf : jws.getFrames()) {
				int index =0;
				for(int y = 0; y < jvf.getH(); y++) {
					for(int x = 0; x < jvf.getW(); x++) {
						
						int worldPosX = jws.getClientViewPosX()+x+jvf.getX();
						int worldPosY = jws.getClientViewPosY()+y+jvf.getY();
						
						List layers = (List)jvf.getData().get(index);
						// layers (/\) looks like this: [0,[2115]]
						Integer passableInt = (Integer)layers.get(0);
						boolean passable = passableInt == 1;
						
						
						ITerrain fgTerrain = null, bgTerrain = null;
						
						if(layers.size() == 3) {
							List<Integer> fgLayer = (List<Integer>) layers.get(1);
							fgTerrain = createTerrain(passable, fgLayer, lc);
							
							List<Integer> bgLayer = (List<Integer>) layers.get(2);
							bgTerrain = createTerrain(passable, bgLayer, lc);
							
						} else {
							List<Integer> fgLayer = (List<Integer>) layers.get(1);
							fgTerrain = createTerrain(passable, fgLayer, lc);
						}
						Tile newTile = new Tile(passable, fgTerrain, bgTerrain);
						newTile.setLastTickUpdated(update.getGameTicks());
						
						localMap.putTile(worldPosX,  worldPosY, newTile);
						
						
						index++;
					}
				}
			}
			
			// Update the object, player, and monster dbs, as well as the local map
			
			for(JsonArmour a : jws.getArmours()) {
				Armour newArmour = new Armour(a);
				objectDb_synch_lock.put(newArmour.getId(), newArmour);
			}
			
			for(JsonWeapon w : jws.getWeapons()) {
				Weapon newWeapon = new Weapon(w);
				objectDb_synch_lock.put(newWeapon.getId(), newWeapon);
			}
			
			for(JsonDrinkableItem jdi : jws.getDrinkables()) {
				DrinkableItem di = new DrinkableItem(jdi);
				objectDb_synch_lock.put(di.getId(), di);
			}
			
			for(JsonVisibleCreature jvc : jws.getVisibleCreatures()) {
				
				Weapon creatureWeapon = (Weapon) objectDb_synch_lock.get(jvc.getWeaponId());
				ArmourSet creatureArmourSet = new ArmourSet();
				
				if(creatureWeapon == null) {
					log.err("Could not find weapon id: "+jvc.getWeaponId(), lc);
				}
				
				for(long armourId : jvc.getArmourIds()) {
					Armour a = (Armour) objectDb_synch_lock.get(armourId);
					if(a != null) {
						creatureArmourSet.put(a);
					} else {
						log.err("Could not find armour id: "+armourId, lc);
					}
				}
				
				if(jvc.isPlayer()) {
					
					PlayerCreature playerCreature = playerDb_sync_lock.get(jvc.getCreatureId());
					
					if(playerCreature == null) {
						playerCreature = new PlayerCreature(jvc, creatureArmourSet, creatureWeapon); 
						playerDb_sync_lock.put(jvc.getCreatureId(), playerCreature);
					} else {
						playerCreature.internalUpdateFromJson(jvc, creatureArmourSet, creatureWeapon);
					}
					
					
					Tile t = localMap.getTile(playerCreature.getPosition());
					if(t != null) {
						t.setLastTickUpdated(update.getGameTicks());
						t.getCreaturesForModification().add(playerCreature);
					}
					
					
				} else {
					
					Monster monster = monsterDb_synch_lock.get(jvc.getCreatureId());
					
					if(monster == null) {
						monster = new Monster(jvc, creatureArmourSet, creatureWeapon);
						monsterDb_synch_lock.put(jvc.getCreatureId(), monster);
					} else {
						monster.internalUpdateFromJson(jvc, creatureArmourSet, creatureWeapon);
					}

					Tile monsterTile = localMap.getTile(monster.getPosition());
					if(monsterTile == null) {
						/* ignore */
					} else {
						monsterTile.setLastTickUpdated(update.getGameTicks());
						monsterTile.getCreaturesForModification().add(monster);
					}
					
				}
				
			}
			
			for(JsonVisibleObject jvo : jws.getVisibleObjects()) {
				
				IObject containedObject = objectDb_synch_lock.get(jvo.getContainedObjectId());
				if(containedObject != null) {
					GroundObject go = new GroundObject(jvo.getObjectId(), containedObject, jvo.getPosition().toPosition());
					Tile t = localMap.getTile(go.getPosition());
					t.getGroundObjectForModification().add(go);
				} else {
					log.severe("Unable to locate contained object in database: contained-object-id: "+jvo.getContainedObjectId(), lc);
				}
			}
			
			if(jws.getTileProperties() != null) {
				
				for(Object o : jws.getTileProperties()) {
					
					Map<String, Object> m = (Map<String, Object>)o;
					
					String type = (String)m.get("type");
					
					if(type.equals(JsonDoorProperty.TYPE)) {
						JsonDoorProperty jdp = new JsonDoorProperty(m);
						
						Tile t = localMap.getTile(jdp.getPosition().getX(), jdp.getPosition().getY());
						t.setLastTickUpdated(update.getGameTicks());
						t.getTilePropertiesForModification().add(new DoorTileProperty(jdp));
					}
				}
			}
			
			// TODO: LOW - Add open door status indicator, because our client terrain does not currently communicate it.
			
			if(selfState_synch_lock == null) {
				PlayerCreature playerCreature = playerDb_sync_lock.get(update.getSelfState().getPlayerId());
				
				selfState_synch_lock = new SelfState(playerCreature);
			} else {
//				selfState_synch_lock.internalUpdateFromJson(jss);				
			}
			
			// Update inventory
			{
				
				List<OwnableObject> newInventory = new ArrayList<>();
				update.getSelfState().getInventory().stream().forEach( e -> {
					
					IObject containedObject = objectDb_synch_lock.get(e.getContainedObject());
					if(containedObject == null) { log.severe("Could not find contained object in database", null); return; }
 					OwnableObject result = new OwnableObject(containedObject, e.getId());
					newInventory.add(result);
				});
				
				PlayerCreature player = (PlayerCreature)(selfState_synch_lock.getPlayer());
				
				player.getInventory().stream().forEach( e -> {
					player.removeFromInventory(e);
				});
				
				if(player.getInventory().size() != 0) {
					log.severe("Player inventory was not empty when it should be", lc);
				}
				
				newInventory.stream().forEach( e -> {
					player.addToInventory(e);
				});
				
				if(newInventory.stream().anyMatch( e -> !player.getInventory().contains(e))) {
					log.severe("Could not locate one or more items in the updated player inventory.", lc);
				}
				
			}
			
			// Process received world events
			{
				List<IEvent> events = new ArrayList<>();
				List<Object> jsonEvents = update.getWorldState().getEvents();
				for(Object e : jsonEvents) {
					Map<?, ?> map = (Map<?, ?>)e;
					String type = (String) map.get("type");
					
					if(type == null) { 
						log.severe("Event type was null.", lc);
						continue;
					}
					
					if(type.equals(JsonStepActionEvent.TYPE)) {
						JsonStepActionEvent jsae = new JsonStepActionEvent(map);
						
						ICreature creature = getCreatureFromDb(jsae.getCreatureId());
						
						if(creature != null) {
							StepActionEvent newEvent = new StepActionEvent(creature, jsae.getFrom().toPosition(), jsae.getTo().toPosition(), jsae.getFrame(), jsae.getEventId());
							events.add(newEvent);					
						} else {
							log.severe("Creature could not be found in step action event", lc);
						}
						
					} else if(type.equals(JsonCombatActionEvent.TYPE)) {
						JsonCombatActionEvent jcae = new JsonCombatActionEvent(map);
	
						ICreature attacker = getCreatureFromDb(jcae.getAttackerId());
						ICreature defender = getCreatureFromDb(jcae.getDefenderId());
						
						if(attacker != null && defender != null) {
							CombatActionEvent newEvent = new CombatActionEvent(attacker, defender, jcae.isHit(), jcae.getDamageDone(), jcae.getFrame(), jcae.getId());
							events.add(newEvent);
						} else {
							log.severe("["+update.getGameTicks()+"] Attacker and defender could not be found in combat action event: "+attacker+" ("+(attacker != null ? attacker.getId() : "n/a")+") "+defender+" ("+(defender != null ? defender.getId() : "n/a")+")", lc);
						}
						
					} else if(type.equals(JsonDrinkItemActionEvent.TYPE)) {
						JsonDrinkItemActionEvent jdiae = new JsonDrinkItemActionEvent(map);
						
						ICreature creature = getCreatureFromDb(jdiae.getPlayerId());
						
						IObject drinkableObject = objectDb_synch_lock.get(jdiae.getObjectId());
						
						if(creature != null && drinkableObject != null && drinkableObject.getObjectType() == ObjectType.ITEM && drinkableObject instanceof DrinkableItem) {
							DrinkItemActionEvent diae = new DrinkItemActionEvent(creature, (DrinkableItem)drinkableObject, jdiae.getFrame(), jdiae.getId());
							events.add(diae);
							
						} else { 
							log.severe("Unexpected drinkable object type in event", lc);
						}
					} else if(type.equals(JsonEquipActionEvent.TYPE)) {
						JsonEquipActionEvent jeae = new JsonEquipActionEvent(map);
						
						ICreature creature = getCreatureFromDb(jeae.getCreatureId());
						
						IObject equippedObject = objectDb_synch_lock.get(jeae.getObjectId());
						
						if(creature != null && equippedObject != null) {
							EquipActionEvent eae = new EquipActionEvent(equippedObject, creature, jeae.getFrame(), jeae.getId());
							events.add(eae);
						} else {
							log.severe("Could not locate all elements of equip object", lc);
						}
					} else if(type.equals(JsonMoveInventoryItemActionEvent.TYPE)) {
						JsonMoveInventoryItemActionEvent jmiiae = new JsonMoveInventoryItemActionEvent(map);
						ICreature creature = getCreatureFromDb(jmiiae.getCreatureId()); 
						IObject obj = objectDb_synch_lock.get(jmiiae.getObjectId());
						
						if(creature != null && obj != null) {
							MoveInventoryItemActionEvent miiae = new MoveInventoryItemActionEvent(creature, obj, jmiiae.isDropItem(), jmiiae.getFrame(), jmiiae.getId());
							events.add(miiae);
						} else {
							log.severe("Could not locate all elements of move inventory event: "+jmiiae.getCreatureId()+" "+creature+", "+jmiiae.getObjectId()+" "+obj, lc);
						}
						
					} else {
						log.severe("Unrecognized event type: "+type, lc);
					}
					
				}
				
				event_log_synch_lock.internalAddEvents(events);
				event_log_synch_lock.internalClearOldEvents(update.getGameTicks());
			}
			
			
			nextFrame_synch_lock++;
			
			map_synch_lock = localMap;
			
			// If the next frame is already in the buffer, then process it now
			JsonFrameUpdate nextFrame = unprocessedFrames_synch_lock.get(nextFrame_synch_lock);
			if(nextFrame != null) {
				unprocessedFrames_synch_lock.remove(nextFrame_synch_lock);
				receiveFrameUpdate(nextFrame);
			} else {
				// Only call the AI if there are no more frames to process
				mapToCallAIWith = map_synch_lock;
			}
			
		} // end synchronized on lock
		
		if(mapToCallAIWith != null) {
			
			if(this.worldState == null) {
				
				this.clientMap = new ClientMap(mapToCallAIWith);
				this.worldState = new WorldState(clientMap);
				
			} else {
				this.clientMap.updateMap(mapToCallAIWith);
			}
			
			this.worldState.setWorldHeight(jws.getWorldHeight());
			this.worldState.setWorldWidth(jws.getWorldWidth());
			this.worldState.setViewXPos(jws.getClientViewPosX());
			this.worldState.setViewYPos(jws.getClientViewPosY());
			this.worldState.setCurrentGameTick(update.getGameTicks()); // Yes, this is the correct game tick, even in the scenario where we have processed multiple frames.
			this.worldState.setViewWidth(jws.getClientViewWidth());
			this.worldState.setViewHeight(jws.getClientViewHeight());
			this.worldState.setRemainingSecondsInRound(jws.getRoundSecsLeft());
			
			RCRuntime.GAME_TICKS.set(update.getGameTicks());
			
			clientState.getRemoteClient().internalReceiveStateUpdate(selfState_synch_lock, this.worldState, event_log_synch_lock);
			
			final IMap fmapToCallAIWith = mapToCallAIWith;

			WorldStateListeners.getInstance().getListeners().forEach( e -> {
				e.worldStateUpdated(-1, -1, jws.getClientViewPosX(), jws.getClientViewPosY(), jws.getClientViewWidth(), jws.getClientViewHeight(), fmapToCallAIWith, update.getGameTicks());
			});
			
		}
		
	}
	
	private ICreature getCreatureFromDb(long id) {
		ICreature creature = playerDb_sync_lock.get(id);
		if(creature == null) {
			creature = monsterDb_synch_lock.get(id);
		}
		return creature;
	}

	
	private static ITerrain createTerrain(boolean passable, List<Integer> ints, LogContext lc) {
		TileType tileType;
//		TerrainType tt;
		
		if(ints.size() == 1) {
			tileType = new TileType(ints.get(0));
//			tt = TerrainType.NONE
//			tt = TerrainTypeMap.getInstance().getTypeByValue(ints.get(1));
			
		} else if(ints.size() == 2) {
			
			tileType = new TileType(ints.get(0), ints.get(1));
//			tt = TerrainTypeMap.getInstance().getTypeByValue(ints.get(1));			
			
		} else {
			log.severe("Invalid number of terrain values received", lc);
			return null;
		}
		
		ITerrain terrain;
		if(passable) {
			terrain = new ImmutablePassableTerrain(tileType);
		} else {
			terrain = new ImmutableImpassableTerrain(tileType);
		}
		
		
//		ITerrain terrain;
//		if(tt == TerrainType.DOOR) {
//			terrain = new DoorTerrain(tileType, false);
//			
//		} else if(tt == TerrainType.GENERIC_IMPASSABLE) {
//			terrain = new ImmutableImpassableTerrain(tileType);
//			
//		} else if(tt == TerrainType.GENERIC_PASSABLE) {
//			terrain = new ImmutableImpassableTerrain(tileType);
//			
//		} else if(tt == TerrainType.WALL) {
//			terrain = null;
//		} else {
//			log.severe("Invalid terrain type: "+tt, lc);
//			return null;
//		}
		
		return terrain;
		
	}
	
	public void receiveBrowserUIUpdate(JsonUpdateBrowserUI o) {
		
		WorldStateListeners.getInstance().getListeners().forEach( e -> {
			e.receiveBrowserUIUpdate(o);
		});
		
	}
	
	/** 
	 * The containing class, ClientWorldState, is principally responsible for maintaining the world state required to implement 
	 * the IClient/RemoteClient API that is used by player AI code. However, in addition, other parts of 
	 * the code may also be want to be informed when the world state is updated. 
	 * 
	 * This class ClientWorldStateListener, allows other sections of the client code to register themselves as listeners,
	 * and to be inform when the world state is updated, a browser UI update is received, or the round completed.
	 *  
	 * Currently this interface is only implemented by LibertyWSClientWorldStateListener */
	public static interface ClientWorldStateListener {
		
		/** Inform the implementing class that the world state has been updated. */
		public void worldStateUpdated(int currClientWorldX, int currClientWorldY, int newWorldPosX, int newWorldPosY, int newWidth, int newHeight, IMap map, long ticks);
		
		/** Inform the implementing class that the browser UI has been updated with score/leaderboard stats/etc */
		public void receiveBrowserUIUpdate(JsonUpdateBrowserUI u);

		/** Inform the implementing class that the round has ended, and when the next round begins */
		public void roundComplete(int nextRoundInXSeconds);
		
		/**  Ask the implementing class if the session they are using is still open. */
		public boolean isClientOpen();
	}


	public void dispose() {
		
		synchronized(lock) {
			if(disposed) {
				return;
			}
			disposed = true;
			
			selfState_synch_lock = null;
			
			unprocessedFrames_synch_lock.clear();
			
			if(map_synch_lock != null) {
//				map_synch_lock.dispose();
				map_synch_lock = null;
			}
			
			playerDb_sync_lock.clear();
			monsterDb_synch_lock.clear();
			objectDb_synch_lock.clear();
			event_log_synch_lock.dispose();
		}
		
	}
}
