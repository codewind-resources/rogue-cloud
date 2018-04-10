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

import com.roguecloud.AIContext;
import com.roguecloud.ActionResponseFuture;
import com.roguecloud.actions.IAction;

/** 
 * This class allows the monster AI code to receive the latest updates on what is happening in the world (the world state), 
 * as well as the ability to respond to that.
 * 
 * Just like player AI code extends the RemoteClient class, monster AI code should extend this class and 
 * implement the implement the 'stateUpdate' method from the parent interface.
 *  
 * To pass actions back the game loop (move, attack, pick up item), call the sendAction(...) method with the action to perform.
 *  
 **/
public abstract class MonsterClient implements IClient {

	protected SelfState selfState;
	
	protected WorldState worldState;
	
	private AIContext aiContext;
	
	protected EventLog eventLog;
	
	public MonsterClient() {
	}
	
	public void setAIContext(AIContext aiContext) {
		this.aiContext = aiContext;
	}
	
	public final ActionResponseFuture sendAction(IAction action) {
		
		ActionResponseFuture future = new ActionResponseFuture();
		aiContext.putAction(action, future);
		return future;
	}

	public void receiveStateUpdate(SelfState selfState, WorldState worldState, EventLog eventLog) {
		this.selfState = selfState;
		this.worldState = worldState;
		this.eventLog = eventLog;
		this.stateUpdate(selfState, worldState, eventLog);
	}

}
