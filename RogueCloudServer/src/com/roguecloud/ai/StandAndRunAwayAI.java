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
import com.roguecloud.actions.IAction;
import com.roguecloud.actions.NullAction;
import com.roguecloud.actions.StepAction;
import com.roguecloud.client.IEventLog;
import com.roguecloud.client.MonsterClient;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.events.CombatActionEvent;
import com.roguecloud.events.IEvent;
import com.roguecloud.events.IEvent.EventType;
import com.roguecloud.map.IMap;
import com.roguecloud.map.Tile;

/**
 * This AI will never attack, it will only ever run away from creatures that attack it.
 */
public class StandAndRunAwayAI extends MonsterClient {

	ICreature creatureWeAreRunningAwayFrom = null;

	IAction ourLastAction;
	
	private ActionResponseFuture actionResponse;
	
	@SuppressWarnings("unused")
	private static final Object lock = new Object();
	
	private boolean LOG = false;
	
	@Override
	public void stateUpdate(SelfState selfState, WorldState worldState, IEventLog eventLog) {
		
		if(actionResponse != null) {
			try {
				log("Waiting for action: "+ourLastAction);
				if(actionResponse.isResponseReceived()) {
					actionResponse = null;
					ourLastAction = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(actionResponse == null) {
			IAction action = findNextMove(selfState, worldState, eventLog);
			if(action != null) {
				actionResponse = sendAction(action);
				ourLastAction = action;
				
			}	
		}
	}
	
	
	private IAction findNextMove(SelfState selfState, WorldState worldState, IEventLog eventLog) {
		ICreature us = selfState.getPlayer();

		log("curr hp: "+selfState.getPlayer().getHp()+" frame: "+worldState.getCurrentGameTick());
		
		List<IEvent> events = eventLog.getLastTurnSelfEvents(us, worldState);

		if(creatureWeAreRunningAwayFrom == null) {
			List<ICreature> attackers = new ArrayList<ICreature>();
			
			log("self events: "+eventLog.getLastTurnSelfEvents(us, worldState).size());
//			log("--------------------------------------");
//			eventLog.getAllEvents().stream().filter( e-> e.isCreatureInvolved(us) ).forEach( e -> {
//				log(e.toString());
//			});
			
//			System.out.println("events: "+eventLog.getAllEvents().size());
			
			events.stream().filter(e -> e.getActionType() == EventType.COMBAT && e.isCreatureInvolved(us)).forEach(e -> {
				CombatActionEvent cae = (CombatActionEvent) e;

				if (cae.getDefender() == null || cae.getAttacker() == null ||  !cae.getDefender().equals(us)) {
					return;
				}
				attackers.add(cae.getAttacker());
			});
			
			attackers.stream().findFirst().ifPresent(e -> {
				creatureWeAreRunningAwayFrom = e;
			});
		}
		
		if(creatureWeAreRunningAwayFrom != null) {
			log("We are being attacked by: "+creatureWeAreRunningAwayFrom);
			
			int distance = creatureWeAreRunningAwayFrom.getPosition().distanceBetween(us.getPosition());
			if(distance > 10) {
				creatureWeAreRunningAwayFrom = null;
				log("We are no longer being attacked. Distance: "+distance+" Creature: "+creatureWeAreRunningAwayFrom);
				return NullAction.INSTANCE;
			}
			
			Position newPosition = getOppositeDirection(us, creatureWeAreRunningAwayFrom, worldState);
			if(newPosition == null) {
				newPosition = findRandomValidStepPosition(us, worldState);
			}
			
			if(newPosition != null) {
				log("Running away, new StepAction: "+us.getPosition()+" -> "+newPosition);
				
				return new StepAction(newPosition);
			}
			
		} else {
//			log("We are not being attacked or running away.");
		}
		
		return NullAction.INSTANCE;

	}
	
	private static Position findRandomValidStepPosition(ICreature us, WorldState worldState) {
		Position[] possibilities = new Position[] {
				new Position(us.getPosition().getX()+1, us.getPosition().getY()),
				new Position(us.getPosition().getX()-1, us.getPosition().getY()),
				new Position(us.getPosition().getX(), us.getPosition().getY()+1),
				new Position(us.getPosition().getX(), us.getPosition().getY()-1)
		};
		
		int indent = (int)(Math.random()*4);
		
		IMap map = worldState.getMap();
		for(int x = 0; x < possibilities.length; x++) {
			Position curr = possibilities[(x+indent)%possibilities.length];
			
			if(curr.isValid(map)) { 
				
				Tile t = map.getTile(curr);
				if(t != null && t.isPresentlyPassable()) {
					return curr;
				}
				
			}
			
		}
		
		return null;
		
	}

	
	private static Position getOppositeDirection(ICreature us, ICreature other, WorldState ws) {
		int newDeltaX = (other.getPosition().getX() - us.getPosition().getX());
		int newDeltaY = (other.getPosition().getY() - us.getPosition().getY());
		
		// Set to 1, and inverse. 
		if(newDeltaX != 0) { newDeltaX = newDeltaX > 0 ? -1 : 1; }
		if(newDeltaY != 0) { newDeltaY = newDeltaY > 0 ? -1 : 1; }
		
		if(Math.abs(newDeltaX) != 0 && Math.abs(newDeltaY) != 0) {
			if(Math.random() < 0.5) {
				newDeltaX = 0;
			} else {
				newDeltaY = 0;
			}
		}
		
		if(newDeltaX == 0 && newDeltaY == 0) {
			int magnitude = Math.random() > 0.5 ? 1 : -1;
			
			if(Math.random() < 0.5) {
				newDeltaX = magnitude;
			} else {
				newDeltaY = magnitude;
			}
		}
		
		Position newPosition = new Position(us.getPosition().getX()+newDeltaX, us.getPosition().getY()+newDeltaY);
		Tile t = ws.getMap().getTile(newPosition);
		if(t != null) {
			if(!t.isPresentlyPassable()) {
				return null;
			}
			
			if(!newPosition.isValid(ws.getMap())) {
				return null;
			}
			
			return newPosition;
			
		} else {
			return null;
		}

	}
	private void log(String str) {
		if(LOG) {
			System.out.println("> "+str);
		}
	}
	
}
