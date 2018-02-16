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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.roguecloud.GameEngine.GameContext;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.creatures.PlayerCreature;
import com.roguecloud.events.CombatActionEvent;
import com.roguecloud.events.DrinkItemActionEvent;
import com.roguecloud.events.EquipActionEvent;
import com.roguecloud.events.IEvent;
import com.roguecloud.events.IEvent.EventType;
import com.roguecloud.events.IMutableEvent;
import com.roguecloud.events.MoveInventoryItemActionEvent;
import com.roguecloud.events.StepActionEvent;
import com.roguecloud.items.Armour;
import com.roguecloud.items.DrinkableItem;
import com.roguecloud.items.IGroundObject;
import com.roguecloud.items.IObject;
import com.roguecloud.items.IObject.ObjectType;
import com.roguecloud.items.Weapon;
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.JsonDoorProperty;
import com.roguecloud.json.JsonPosition;
import com.roguecloud.json.JsonVisibleCreature;
import com.roguecloud.json.JsonVisibleObject;
import com.roguecloud.json.JsonWorldState;
import com.roguecloud.json.JsonWorldState.JsonViewFrame;
import com.roguecloud.map.DoorTileProperty;
import com.roguecloud.map.IMap;
import com.roguecloud.map.ITileProperty;
import com.roguecloud.map.ITileProperty.TilePropertyType;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;

public class WorldStateJsonGenerator {
	
	private static final Logger log = Logger.getInstance();
	
	public static JsonWorldState generateJsonWorldState(int clientWidth, int clientHeight, EngineWebsocketState ews, IMap map, 
			List<Position> changedTiles, GameContext gc, LogContext lc, RoundScope roundScope, boolean sendFullFrame) {
		
		RCRuntime.assertGameThread();
		
		ICreature creature = ews.getPlayerCreature();
		
		Position newClientPos = creature.getPosition();
		{
			int playerX = newClientPos.getX()-(clientWidth/2);
			int playerY = newClientPos.getY()-(clientHeight/2);
			newClientPos = GameEngine.normalizePositionBox(playerX, playerY, clientWidth, clientHeight, map);
		}
		
		JsonWorldState jws = new JsonWorldState();
		jws.setClientViewPosX(newClientPos.getX());
		jws.setClientViewPosY(newClientPos.getY());
		jws.setClientViewWidth(clientWidth);
		jws.setClientViewHeight(clientHeight);
		jws.setWorldWidth(map.getXSize());
		jws.setWorldHeight(map.getYSize());
		
		if(roundScope != null) {
			jws.setRoundSecsLeft((int)TimeUnit.SECONDS.convert(roundScope.getCurrentRoundEndInNanos()-System.nanoTime(), TimeUnit.NANOSECONDS));
		}
				
		// This list may contain not only events from the current frame, but also events from (potentially many) previous frames as well.
		List<IMutableEvent> eventsToSend = new ArrayList<>();
		{
			
			List<IEvent> evList;
			
			if(!sendFullFrame) {
				evList = gc.eventsPreviousFrames.getList(gc.getTicks()-1);
			} else {
				evList = gc.eventsPreviousFrames.getAll();
			}
			
			for(IEvent e : evList) {
				
				// Is the event visible to the creature?
				if(Position.containedInBox(e.getWorldLocation(), 
						jws.getClientViewPosX(), jws.getClientViewPosY(), 
						jws.getClientViewWidth(), jws.getClientViewHeight())) {
					
					eventsToSend.add((IMutableEvent)e);
				}
			}	
		}

		// Send visible things
		{
			HashMap<Long /*obj id*/, Boolean> objectsToSend = new HashMap<>();
			List<Weapon> weaponsToSend = new ArrayList<>();
			List<Armour> armourToSend = new ArrayList<>();
			List<DrinkableItem> drinkablesToSend = new ArrayList<>();
			
			List<JsonVisibleCreature> visibleCreatures = new ArrayList<JsonVisibleCreature>();
			jws.setVisibleCreatures(visibleCreatures);
			
			Map<Long /* creatureId*/, ICreature> creaturesToSend = new HashMap<>();
			
			for(int y = jws.getClientViewPosY(); y < jws.getClientViewPosY()+jws.getClientViewHeight(); y++) {
				for(int x = jws.getClientViewPosX(); x < jws.getClientViewPosX()+jws.getClientViewWidth(); x++) {
					
					Tile t = map.getTile(x, y);
					for(ICreature otherCreature : t.getCreatures()) {
//						if(otherCreature.getId() == creature.getId()) { continue; }
						
						creaturesToSend.put(otherCreature.getId(), otherCreature);
					}
					
					for(ITileProperty tp : t.getTileProperties()) {
						if(tp.getType() == TilePropertyType.DOOR) {
							DoorTileProperty dtp = (DoorTileProperty)tp;
							
							JsonDoorProperty json = new JsonDoorProperty();
							json.setOpen(dtp.isOpen());
							json.setPosition(new JsonPosition( x, y ));
							
							
							jws.getTileProperties().add(json);
							
						} else {
							log.severe("Unrecognized tile property - "+tp.getType(), lc);
						}
					}
					
					for(IGroundObject groundObject : t.getGroundObjects()) {
						JsonVisibleObject jvo = new JsonVisibleObject();
					
						jvo.setObjectId(groundObject.getId());
						jvo.setPosition(groundObject.getPosition().toJson());
						jvo.setContainedObjectId(groundObject.get().getId());
						
						addObjectToTypeList(groundObject.get(), weaponsToSend, armourToSend, drinkablesToSend, lc);
						
						jws.getVisibleObjects().add(jvo);
					}
					
				}
			}
			
			
			creature.getArmour().getAll().stream().forEach( e -> {
				armourToSend.add(e);
			});
			
			weaponsToSend.add(creature.getWeapon());
			
			creature.getInventory().stream().forEach( e -> {
				IObject o = e.getContainedObject();
				addObjectToTypeList(o, weaponsToSend, armourToSend, drinkablesToSend, lc);
			});
			
			// Make sure the user has seen any objects or creatures that relate to events that occurred in the previous frame.
			{
				List<ICreature> creaturesInEvents = new ArrayList<ICreature>();
				for(IMutableEvent e : eventsToSend) {
					
					// Only send objects or creatures that are in the most recent frame.
//					if(e.getFrame() != gc.ticks-1) { continue; }
					
					if(e.getActionType() == EventType.DRINK) {
						DrinkItemActionEvent diae = (DrinkItemActionEvent)e;
						creaturesInEvents.add(diae.getCreature());
						addObjectToTypeList(diae.getDrinkableItem(), weaponsToSend, armourToSend, drinkablesToSend, lc);
					} else if(e.getActionType() == EventType.COMBAT) {
						CombatActionEvent cae = (CombatActionEvent)e;
						creaturesInEvents.add(cae.getAttacker());
						creaturesInEvents.add(cae.getDefender());
						
					} else if(e.getActionType() == EventType.STEP) {
						StepActionEvent sae = (StepActionEvent)e;
						creaturesInEvents.add(sae.getCreature());
						
					} else if(e.getActionType() ==  EventType.EQUIP) {
						EquipActionEvent eae = (EquipActionEvent)e;
						addObjectToTypeList(eae.getEquippedObject(), weaponsToSend, armourToSend, drinkablesToSend, lc);
						creaturesInEvents.add(eae.getCreature());
						
					} else if(e.getActionType() == EventType.MOVE_INVENTORY_ITEM) {
						MoveInventoryItemActionEvent miiae = (MoveInventoryItemActionEvent)e;
						addObjectToTypeList(miiae.getObject(), weaponsToSend, armourToSend, drinkablesToSend, lc);
						creaturesInEvents.add(miiae.getCreature());
					} else {
						log.severe("Unexpected event type: "+e.getActionType(), lc);
					}					
				}
				
				creaturesInEvents.stream().filter(e -> e != null).forEach( e -> {
					creaturesToSend.put(e.getId(), e);
				});
				
			}
			
			for(ICreature otherCreature : creaturesToSend.values()) {
				JsonVisibleCreature jvc = convertCreatureToJson(otherCreature, weaponsToSend, armourToSend);
				visibleCreatures.add(jvc);
			}
			
			for(Weapon w : weaponsToSend) {
				if(!ews.isObjectSeenByPlayer(w.getId()) && !objectsToSend.containsKey(w.getId())) {
					jws.getWeapons().add(w.toJson());
					objectsToSend.put(w.getId(), true);
					ews.putObjectSeenByPlayer(w.getId());
				}
			}
			
			for(Armour a : armourToSend) {
				if(!ews.isObjectSeenByPlayer(a.getId()) && !objectsToSend.containsKey(a.getId())) {
					jws.getArmours().add(a.toJson());
					objectsToSend.put(a.getId(), true);
					ews.putObjectSeenByPlayer(a.getId());
				}
			}
			
			for(DrinkableItem di : drinkablesToSend) {
				if(!ews.isObjectSeenByPlayer(di.getId()) && !objectsToSend.containsKey(di.getId())) {
					jws.getDrinkables().add(di.toJson());
					objectsToSend.put(di.getId(), true);
					ews.putObjectSeenByPlayer(di.getId());					
				}
			}
		
		} // end send visible things

		for(IMutableEvent ev : eventsToSend) {
			JsonAbstractTypedMessage json = ev.toJson();
			jws.getEvents().add(json);
		}
		
		List<JsonViewFrame> frameList = jws.getFrames();
		
		boolean fullThingSent = false;
		
		if(newClientPos.getX() != ews.getCurrClientWorldX() || newClientPos.getY() != ews.getCurrClientWorldY()/* || newWidth != ssd.getCurrWidth() || newHeight != ssd.getCurrHeight()*/) {
		
			final int deltaX = newClientPos.getX() - ews.getCurrClientWorldX(); // > 0 if the view has shifted right
			final int deltaY = newClientPos.getY() - ews.getCurrClientWorldY(); // > 0 if the view has shifted down

//			log.info("client position shift: "+deltaX+" "+deltaY+" on frame "+gameTicks, null);
			
			if(sendFullFrame || ews.getCurrClientWorldX() == -1 || ews.getCurrClientWorldY() == -1 || (Math.abs(deltaX) > 0 && Math.abs(deltaY) > 0 ) ) {
				
				// Send the whole thing
				JsonViewFrame jvf = new JsonViewFrame();
				jvf.setX(0);
				jvf.setY(0);
				jvf.setW(jws.getClientViewWidth());
				jvf.setH(jws.getClientViewHeight());
						
				for(int y = jws.getClientViewPosY(); y < jws.getClientViewPosY()+jws.getClientViewHeight(); y++) {
					for(int x = jws.getClientViewPosX(); x < jws.getClientViewPosX()+jws.getClientViewWidth(); x++) {
						
						Tile t = map.getTile(x, y);
						jvf.getData().add(convertSingle(t));
						
					}
				}
				
				frameList.add(jvf);
				
				fullThingSent = true;
				
			} else {
				// Send only the new bits
				if(deltaX > 0) {
					
					JsonViewFrame jvf = new JsonViewFrame();
					jvf.setX(jws.getClientViewWidth()-deltaX /*newWorldPosX*/);
					jvf.setY(0 /*newWorldPosY*/);
					jvf.setW(deltaX);
					jvf.setH(jws.getClientViewHeight());

					for(int y = jws.getClientViewPosY(); y < jws.getClientViewPosY()+jws.getClientViewHeight(); y++) {
						for(int x = jws.getClientViewPosX()+jws.getClientViewWidth()-deltaX; x < jws.getClientViewPosX()+jws.getClientViewWidth(); x++) {
							
							Tile t = map.getTile(x, y);
							jvf.getData().add(convertSingle(t));
							
						}
					}
					frameList.add(jvf);						
					
				} else if(deltaX < 0) {
					
					JsonViewFrame jvf = new JsonViewFrame();
					jvf.setX(0);
					jvf.setY(0);
					jvf.setW(Math.abs(deltaX));
					jvf.setH(jws.getClientViewHeight());

//					JsonArrayBuilder data = Json.createArrayBuilder();
		
					for(int y = jws.getClientViewPosY(); y < jws.getClientViewPosY()+jws.getClientViewHeight(); y++) {
						for(int x = jws.getClientViewPosX(); x < jws.getClientViewPosX()+Math.abs(deltaX); x++) {
							
							Tile t = map.getTile(x, y);		
							jvf.getData().add(convertSingle(t));
							
						}
					}
					
					frameList.add(jvf);
					
				} else if(deltaY > 0) {

					JsonViewFrame jvf = new JsonViewFrame();
					jvf.setX(0);
					jvf.setY(jws.getClientViewHeight()-deltaY);
					jvf.setW(jws.getClientViewWidth());
					jvf.setH(deltaY);
		
					for(int y = jws.getClientViewPosY()+jws.getClientViewHeight()-deltaY; y < jws.getClientViewPosY()+jws.getClientViewHeight(); y++) {
						for(int x = jws.getClientViewPosX(); x < jws.getClientViewPosX()+jws.getClientViewWidth(); x++) {
							
							Tile t = map.getTile(x, y);
							jvf.getData().add(convertSingle(t));
							
						}
					}
					frameList.add(jvf);	
					
				} else if(deltaY < 0 ) {
					
					JsonViewFrame jvf = new JsonViewFrame();
					jvf.setX(0);
					jvf.setY(0);
					jvf.setW(jws.getClientViewWidth());
					jvf.setH(Math.abs(deltaY));

					
					for(int y = jws.getClientViewPosY(); y < jws.getClientViewPosY()+Math.abs(deltaY); y++) {
						for(int x = jws.getClientViewPosX(); x < jws.getClientViewPosX()+jws.getClientViewWidth(); x++) {
							
							Tile t = map.getTile(x, y);		
							jvf.getData().add(convertSingle(t));
							
						}
					}
					
					frameList.add(jvf);
					
				} else {
					log.severe("Unexpected delta condition when generating json to send to client: "+deltaX+" "+deltaY, lc);
				}
			}
		}
		
		if(!fullThingSent) {
			
			for(Position p : changedTiles) {
			
				if(!Position.containedInBox(p, jws.getClientViewPosX(), jws.getClientViewPosY(), jws.getClientViewWidth(), jws.getClientViewHeight())) { continue; }
				
				JsonViewFrame jvf = new JsonViewFrame();
				jvf.setX(p.getX()-jws.getClientViewPosX());
				jvf.setY(p.getY()-jws.getClientViewPosY());
				jvf.setW(1);
				jvf.setH(1);
				
				Tile t = map.getTile(p);
				jvf.getData().add(convertSingle(t));
				
				frameList.add(jvf);

			}
		}

		return jws;
	}


	private static void addObjectToTypeList(IObject o, List<Weapon> weaponsToSend, List<Armour> armourToSend, List<DrinkableItem> drinkablesToSend,  LogContext lc) {
		if(o.getObjectType() == ObjectType.ARMOUR) {
			armourToSend.add((Armour)o);
		} else if(o.getObjectType() == ObjectType.WEAPON) {
			weaponsToSend.add((Weapon)o);
		} else if(o.getObjectType() == ObjectType.ITEM) {
			if(o instanceof DrinkableItem) {
				drinkablesToSend.add((DrinkableItem)o);
			} else {
				log.severe("Other item types not yet supported."+o.getObjectType().name(), lc );	
			}
			
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ArrayList<Object> convertSingle(Tile t) {
		
		TileType[] ttArr = t.getTileTypeLayers();
		
		ArrayList outer = new ArrayList();
//		// [
		boolean passable = t.isPresentlyPassable();
//		if(!passable) {
			outer.add(passable ? 1 : 0);
//		}
		
		
		for(int c = 0; c < ttArr.length; c++) {			
//			JsonArrayBuilder tileLayer = Json.createArrayBuilder();
			// [
			
			// For each tile layer
			TileType tt = ttArr[c];
			
			if(tt.getRotation() != 0) {
				// [ number, rotation], 
				ArrayList al = new ArrayList();
				al.add(tt.getNumber());
				al.add(tt.getRotation());
				outer.add(al);
			} else{
				// [ number],
				ArrayList al = new ArrayList();
				al.add(tt.getNumber());
				outer.add(al);
			}
			
			// ]
//			outer = outer.add(tileLayer);
		}
		
		// ]
		return outer;
		
	}

	private static JsonVisibleCreature convertCreatureToJson(ICreature otherCreature, List<Weapon> weaponsToSend, List<Armour> armourToSend) {
		JsonVisibleCreature jvc = new JsonVisibleCreature();
		
		jvc.setName(otherCreature.getName());
		jvc.setCreatureId(otherCreature.getId());
		jvc.setCurrHp(otherCreature.getHp());
		jvc.setLevel(otherCreature.getLevel());
		jvc.setMaxHp(otherCreature.getMaxHp());
		jvc.setPosition(otherCreature.getPosition().toJson());
		jvc.setPlayer(otherCreature instanceof PlayerCreature);
		jvc.setTileTypeNumber(otherCreature.getTileType().getNumber());

		if(otherCreature.getWeapon() != null) {
			Weapon w = otherCreature.getWeapon();
			weaponsToSend.add(w);
			jvc.setWeaponId(w.getId());
		}
		
		for(Armour a : otherCreature.getArmour().getAll()) {						
			armourToSend.add(a);
			jvc.getArmourIds().add(a.getId());
			
		}
		
		otherCreature.getActiveEffects().stream().forEach( e -> {
			jvc.getEffects().add(e.toJson());
		});

		return jvc;
	}


}
