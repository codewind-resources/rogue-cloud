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

import com.roguecloud.items.Effect;
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.actions.JsonDrinkItemActionResponse;

/**
 * When a character drinks a potion, this value will be returned indicating the result.
 * 
 * The effect of the item on the character can be retrieved by calling getEffect(...)
 * 
 * Note: The only reason for a drink item action to fail is if the agent does not have the item in their inventory. 
 *  
 * See the IActionResponse class for more information on action responses.
 *  
 **/
public class DrinkItemActionResponse implements IActionResponse {
	
	private final long id;
	
	private final boolean success;
	
	/** This may be null, for example if success is false.*/
	private final Effect effect;

	public DrinkItemActionResponse(boolean success, long id, Effect effect) {
		this.success = success;
		this.id = id;
		this.effect = effect;
	}

	public final boolean isSuccess() {
		return success;
	}

	public final Effect getEffect() {
		return effect;
	}

	@Override
	public boolean actionPerformed() {
		return success;
	}

	// Internal methods ---------------------------------------------
	
	public JsonAbstractTypedMessage toJson() {
		JsonDrinkItemActionResponse result = new JsonDrinkItemActionResponse();
		result.setId(this.id);
		result.setSuccess(this.success);
		if(effect != null) {
			result.setEffect(effect.toJson());
		}
		return result;
	}

	public final long getId() {
		return id;
	}

}
