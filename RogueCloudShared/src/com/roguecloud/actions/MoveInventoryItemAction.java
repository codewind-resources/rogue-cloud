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

import com.roguecloud.json.actions.JsonMoveInventoryItemAction;

/** 
 * To pick up an item from the ground, or place an item (from your inventory) on the ground, 
 * create this object and call the client sendAction(...) method.
 * 
 * If you wish to pick up an item, use the MoveInventoryItemAction.Type.PICK_UP_ITEM.
 * If you wish to drop an item, use the MoveInventoryItemAction.Type.DROP_ITEM.
 *  
 **/
public class MoveInventoryItemAction implements IAction {

	public static enum Type { PICK_UP_ITEM, DROP_ITEM };
	
	private final long objectId;
	private final boolean dropItem;
	
	@Override
	public ActionType getActionType() {
		return ActionType.MOVE_INVENTORY_ITEM;
	}
	
	public MoveInventoryItemAction(long objectId, Type type) {
		this.objectId = objectId;
		this.dropItem = type == Type.DROP_ITEM;
	}

	
	// Internal methods ------------------------------------------
	
	public boolean isDropItem() {
		return dropItem;
	}
	
	public long getObjectId() {
		return objectId;
	}

	public JsonMoveInventoryItemAction toJson() {
		JsonMoveInventoryItemAction result = new JsonMoveInventoryItemAction();
		result.setObjectId(objectId);
		result.setDropItem(dropItem);
		return result;
	}
}
