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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.roguecloud.ActionResponseFuture;
import com.roguecloud.Position;
import com.roguecloud.actions.CombatAction;
import com.roguecloud.actions.IAction;
import com.roguecloud.actions.IAction.ActionType;
import com.roguecloud.actions.NullAction;
import com.roguecloud.actions.StepAction;
import com.roguecloud.client.IEventLog;
import com.roguecloud.client.MonsterClient;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.map.Tile;
import com.roguecloud.utils.AIUtils;
import com.roguecloud.utils.FastPathSearch;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.ServerUtil;

public class WanderingAttackAllAI extends MonsterClient {
	
	public static final Logger log = Logger.getInstance();

	public static final boolean LOG = false;
	
	private ActionResponseFuture actionResponse;
	
	private enum WanderingState { WANDERING, ATTACKING};
	
	private WanderingState current = WanderingState.WANDERING; 
	
	private WanderingData wanderingData = null;
	private AttackingData attackingData = null;

	
	/** May be null */
	private final Position wanderPointFixedTopLeft;
	
	/** May be null */
	private final Position wanderPointFixedBottomRight;
	
	/* Only used if the other wander point vars are set */
	private final boolean wanderPointOnlyAttackInside; 
	
	private boolean attackOtherMonsters = true;
	
	public WanderingAttackAllAI() {
		this.wanderPointFixedTopLeft = null;
		this.wanderPointFixedBottomRight = null;
		this.wanderPointOnlyAttackInside = false;
	}
	
	public WanderingAttackAllAI(Position wanderPointFixedTopLeft, Position wanderPointFixedBottomRight, boolean onlyAttackInsideWanderRectangle) {
		this.wanderPointFixedTopLeft = wanderPointFixedTopLeft;
		this.wanderPointFixedBottomRight = wanderPointFixedBottomRight;
		this.wanderPointOnlyAttackInside = onlyAttackInsideWanderRectangle;
	}

	@Override
	public void stateUpdate(SelfState selfState, WorldState worldState, IEventLog eventLog) {

		boolean lastMoveFailed = false;
		
		ICreature me = selfState.getPlayer();

		if(me.isDead()) {
			return;
		}
		
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
			log.severe("Unrecognize state: "+current, null);
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
		if(AIUtils.canReach(us.getPosition(), toAttacker.getPosition(), worldState.getMap())) {
			
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
					} else {
						// If we can't find a path to them, then reset back to wandering
						data.creatureToAttack = null;
						data.nextSteps = null;
						this.attackingData = null;
						current = WanderingState.WANDERING;
						
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

		ICreature me = selfState.getPlayer();
		
		// Check if someone attacked us, and attack them back!
		{
			
			List<ICreature> creatures = Collections.emptyList();
			
			if(wanderPointFixedBottomRight != null && wanderPointFixedTopLeft != null && wanderPointOnlyAttackInside) {
				
				// Only look for things to attack if we are physically inside the wander box
				if(Position.containedInBoxCoords(selfState.getPlayer().getPosition(), wanderPointFixedTopLeft.getX(), wanderPointFixedTopLeft.getY(), wanderPointFixedBottomRight.getX(), wanderPointFixedBottomRight.getY() )) {
					// Only attack creatures in the wander box (this is useful for guarding monsters)
					creatures = AIUtils.findCreaturesInRectangle(wanderPointFixedTopLeft.getX(), wanderPointFixedTopLeft.getY(), 
							wanderPointFixedBottomRight.getX(), wanderPointFixedBottomRight.getY(), selfState.getPlayer().getPosition(), worldState.getMap());
				}
				
			} else {
				// Attack any creature we can see
				creatures = AIUtils.findCreaturesInRange(worldState.getViewWidth(), worldState.getViewHeight(), selfState.getPlayer().getPosition(), worldState.getMap());
			}
			
			if(!attackOtherMonsters) {
				creatures = creatures.stream().filter( e -> e.isPlayerCreature()).collect(Collectors.toList());
			}

			if(creatures.size() > 0) {

				AIUtils.sortClosestCreatures(me.getPosition(), creatures);

				current = WanderingState.ATTACKING;
				attackingData = new AttackingData();
				attackingData.creatureToAttack = creatures.get(0);
				
//				log(me+" is now attacking "+attackingData.creatureToAttack+" "+me.getPosition().distanceBetween(attackingData.creatureToAttack.getPosition()));
				
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

			int x, y;
			
			if(wanderPointFixedBottomRight != null && wanderPointFixedTopLeft != null) {
				// Pick a random spot within the wander point rectangle
				x = (int)(Math.random() * (wanderPointFixedBottomRight.getX() - wanderPointFixedTopLeft.getX())) + wanderPointFixedTopLeft.getX();
				y  = (int)(Math.random() * (wanderPointFixedBottomRight.getY() - wanderPointFixedTopLeft.getY())) + wanderPointFixedTopLeft.getY();
			} else {
				x = (int)(Math.random() * worldWidth);
				y = (int)(Math.random() * worldHeight);
				
			}

			Position targetPos = new Position(x, y);
			
			Tile t = worldState.getMap().getTile(targetPos);
			if(t != null && t.isPresentlyPassable()) {
		
				List<Position> l = FastPathSearch.doSearchWithAStar(selfState.getPlayer().getPosition(), targetPos, worldState.getMap());
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
	
	public void setAttackOtherMonsters(boolean attackOtherMonsters) {
		this.attackOtherMonsters = attackOtherMonsters;
	}
	
	public Long getConstrainedArea() {
		
		if(wanderPointFixedTopLeft == null || wanderPointFixedBottomRight == null) { return null; }
		
		long xDiff = wanderPointFixedBottomRight.getX() - wanderPointFixedTopLeft.getX() + 1;
		
		long yDiff = wanderPointFixedBottomRight.getY() -  wanderPointFixedTopLeft.getY() + 1;
		
		if(xDiff < 0 || yDiff  < 0) {
			throw new IllegalStateException();
		}
		
		return xDiff * yDiff;
		
	}
	
	
	private static class AttackingData {
		List<Position> nextSteps = new ArrayList<>();
		ICreature creatureToAttack;
	}
	
	private static class WanderingData {
		List<Position> nextSteps = new ArrayList<>();
	}
}
