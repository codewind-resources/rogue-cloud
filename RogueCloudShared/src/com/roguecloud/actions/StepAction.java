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

package com.roguecloud.actions;

import com.roguecloud.Position;
import com.roguecloud.json.actions.JsonStepAction;

/**
 * 
 * To move your character, create an instance of this action and call the client API's sendAction(...) command.
 * 
 * You may only move one step at a time, and may not move diagonally.  
 * 
 **/
public class StepAction implements IAction {

	final Position destPosition;
	
	public StepAction(Position p) {
		this.destPosition = p;
	}
	
	public Position getDestPosition() {
		return destPosition;
	}

	@Override
	public ActionType getActionType() {
		return ActionType.STEP;
	}

	@Override
	public String toString() {
		return "StepAction: "+destPosition;
	}
	
	// Internal methods -----------------------------------------
	
	public JsonStepAction toJson() {
		JsonStepAction jsa = new JsonStepAction();
		jsa.setDestination(destPosition.toJson());
		return jsa;
	}
}
