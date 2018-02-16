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
import com.roguecloud.json.actions.JsonCombatActionResponse;

/** 
 * When a combat action occurs, this response indicates whether it succeeded (HIT/MISS), and if hit then 
 * how much damage was done.
 * 
 * See IActionResponse for a description of what an Action response is, and what that means.
 *  
 **/
public class CombatActionResponse implements IActionResponse {

	/** Whether the combat action succeeded, or failed (and why it failed) */
	public static enum CombatActionResult { HIT, MISS, COULD_NOT_ATTACK, OTHER };
	
	private final CombatActionResult result;
	
	private final int damageDealt;
	
	private final ICreature creature; // may be null
	
	public CombatActionResponse(CombatActionResult result, int damageDealt, ICreature creature) {
		this.result = result;
		this.damageDealt = damageDealt;
		this.creature = creature;
	}

	/** The amount of damage done in combat. */
	public int getDamageDealt() {
		return damageDealt;
	}
	
	/** Whether the attack was successful */
	public CombatActionResult getResult() {
		return result;
	}
	
	/** The creatue attacked. */
	public ICreature getCreature() {
		return creature;
	}
	
	public JsonCombatActionResponse toJson() {
		JsonCombatActionResponse result = new JsonCombatActionResponse();
		result.setDamageDealt(damageDealt);
		result.setResult(this.result.name());
		
		if(this.creature != null) {
			result.setTargetCreatureId(this.creature.getId());
		} else {
			result.setTargetCreatureId(-1);
		}
		
		return result;
	}

	@Override
	public boolean actionPerformed() {
		if(result == CombatActionResult.COULD_NOT_ATTACK || result == CombatActionResult.OTHER) {
			return false;
		}
		
		return true;
	}
	
}
