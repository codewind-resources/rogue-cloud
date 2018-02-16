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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.roguecloud.ActionResponseFuture;
import com.roguecloud.actions.IAction;
import com.roguecloud.client.EventLog;
import com.roguecloud.client.IClient;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.utils.Logger;

/** 
 * This class allows your agent code to receive the latest updates on what is happening in the world (the world state), 
 * as well as the ability to respond to that (by sending actions back to the server.)
 * 
 * Agent code should extend this class and implement the 'stateUpdate' method. 
 * To send actions to the server (move, attack, pick up item), call the sendAction(...) method with the action to perform.
 *  
 * See the sample agent code for an example of how this class' code is used. 
 **/
public abstract class RemoteClient implements IClient {
	
	private static final Logger log = Logger.getInstance(); 

	protected SelfState selfState = null;
	
	protected WorldState worldState = null;
	
	private ClientState clientState;
	
	protected EventLog eventLog;

	boolean disposed = false;
	
	public void setClientState(ClientState clientState) {
		this.clientState = clientState;
	}

	@Override
	public final ActionResponseFuture sendAction(IAction action) {
		try {
			if(disposed) { return null; }
			
			return clientState.sendAction(action);
		} catch (JsonProcessingException e) {
			log.severe("Exception on send action", e, null);
			return null;
		}
	}
	
	
	public IKeyValueStore getKeyValueStore() {
		return clientState.getKeyValueStore();
	}
	

	public void internalReceiveStateUpdate(SelfState selfState, WorldState newWorldState, EventLog eventLog) {		
		if(disposed) { return; } 
		this.worldState = newWorldState;
		
		this.selfState = selfState;
		
		this.eventLog = eventLog;
		
		try {
			stateUpdate(selfState, worldState, eventLog);
		} catch(Throwable t) {
			// Prevent bad agent code from affecting the calling class
			t.printStackTrace();
		}
	}
	
	public SelfState getSelfState() {
		return selfState;
	}
	
	public WorldState getWorldState() {
		return worldState;
	}
	
	public EventLog getEventLog() {
		return eventLog;
	}
	

	void internalDispose() {
		disposed = true;
		selfState = null;
		worldState = null;
		clientState = null;
		eventLog = null;
	}

}
