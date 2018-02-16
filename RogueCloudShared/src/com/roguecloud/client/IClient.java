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

import com.roguecloud.ActionResponseFuture;
import com.roguecloud.actions.IAction;

/** 
 * This interface is implemented by the client API, to allow agents to receive the latest self/world information (from the stateUpdate(...) method)
 * and allow agents to send actions to be performed by their character in the game world. 
 */
public interface IClient {

	/** This method is called by the agent API to inform the agent code of the latest state of the world and their character. */
	public void stateUpdate(SelfState selfState, WorldState worldState, IEventLog eventLog);
	
	/** This method is called by agent code to have the character perform an action in the world (such as attacking, moving, etc). */
	public ActionResponseFuture sendAction(IAction action);
	
}
