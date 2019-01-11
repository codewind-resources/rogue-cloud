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

package com.roguecloud.ai;

import java.util.ArrayList;
import java.util.List;

import com.roguecloud.ActionResponseFuture;
import com.roguecloud.Position;
import com.roguecloud.actions.CombatAction;
import com.roguecloud.actions.IAction;
import com.roguecloud.actions.NullAction;
import com.roguecloud.actions.StepAction;
import com.roguecloud.actions.IAction.ActionType;
import com.roguecloud.client.IEventLog;
import com.roguecloud.client.MonsterClient;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.events.CombatActionEvent;
import com.roguecloud.events.IEvent;
import com.roguecloud.events.IEvent.EventType;
import com.roguecloud.map.Tile;
import com.roguecloud.utils.AIUtils;
import com.roguecloud.utils.FastPathSearch;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.ServerUtil;

/** 
 * This AI either wanders the world, or wanders around a given position (within a given range). 
 * This AI will never initiate an attack, but will attempt to destroy anyone 
 * that attacks it first (the "sleepy bear" strategy). 
 **/
public class WanderingDefendOnlyAI extends MonsterClient {
	
	public static final Logger log = Logger.getInstance();

	public static final boolean LOG = false;
	
	private ActionResponseFuture actionResponse;
	
//	private static final Object lock = new Object();
	
	private enum WanderingState { WANDERING, ATTACKING};
	
	private WanderingState current = WanderingState.WANDERING; 
	
	private WanderingData wanderingData = null;
	private AttackingData attackingData = null;
	
	/** May be null */
	private final Position fixedPoint;
	
	/** May be null */
	private final Integer range;
	
	
	public WanderingDefendOnlyAI(Position fixedPoint, Integer range) {
		this.fixedPoint = fixedPoint;
		this.range = range;
	}


	@Override
	public void stateUpdate(SelfState selfState, WorldState worldState, IEventLog eventLog) {

		boolean lastMoveFailed = false;
		
		if(actionResponse != null) {
			try {
//				log("Waiting for action: "+actionResponse);
				if(actionResponse.isResponseReceived()) {
					if(!actionResponse.getOrWaitForResponse().actionPerformed()) {
						lastMoveFailed = true;
					}
					actionResponse = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(actionResponse == null) {
			IAction action = findNextMove(selfState, worldState, eventLog, lastMoveFailed);
			if(action != null && action.getActionType() != ActionType.NULL) {
				actionResponse = sendAction(action);
			}	
		}
	}
	
	
	private IAction findNextMove(SelfState selfState, WorldState worldState, IEventLog eventLog, boolean lastMoveFailed) {

		if(current == WanderingState.WANDERING) {
			return doWandering(selfState, worldState, eventLog, wanderingData, lastMoveFailed);
		} else if(current == WanderingState.ATTACKING) {
			return doAttacking(selfState, worldState, eventLog, attackingData, lastMoveFailed);
		} else {
			log.severe("Unrecognized state: "+current, null);
		}
		
		return NullAction.INSTANCE;
		
	}
	
	private IAction doAttacking(SelfState selfState, WorldState worldState, IEventLog eventLog, AttackingData data, boolean lastMoveFailed) {
		
		ICreature us = selfState.getPlayer();
		
		ICreature toAttacker = data.creatureToAttack;
		
		if(toAttacker == null || toAttacker.isDead()) {
			// Attack has died, so go back to wandering.
			attackingData = null;
			current = WanderingState.WANDERING;
			return NullAction.INSTANCE;
		}
		
		// If we can attack the target, then do it!
		if(AIUtils.canAttack(us.getPosition(), toAttacker.getPosition(), worldState.getMap(), selfState.getPlayer().getWeapon() )) {
			
			CombatAction ca = new CombatAction(toAttacker);
			log("Attacking: "+toAttacker+" [we are: "+us+"]");
			return ca;
			
		} else {
			
			int distance = us.getPosition().distanceBetween(toAttacker.getPosition());
			
			if(distance > 10) {

				// If we have no path, or our previous path ended
				if(data.nextSteps == null || data.nextSteps.size() == 0) {
					data.nextSteps = null;
					
					List<Position> path = FastPathSearch.doSearchWithAStar(us.getPosition(), toAttacker.getPosition(), ServerUtil.maxAStarSearch(worldState.getMap()), worldState.getMap());
					if(path != null && path.size() > 0) {
						path.remove(0);
						data.nextSteps = path;
					}					
				}
				
				// If we already have a path, then follow it
				if(data.nextSteps != null && data.nextSteps.size() > 0) {
					Position next = data.nextSteps.remove(0);
					return new StepAction(next);
				} 
				
			} else {
				
				List<Position> path = FastPathSearch.doSearchWithAStar(us.getPosition(), toAttacker.getPosition(), ServerUtil.maxAStarSearch(worldState.getMap()), worldState.getMap());
				if(path != null && path.size() > 1) {
					path.remove(0);
					return new StepAction(path.remove(0));
				}
			}
		}
		
		return NullAction.INSTANCE;
		
	}

	private IAction doWandering(SelfState selfState, WorldState worldState, IEventLog eventLog, WanderingData data, boolean lastMoveFailed) {

		// Check if someone attacked us, and attack them back!
		{
			IEvent defenseEvent = eventLog.getLastXTurnsSelfEvents(10, selfState.getPlayer(), worldState)
				.stream()
				.filter(e -> {
					if(e.getActionType() != EventType.COMBAT) { return false; }
					CombatActionEvent cae = (CombatActionEvent)e;
					if(cae.getDefender() != null && cae.getDefender().equals(selfState.getPlayer())) { 
						
						ICreature creature = cae.getAttacker();
						if(!creature.isDead()) {
							return true;
						}
					}
					return false;
							
				}).findAny().orElse(null);
			
			if(defenseEvent != null) {
				current = WanderingState.ATTACKING;
				attackingData = new AttackingData();
				attackingData.creatureToAttack = ((CombatActionEvent)defenseEvent).getAttacker();
				
				return NullAction.INSTANCE;
			}
		}
		
		
		if(data == null) {
			this.wanderingData = new WanderingData();
			data = wanderingData;
		}
		
		// If we have reached the end of nextSteps, then look for a new position.
		if(data.nextSteps != null && data.nextSteps.size() == 0) {
			data.nextSteps = null;
		}

		if(lastMoveFailed) {
			data.nextSteps = null;
		}
		
		if(data.nextSteps == null) {
			int worldHeight = worldState.getWorldHeight();
			int worldWidth = worldState.getWorldWidth();
			
			Position targetPos;
			if(range != null && fixedPoint != null) {
				int x, y;
				x = fixedPoint.getX() + (int)(Math.random() * (range * 2))-range;
				y = fixedPoint.getY() + (int)(Math.random() * (range * 2))-range;
				
				if(x < 0) { x = 0; } 
				if(y < 0) { y = 0; }
				if(x > worldState.getWorldWidth()-2) { x = worldState.getWorldWidth()-2; }
				if(y > worldState.getWorldHeight()-2) { y = worldState.getWorldHeight()-2; }
				
				targetPos = new Position(x, y);
				
			} else {
				int x, y;
				x = (int)(Math.random() * worldWidth);
				y = (int)(Math.random() * worldHeight);
				targetPos = new Position(x, y);				
			}
			
			
			Tile t = worldState.getMap().getTile(targetPos);
			if(t != null && t.isPresentlyPassable()) {
		
				List<Position> l = FastPathSearch.doSearch(selfState.getPlayer().getPosition(), targetPos, worldState.getMap());
				if(l != null && l.size() > 0) {
					// Remove the first element, which is the current square
					l.remove(0);
					data.nextSteps = l;
				}
				
			}
			
		}
		
		// We have a plan, do continue acting on it
		if(data.nextSteps != null && data.nextSteps.size() > 0) {
			Position nextMove = data.nextSteps.remove(0);
			return new StepAction(nextMove);
		}		
		
		return NullAction.INSTANCE;
	}
		
	private void log(String str) {
		if(LOG) {
			System.out.println("> "+str);
		}
	}
	
	
	/** When we are in the attacking state, this is where we are going and who we are attacking */
	private static class AttackingData {
		List<Position> nextSteps = new ArrayList<>();
		ICreature creatureToAttack;
	}
	
	/** When we are in the wandering state, this is a step-by-step route of where we are going next */
	private static class WanderingData {
		List<Position> nextSteps = new ArrayList<>();
	}
}
