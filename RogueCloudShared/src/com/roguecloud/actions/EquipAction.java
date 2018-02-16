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
import com.roguecloud.json.actions.JsonEquipAction;

/**
 * To equip weapons or armour from your inventory, create this object and send it with the client sendAction(...) method.
 * 
 * The piece of weapon/armour must be in your inventory before the action is performed. Items may be moved into your
 * inventory using the MoveInventoryItemAction.
 *  
 **/
public class EquipAction implements IAction {

	private final OwnableObject ownableObject;
	
	public EquipAction(OwnableObject objectToEquip) {
		this.ownableObject = objectToEquip;
	}
	
	public OwnableObject getOwnableObject() {
		return ownableObject;
	}
	
	@Override
	public ActionType getActionType() {
		return ActionType.EQUIP;
	}
	
	// Internal methods---------------------------------------------------

	public JsonEquipAction toJson() {
		JsonEquipAction result = new JsonEquipAction();
		result.setObjectId(ownableObject.getId());
		return result;
	}

}
