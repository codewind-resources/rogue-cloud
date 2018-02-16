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
import com.roguecloud.map.IMap;
import com.roguecloud.utils.AIUtils;
import com.roguecloud.utils.FastPathSearch;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.ServerUtil;

public class StationaryGuardAndChaseAI extends MonsterClient {

	public static final Logger log = Logger.getInstance();

	public static final boolean LOG = true;

	public enum State { GUARDING, CHASING, 
		MOVING_BACK_TO_GUARDING, 
		STUCK }; 
	
	public State current = State.GUARDING;
	
	private ActionResponseFuture actionResponse;

	private Position positionIAmGuarding = null;
	
	private ChasingData chasingData = null;
	private MovingBackData movingBackData = null;
		
	@Override
	public void stateUpdate(SelfState selfState, WorldState worldState, IEventLog eventLog) {

		if(positionIAmGuarding == null) {
			positionIAmGuarding = selfState.getPlayer().getPosition();
		}
		
		boolean lastMoveFailed = false;

		if (actionResponse != null) {
			try {
				// log("Waiting for action: "+actionResponse);
				if (actionResponse.isResponseReceived()) {
					if (!actionResponse.getOrWaitForResponse().actionPerformed()) {
						lastMoveFailed = true;
					}
					actionResponse = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (actionResponse == null) {
			IAction action = findNextMove(selfState, worldState, eventLog, lastMoveFailed);
			if (action != null && action.getActionType() != ActionType.NULL) {
				actionResponse = sendAction(action);
			}
		}
	}

	private IAction findNextMove(SelfState selfState, WorldState worldState, IEventLog eventLog, boolean lastMoveFailed) {

		if(current == State.GUARDING) {
			AIUtils.findCreaturesInRange(10, worldState.getMap(), selfState.getPlayer().getPosition())
				.stream()
				.filter(e -> e.isPlayerCreature())
				.findFirst().ifPresent( e -> {
					log(selfState.getPlayer()+" is now chasing "+e);
					chasingData = new ChasingData();
					chasingData.creatureToAttack = e;
					current = State.CHASING;
				}
			);
		} 
		
		if(current == State.CHASING) {
			return doChasing(selfState, worldState, eventLog);
		} 
		
		if(current == State.MOVING_BACK_TO_GUARDING) {
			return doMovingBack(selfState, worldState, eventLog);
		}
		
		return NullAction.INSTANCE;
	}
	
	private IAction doMovingBack(SelfState selfState, WorldState worldState, IEventLog eventLog) {

		ICreature me = selfState.getPlayer();
		
		if(positionIAmGuarding == null) {
			movingBackData = null;
			current = State.GUARDING;
			return NullAction.INSTANCE;
		}

		// Have we arrived at our destination?
		if(selfState.getPlayer().getPosition().equals(positionIAmGuarding)) {
			movingBackData = null;
			log(me+" is now guarding.");
			current = State.GUARDING;
			return NullAction.INSTANCE;
		}
		
		// If next steps is empty, then null it
		if(movingBackData.nextSteps != null && movingBackData.nextSteps.size() == 0) {
			movingBackData.nextSteps = null;
		}
		
		// If we have no steps to follow, then try to find a path back to 'positionIAmGuarding'
		if(movingBackData.nextSteps == null) {
			
			List<Position> p = FastPathSearch.doSearchWithAStar(selfState.getPlayer().getPosition(), positionIAmGuarding, ServerUtil.maxAStarSearch(worldState.getMap()), worldState.getMap());
			if(p.size() > 0) {
				p.remove(0);
				movingBackData.nextSteps = p;
			} else {
				movingBackData = null;
				log(me+" became stuck.");
				current = State.STUCK;
				return NullAction.INSTANCE;
			}
		}
		
		if(movingBackData.nextSteps != null && movingBackData.nextSteps.size() > 0) {
			return new StepAction(movingBackData.nextSteps.remove(0));
		}
		
		return NullAction.INSTANCE;
		
	}
	
	private IAction doChasing(SelfState selfState, WorldState worldState, IEventLog eventLog) {
		
		ChasingData data = chasingData;
		ICreature me = selfState.getPlayer();
		IMap map = worldState.getMap();
		
		if(data.creatureToAttack == null || data.creatureToAttack.isDead()) {
			chasingData = null;
			log(me+" is moving back to guarding.");
			current = State.MOVING_BACK_TO_GUARDING;
			movingBackData = new MovingBackData();
			return NullAction.INSTANCE;
		}
		
		if(AIUtils.canReach(me.getPosition(), data.creatureToAttack.getPosition(), map)) {
			data.nextSteps = null;
			return new CombatAction(data.creatureToAttack);
		}
	
		// If we are at the end of our step list, then null it
		if(data.nextSteps != null && data.nextSteps.size() == 0) {
			data.nextSteps = null;
		}
		
		// If we are closer than 10 steps, then recalculate a new path every time
		if(me.getPosition().manhattanDistanceBetween(data.creatureToAttack.getPosition()) <= 10) {
			data.nextSteps = null;
		} else if(data.nextSteps != null && data.nextSteps.size() > 0) {
			// If we are farther than 10, and there is a path to follow, then follow it.
			Position next = data.nextSteps.remove(0);
			return new StepAction(next);
		}
		
		if(data.nextSteps == null) {
			
			List<Position> path = FastPathSearch.doSearchWithAStar(me.getPosition(), data.creatureToAttack.getPosition(), ServerUtil.maxAStarSearch(map), worldState.getMap());
			if(path.size() > 1) {
				path.remove(0); // Always remove the first, as it is the player's current pos
				
				data.nextSteps = path;
				
				return new StepAction(path.remove(0));
			}
			
		}
		
		return NullAction.INSTANCE;
		
	}

	private void log(String str) {
		if (LOG) {
			System.out.println("> " + str);
		}
	}
	
	private static class MovingBackData {
		List<Position> nextSteps = null;
	}
	
	private static class ChasingData {
		List<Position> nextSteps = null;
		ICreature creatureToAttack; 		
	}
}
