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

import com.roguecloud.ActionResponseFuture;
import com.roguecloud.actions.IAction;
import com.roguecloud.client.MonsterClient;

public class AIContext {

	private final MonsterClient client;
	
	private final Object lock = new Object(); 
	
	private IAction action_synch_lock = null;
	
	private ActionResponseFuture future_synch_lock = null;
	
	private final long creatureId;
	
	public AIContext(long creatureId, MonsterClient client) {
		this.client = client;
		this.creatureId = creatureId;
	}
		
	public MonsterClient getClient() {
		return client;
	}
	
	public IAction getAction() {
		synchronized (lock) {
			return action_synch_lock;			
		}
	}

	public ActionResponseFuture getFuture() {
		synchronized(lock) {
			return future_synch_lock;
		}
	}
	
	public void putAction(IAction action, ActionResponseFuture future) {
		synchronized (lock) {
			action_synch_lock = action;
			future_synch_lock = future;
		}
	}
	
	
	public long getCreatureId() {
		return creatureId;
	}
		
}

