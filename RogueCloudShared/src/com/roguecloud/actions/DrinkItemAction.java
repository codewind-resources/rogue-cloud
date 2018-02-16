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

import com.roguecloud.items.OwnableObject;
import com.roguecloud.json.actions.JsonDrinkItemAction;

/**
 * To drink a potion that is in your inventory, create this object and send it with client sendAction(...) method.
 * 
 * This will cause your character to drink the inventory item (and grant you the effect of the potion, such as healing or damage reduction.)
 *  
 **/
public class DrinkItemAction implements IAction {

	private OwnableObject oo;
	
	public DrinkItemAction(final OwnableObject objectToDrink) {
		if(objectToDrink == null) { throw new IllegalArgumentException(); }
		this.oo  = objectToDrink;
	}
	
	@Override
	public ActionType getActionType() {
		return ActionType.DRINK;
	}

	public OwnableObject getObject() {
		return oo;
	}
	
	// Internal methods --------------------------------------------------------
	
	public JsonDrinkItemAction toJson() {
		JsonDrinkItemAction jdia = new JsonDrinkItemAction();
		jdia.setId(oo.getId());
		return jdia;
	}
	
}
