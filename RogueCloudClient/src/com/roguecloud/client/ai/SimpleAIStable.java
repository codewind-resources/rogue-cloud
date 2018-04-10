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

package com.roguecloud.client.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.roguecloud.ActionResponseFuture;
import com.roguecloud.Position;
import com.roguecloud.actions.CombatAction;
import com.roguecloud.actions.DrinkItemAction;
import com.roguecloud.actions.EquipAction;
import com.roguecloud.actions.IAction;
import com.roguecloud.actions.IActionResponse;
import com.roguecloud.actions.MoveInventoryItemAction;
import com.roguecloud.actions.MoveInventoryItemAction.Type;
import com.roguecloud.actions.MoveInventoryItemActionResponse;
import com.roguecloud.actions.NullAction;
import com.roguecloud.actions.NullActionResponse;
import com.roguecloud.actions.StepAction;
import com.roguecloud.client.IEventLog;
import com.roguecloud.client.RemoteClient;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.events.CombatActionEvent;
import com.roguecloud.events.IEvent.EventType;
import com.roguecloud.items.Armour;
import com.roguecloud.items.DrinkableItem;
import com.roguecloud.items.IGroundObject;
import com.roguecloud.items.IObject;
import com.roguecloud.items.IObject.ObjectType;
import com.roguecloud.items.OwnableObject;
import com.roguecloud.items.Weapon;
import com.roguecloud.map.IMap;
import com.roguecloud.map.Tile;
import com.roguecloud.utils.AIUtils;
import com.roguecloud.utils.AStarSearch;
import com.roguecloud.utils.FastPathSearch;

/** 
 * Each turn, the simple agent implementation code asks your implementation code the following, to determine the next action to perform:
 * 
 * ```
 * 1) If someone is attacking us (and the attack is unprovoked), ask if we should attack back.
 * 	- If yes, attack back, then goto 1 (if no, goto 2)
 * 2) Do you want me to drink a potion?
 * 	- If yes, drink the potion then goto 1 (if no, goto 2)
 * 3) Do you want me to attack any of the creatures around you? 
 *  	- If yes, walk to it and attack, then goto 1 (If no, goto 4)
 * 4) Do want me to pick up any of the items around you?
 *  	 - If yes, walk to it and pick it up, then goto 1 (If no, goto 5)
 *  	 	- If picked up item, ask if it should be equipped 
 *  	 		- If yes, equip it. If no, goto 1.
 * 5) Where do you want me to go?
 * 	- If the user specified a position, then walk to it, then goto 1 (otherwise goto 1)
 * 
 * Each of the questions above correspond to methods below. More information on improving your code from the default behaviours
 * can be found on the Github project pages.
 * ```
*/
@SuppressWarnings("unused")
public class SimpleAIStable extends RemoteClient {
	
	/** 
	 * This method is given the list of ALL items on the ground in your current view, and is asked, do you want to pick any of these up?
	 * If you see an item in the list that you want to pick up, return the IGroundObject that contains it!
	 **/
	public IGroundObject shouldIPickUpItem(List<IGroundObject> allVisibleGroundObjects) {

		SelfState selfState = getSelfState();
		WorldState worldState = getWorldState();

		// Look at all the objects the agent can see, and decide which, if any, they should go and pick up.
		// Be careful, some objects might be guarded by monsters! 
		// You can see monsters by calling AIUtils.findCreaturesInRange(...).

		
		// Default behaviour: pick up the first thing I see...
		for(IGroundObject visibleGroundObjectContainer : allVisibleGroundObjects) {
			IObject objectOnGround = visibleGroundObjectContainer.get();
			
			if(objectOnGround.getObjectType() == ObjectType.ARMOUR) {
				Armour a = (Armour)objectOnGround;
				
				return visibleGroundObjectContainer;
				
			} else if(objectOnGround.getObjectType() == ObjectType.WEAPON) {
				Weapon w = (Weapon)objectOnGround;
				
				return visibleGroundObjectContainer;
				
			} else if(objectOnGround.getObjectType() == ObjectType.ITEM) {
				DrinkableItem i = (DrinkableItem)objectOnGround;
				
				return visibleGroundObjectContainer;
			}
			
		}
		
		return null;
	}
	
	/** While your character is wandering around the world, it will see other monsters, which it may optionally attack.
	 *
	 * This method is called each tick with a list of all monsters currently on screen. If you wish to 
	 * attack one of them, return the creature object you wish to attack. 
	 **/
	public ICreature shouldIAttackCreature(List<ICreature> visibleMonsters) {
		
		SelfState selfState = getSelfState();
		WorldState worldState = getWorldState();
		
		// Default behaviour: Attack the first creature that I see.
		for(ICreature c : visibleMonsters) {
			
			int creatureLevel = c.getLevel();
			
			return c;
			
			// Uncomment me to attack a creature! Might want to check their level first!
			// return c;
		}
		
		return null;
		
	}
	

	/**
	 * When your character has nothing else it do it (no items to pick up, or creatures to attack), it will call
	 * this method. The coordinate on the map you pick is the coordinate that the code will move to.
	 */
	public Position whereShouldIGo() {

		SelfState selfState = getSelfState();
		WorldState worldState = getWorldState();
		
		IMap whatWeHaveSeenMap = worldState.getMap();
		
		int x1;
		int y1;
		int x2;
		int y2;

		boolean randomPositionInView = false; 
		if(randomPositionInView) {
			x1 = worldState.getViewXPos();
			y1 = worldState.getViewYPos();
			x2 = worldState.getViewXPos() + worldState.getViewWidth()-1;
			y2 = worldState.getViewYPos() + worldState.getViewHeight()-1;			
		} else {
			x1 = 0;
			y1 = 0;
			x2 = worldState.getWorldWidth();
			y2 = worldState.getWorldHeight();
		}
		
		// Default behaviour: Pick a random spot in the world and go there.
		Position p  = AIUtils.findRandomPositionOnMap(x1, y1, x2, y2, !randomPositionInView, whatWeHaveSeenMap);
		
		System.out.println("Going to "+p);
		if(p != null) {
			return p;
		}
		
		return null;
		
	}

	/** Each turn, we call this method to ask if you character should drink a potion (and which one it should drink). 
	 *
	 * To drink a potion, return the inventory object for the potion (ownable object). 
	 * To drink no potions this turn, return null. 
	 **/
	public OwnableObject shouldIDrinkAPotion() {
		
		ICreature me = getSelfState().getPlayer();
		WorldState worldState = getWorldState();
		List<OwnableObject> ourInventory = selfState.getPlayer().getInventory();

		int percentHealthLeft = (int)(100d * (double)me.getHp() / (double)me.getMaxHp()); 

		// Default behaviour: if our health is less than 50, then drink the first potion 
		// in our inventory (but it might not be helpful in this situation!) 

		if(percentHealthLeft < 50) {
			
			for(OwnableObject oo : ourInventory) {
				IObject obj = oo.getContainedObject();
				if(obj.getObjectType() == ObjectType.ITEM) {
					DrinkableItem potion = (DrinkableItem)obj;
					return oo;
				}
			}			
		}
		
		// Otherwise drink no potions
		return null;
	}
	
	/** When your character picks up a new item (weapon or armour), they have a choice on whether or not to equip it.
	 * 
	 * Return true if you wish to put on or use this new item, or false otherwise.
	 * 
	 **/
	public boolean shouldIEquipNewItem(IObject newItem) {
		ICreature me = getSelfState().getPlayer();
		
		if(newItem.getObjectType() == ObjectType.ARMOUR) {
			Armour a = (Armour) newItem;			
			
			Armour previouslyEquipped = me.getArmour().get(a.getType());
			if(previouslyEquipped != null) {
				// Put your own logic here... compare what you have equipped with what you just picked up!
			}
			
			// Default behaviour: Always equip everything we pick up
			return true;
			
			
		} else if(newItem.getObjectType() == ObjectType.WEAPON) {
			Weapon w = (Weapon) newItem;
			
			Weapon previouslyEquipped = me.getWeapon();
			if(previouslyEquipped != null) {
				// Put your own logic here... compare what you have equipped with what you just picked up!
			}

			// Default behaviour: Always equip everything we pick up
			return true;

		}
		
		return false;
	}
	
	/** If a creature is attacking us (and we did not initiate the combat through the shouldIAttackCreature method), we 
	 * can choose whether to attack back or to ignore them. 
	 * 
	 * Your attacking an attacking creature back is not always the best course of action, 
	 * as some creatures will stop attacking once you leave their territory.
	 * 
	 * If you wish to attack back, return a creature from the list, otherwise return null.
	 **/
	public ICreature unprovokedAttackShouldIAttackBack(List<ICreature> creaturesAttackingUs) {
		
		ICreature me = getSelfState().getPlayer();
		WorldState worldState = getWorldState();
		
		Collections.shuffle(creaturesAttackingUs);
		
		for(ICreature c : creaturesAttackingUs) {
			
			int monsterLevel = c.getLevel();
			
			// Default behaviour: Attack the first creature in the list, after shuffling the list 
			return c;
			
		}

		return null;
	}
	
	// ------------------------------- ( Agent implementation) ------------------------------------------------------------
	
	public static enum State { WANDERING, GETTING_ITEM, KILLING_MONSTER };
	
	private State currentState = State.WANDERING;
	
	private WanderingStateData wanderingStateDate = null;
	
	private AttackingStateData attackingStateData = null;
	
	private PickUpItemData pickUpItemData = null;
	
	private ActionResponseFuture waitingForActionResponse;
	
	
	@Override
	public void stateUpdate(SelfState selfState, WorldState worldState, IEventLog eventLog) {
	
		ICreature me = selfState.getPlayer();
		
		if(me.isDead()) {
			return;
		}
		
		if(waitingForActionResponse != null) {
			IActionResponse response = waitingForActionResponse.getOrReturnNullIfNoResponse();
			if(response != null) {
				waitingForActionResponse = null;
				
				if(response.actionPerformed()) {
					// Success
					
					if(response instanceof MoveInventoryItemActionResponse) {
						MoveInventoryItemActionResponse miiar = (MoveInventoryItemActionResponse)response;
						if(!miiar.isDropItem()) {
							OwnableObject oo = me.getInventory().stream().filter( e -> e.getId() == miiar.getObjectId()
									&& (e.getContainedObject().getObjectType() == ObjectType.ARMOUR 
									    || e.getContainedObject().getObjectType() == ObjectType.WEAPON)
									).findFirst().orElse(null);
							if(oo != null) {
								boolean equipItem = shouldIEquipNewItem(oo.getContainedObject());
								if(equipItem) {
									waitingForActionResponse  = sendAction(new EquipAction(oo));
									return;
								}
							}
						}
						miiar.getObjectId();
					}
					
				}
				
				// If a wander action fails, then restart from scratch
				if(!response.actionPerformed()) {
					
					if(response instanceof NullActionResponse) {
						/* ignore, this is expected */
					} else if(currentState == State.WANDERING) {
						wanderingStateDate = null;
					} else if(currentState == State.KILLING_MONSTER) {
						if(attackingStateData != null) {
							attackingStateData.ourCurrentRoute = null;
						}
					} else if(currentState == State.GETTING_ITEM) {
						pickUpItemData = null;
						currentState = State.WANDERING;
					}
					
				}
				
			} else {
				// We are still waiting for a previous response
				return;
			}
			
		} 
		
		OwnableObject oo = shouldIDrinkAPotion();
		if(oo != null) {
			waitingForActionResponse = sendAction(new DrinkItemAction(oo));
			return;
		}
		
		if(currentState == State.WANDERING) {
			IAction action = doWandering(selfState, worldState, eventLog);
			waitingForActionResponse = sendAction(action);
			return;
			
		} else if(currentState == State.KILLING_MONSTER) {
			IAction action = doAttacking(selfState, worldState, eventLog);
			waitingForActionResponse = sendAction(action);
			return;			
		} else if(currentState == State.GETTING_ITEM) {
			IAction action = doPickUpItem(selfState, worldState, eventLog);
			waitingForActionResponse = sendAction(action);
			return;
		}
				
	}

	
	private IAction doPickUpItem(SelfState selfState, WorldState worldState, IEventLog eventLog) {

		IMap map = worldState.getMap();
		ICreature me = selfState.getPlayer();
		
		if(pickUpItemData == null) {
			pickUpItemData = new PickUpItemData();
		}
		
		if(pickUpItemData.objectToPickUp == null) {
			pickUpItemData = null;
			currentState = State.WANDERING;
			return NullAction.INSTANCE;
		}
		
		IGroundObject go = pickUpItemData.objectToPickUp;
		
		if(go.getPosition() == null) {
			// If the ground object has already been taken by something else, then give up
			pickUpItemData = null;
			currentState = State.WANDERING;
			return NullAction.INSTANCE;
		} else {
			
			Tile t = map.getTile(go.getPosition());
			if(t == null || !t.getGroundObjects().contains(go)) {
				// If the ground object has already been taken by something else, then give up
				pickUpItemData = null;
				currentState = State.WANDERING;
				return NullAction.INSTANCE;
			}
		}
		
		if(AIUtils.canReach(me.getPosition(), go.getPosition(), map)) {
			currentState = State.WANDERING;
			pickUpItemData = null;
			return new MoveInventoryItemAction(go.getId(), Type.PICK_UP_ITEM );
		}
		
		
		if(pickUpItemData.ourCurrentRoute != null && pickUpItemData.ourCurrentRoute.size() == 0) {
			pickUpItemData.ourCurrentRoute = null;
		}
		
		if(pickUpItemData.ourCurrentRoute == null) {
			// Find a new route to the ground object
			List<Position> routeToDestination = FastPathSearch.doSearchWithAStar(selfState.getPlayer().getPosition(), go.getPosition(), worldState.getMap());

			if(routeToDestination.size() > 1) {
				// Remove the first item, which is our current position
				routeToDestination.remove(0);
				
				pickUpItemData.ourCurrentRoute = routeToDestination;

			}
	
		}
		
		if(pickUpItemData.ourCurrentRoute != null && pickUpItemData.ourCurrentRoute.size() > 0) {
			Position nextMove = pickUpItemData.ourCurrentRoute.remove(0);
			return new StepAction(nextMove);
		}
		
		return NullAction.INSTANCE;	
		
	}
	
	private IAction doAttacking(SelfState selfState, WorldState worldState, IEventLog eventLog) {
		
		IMap map = worldState.getMap();
		ICreature me = selfState.getPlayer();
		
		if(attackingStateData == null) {
			// If the state doesn't exist, create it
			attackingStateData = new AttackingStateData();
		}
		
		if(attackingStateData.creatureToAttack == null) {
			// If we have no creature to attack, then give up and go back to wandering
			attackingStateData = null;
			currentState  = State.WANDERING;
			return NullAction.INSTANCE;
		}
		
		ICreature creatureToAttack = attackingStateData.creatureToAttack;
		
		if(creatureToAttack.isDead()) {
			// If our creature is dead... success!! Go back to wandering.
			attackingStateData = null;
			currentState  = State.WANDERING;
			return NullAction.INSTANCE;
		}
		
		if(creatureToAttack.getPosition() != null) {
			Tile t = map.getTile(creatureToAttack.getPosition());
			if(t == null) {
				currentState = State.WANDERING;
				attackingStateData = null;
				return NullAction.INSTANCE;
			} else if(!t.getCreatures().contains(creatureToAttack)) {
				currentState = State.WANDERING;
				attackingStateData = null;
				return NullAction.INSTANCE;								
			}
		}
		
		if(AIUtils.canReach(selfState.getPlayer().getPosition(), creatureToAttack.getPosition(), map)) {
			// If we can attack the creature from where we are standing, then do it!
			return new CombatAction(creatureToAttack);
		}
		
		if(attackingStateData.ourCurrentRoute != null && attackingStateData.ourCurrentRoute.size() == 0) {
			// If we have a route, but we have completed it, then reset to null.
			attackingStateData.ourCurrentRoute = null;
		}
		
		int distance = me.getPosition().manhattanDistanceBetween(creatureToAttack.getPosition());
		
		
		List<Position> fastPathSearchResult = AStarSearch.findPath(selfState.getPlayer().getPosition(), creatureToAttack.getPosition(), worldState.getMap());
		if(fastPathSearchResult.size() > 1) {
			fastPathSearchResult.remove(0);
			attackingStateData.ourCurrentRoute = fastPathSearchResult;
		
		} else if(attackingStateData.ourCurrentRoute == null || distance < 15) {
			// Find a new route to the creature
			List<Position> routeToDestination = AStarSearch.findPath(selfState.getPlayer().getPosition(), creatureToAttack.getPosition(), worldState.getMap());
			if(routeToDestination.size() > 1) {
				// Remove the first item, which is our current position
				routeToDestination.remove(0);
				
				attackingStateData.ourCurrentRoute = routeToDestination;

			}
			
		}
		
		// We have a valid route, so take the next step on it
		if(attackingStateData.ourCurrentRoute != null && attackingStateData.ourCurrentRoute.size() > 0) {
			Position nextMove = attackingStateData.ourCurrentRoute.remove(0);
			return new StepAction(nextMove);	
		}
		
		
		return NullAction.INSTANCE;
	}
	
	private IAction doWandering(SelfState selfState, WorldState worldState, IEventLog eventLog) {

		ICreature me = selfState.getPlayer();
		IMap map = worldState.getMap();

		// Did any creatures attack us last turn?
		{
			List<ICreature> creaturesAttackingUs = eventLog.getLastTurnSelfEvents(me, worldState).stream()
				.filter( e -> e.getActionType() == EventType.COMBAT && ((CombatActionEvent)e).getDefender() != null &&  ((CombatActionEvent)e).getDefender().equals(me))
				.map( e -> ((CombatActionEvent)e).getAttacker()).collect(Collectors.toList());
			
			if(creaturesAttackingUs.size() > 0) {
				ICreature attackBack = unprovokedAttackShouldIAttackBack(creaturesAttackingUs);
				if(attackBack != null) {
					wanderingStateDate = null;
					attackingStateData = new AttackingStateData();
					attackingStateData.creatureToAttack = attackBack;
					currentState = State.KILLING_MONSTER;
					return NullAction.INSTANCE;
				}
			}
		}
		
		
		// Are there any creatures we might want to attack?
		{
			List<ICreature> creatures = AIUtils.findCreaturesInRange(worldState.getViewWidth(), worldState.getViewHeight(), selfState.getPlayer().getPosition(), worldState.getMap());
			AIUtils.removePlayerCreaturesFromList(creatures);
			AIUtils.sortClosestCreatures(selfState.getPlayer().getPosition(), creatures);
			
			ICreature creatureToAttack = shouldIAttackCreature(creatures);
			if(creatureToAttack != null) {
				pickUpItemData = null;
				wanderingStateDate = null;
				attackingStateData = new AttackingStateData();
				attackingStateData.creatureToAttack = creatureToAttack;
				currentState = State.KILLING_MONSTER;
				
				return NullAction.INSTANCE;
			}
		}
		
		// Are there any items we might want to pickup?
		{
			List<IGroundObject> groundObjects = AIUtils.findAndSortGroundObjectsInRange(worldState.getViewWidth(), worldState.getViewHeight(), me.getPosition(), map);
			
			IGroundObject objectToPickup = shouldIPickUpItem(groundObjects);
			if(objectToPickup != null) {
				wanderingStateDate = null;
				attackingStateData = null;
				pickUpItemData = new PickUpItemData();
				pickUpItemData.objectToPickUp = objectToPickup;
				currentState = State.GETTING_ITEM;
				return NullAction.INSTANCE;
			}
		}
		
		if(wanderingStateDate == null) {
			wanderingStateDate = new WanderingStateData();			
		}
		
		if(wanderingStateDate.ourCurrentRoute.size() == 0) {
			// Our previous route is finished -- so find a new route to go
			wanderingStateDate.ourCurrentRoute = null;
		}
		
		// Find a new route
		if(wanderingStateDate.ourCurrentRoute == null) {
			Position destination = whereShouldIGo();
			if(destination == null) { return NullAction.INSTANCE; }
			
			List<Position> routeToDestination = FastPathSearch.doSearchWithAStar(me.getPosition(), destination, worldState.getMap());
			if(routeToDestination.size() > 1) {
				// Success!
				
				// Remove the first item, which is our current position
				routeToDestination.remove(0);
				
				wanderingStateDate.ourCurrentRoute = routeToDestination;
			}
			
		}
		
		// If we found a route, then take it!
		if(wanderingStateDate.ourCurrentRoute != null) {
			Position nextMove = wanderingStateDate.ourCurrentRoute.remove(0);
			return new StepAction(nextMove);
		}
		
		// No luck, try again next frame!
		return NullAction.INSTANCE;
		
	}
	
	/** When we are in the GETTING_ITEM state, this contains the object we are trying to pick up, and 
	 * the step-by-step route to get there (if it has been calculated) */
	private static class PickUpItemData {
		IGroundObject objectToPickUp;
		List<Position> ourCurrentRoute = new ArrayList<Position>();
	}
	
	
	/** When we are in the KILLING_MONSTER state, this contains the create we want to attack, their last position,
	 * and our step-by-step route to them (if it has been calculated)
	 */
	private static class AttackingStateData {
		ICreature creatureToAttack = null;
		List<Position> ourCurrentRoute = new ArrayList<Position>();
	}

	/** When we are in the WANDERING state, this contains the step-by-step route to our next position */
	private static class WanderingStateData {
		List<Position> ourCurrentRoute = new ArrayList<Position>();
	}
	
}


