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

import com.roguecloud.creatures.ICreature;
import com.roguecloud.json.actions.JsonCombatAction;

/**
 * To attack another creature, create this object and send it with the client sendAction(...) method.
 * 
 * This will cause your character to attack another creature from your current position.
 *  
 **/
public class CombatAction implements IAction {
	
	private final ICreature target;
	
	public CombatAction(ICreature target) {
		if(target == null) { throw new IllegalArgumentException(); }
		this.target = target;
	}

	@Override
	public ActionType getActionType() {
		return IAction.ActionType.COMBAT;
	}

	/** The desired target of the attack */
	public ICreature getTarget() {
		return target;
	}

	public String toString() {
		return "CombatAction - target: "+target.getId();
	}
	
	
	// Internal methods ------------------------------------------
	
	public JsonCombatAction toJson() {
		JsonCombatAction jca = new JsonCombatAction();
		jca.setTargetCreatureId(target.getId());
		return jca;
	}
}
