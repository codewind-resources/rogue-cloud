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

import com.roguecloud.json.actions.JsonNullAction;

/** 
 * A null action may be sent by the player to indicate that they wish their character to not perform an action this turn. 
 * 
 * Since this action is a no-op on the server side, this is also equivalent to not sending an action at all.
 * 
 **/
public class NullAction implements IAction {

	public static final NullAction INSTANCE = new NullAction();
	
	private NullAction() {
	}

	@Override
	public ActionType getActionType() {
		return ActionType.NULL;
	}
	
	@Override
	public String toString() {
		return "NullAction";
	}
	
	// Internal methods -------------------------------------------
	
	public JsonNullAction toJson() {
		return new JsonNullAction();
	}
}
