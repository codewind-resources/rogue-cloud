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

import com.roguecloud.ActionResponseFuture;
import com.roguecloud.actions.IAction;
import com.roguecloud.actions.IActionResponse;
import com.roguecloud.actions.NullAction;
import com.roguecloud.client.IEventLog;
import com.roguecloud.client.RemoteClient;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.creatures.ICreature;

/** 
 * This class is a template for an advanced AI. 
 * 
 * For more information on building an advanced AI, consult the documentation on Github page.
 * You can also consult the implementation details of the SimpleAI class for an example of how to structure an agent implementation. 
 * 
 * To switch to running this class (rather than SimpleAI), change the 'StartAgentServlet.constructMyAI()' method 
 * to return an instance of this class. 
 * 
 **/
public class AdvancedAI extends RemoteClient {

	private ActionResponseFuture waitingForActionResponse;
	
	@Override
	public void stateUpdate(SelfState selfState, WorldState worldState, IEventLog eventLog) {

		ICreature me = selfState.getPlayer();
		
		if(me.isDead()) {
			return;
		}
		
		// Have we already sent an action that we are waiting for a response from?
		if(waitingForActionResponse != null) {
			IActionResponse response = waitingForActionResponse.getOrReturnNullIfNoResponse();
			if(response != null) {
				waitingForActionResponse = null;
				
				if(response.actionPerformed()) {
					// Our action succeeded, on to the next one.
				} else {
					// Handle the fact that the action failed, if necessary
					
				}
			}
		}
		
		IAction actionToPerform = NullAction.INSTANCE;
		
		// actionToPerform = ( what do you want your character to do? );
		

		// Send the action and store the response object 
		waitingForActionResponse = sendAction(actionToPerform);

	}

}
