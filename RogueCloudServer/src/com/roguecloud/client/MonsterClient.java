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
import com.roguecloud.RCRuntime;
import com.roguecloud.NG;
import com.roguecloud.actions.IAction;
import com.roguecloud.client.EventLog;
import com.roguecloud.client.IClient;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;

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
