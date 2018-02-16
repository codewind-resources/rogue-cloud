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

import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.actions.JsonMoveInventoryItemActionResponse;

/*
 * When the player picks up an item, or drops an item, this response object is returned to indicate whether or not the action succeeded.
 * 
 * The move inventory item action can fail for the following reasons:
 * 
 * - No item was found at the character's position (or within their reach) that they could pick up 
 * - The character attempted to drop an item that was not in their inventory.  
 * 
 * See the IActionResponse class for more information on action responses.
 *  
 **/
public class MoveInventoryItemActionResponse implements IActionResponse {

	final long objectId;
	final boolean success;
	
	final boolean dropItem;
	
	public MoveInventoryItemActionResponse(long objectId, boolean success, boolean dropItem) {
		this.objectId = objectId;
		this.success = success;
		this.dropItem = dropItem;
	}
	
	public MoveInventoryItemAction.Type getType() {
		return this.dropItem ? MoveInventoryItemAction.Type.DROP_ITEM : MoveInventoryItemAction.Type.PICK_UP_ITEM;
	}

	@Override
	public boolean actionPerformed() {
		return success;
	}
	
	public boolean isDropItem() {
		return dropItem;
	}
	
	@Override
	public String toString() {
		return this.getClass().getName()+" - "+objectId+" success: "+success+" "+dropItem;
	}
	
	// Internal methods ---------------------------------------------------------------
	
	public long getObjectId() {
		return objectId;
	}

	public JsonAbstractTypedMessage toJson() {
		JsonMoveInventoryItemActionResponse result = new JsonMoveInventoryItemActionResponse();
		
		result.setObjectId(objectId);
		result.setSuccess(success);
		result.setDropItem(dropItem);
		
		return result;
	}

	
}
