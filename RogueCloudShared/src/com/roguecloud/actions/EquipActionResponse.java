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
import com.roguecloud.json.actions.JsonEquipActionResponse;

/**
 * When a character equips an item (weapon/armour), the object will be returned indicating the result.
 * 
 * An equip item action can fail for the following reasons: 
 * 
 * - The item was not in the players inventory.
 * - The item is already equipped.
 * 
 * See the IActionResponse class for more information on action responses.
 *
 **/
public class EquipActionResponse implements IActionResponse {

	private final boolean success;
	
	private final long objectId;
	
	public EquipActionResponse(boolean success, long objectId) {
		this.success = success;
		this.objectId = objectId;
	}

	public long getObjectId() {
		return objectId;
	}
	
	@Override
	public boolean actionPerformed() {
		return success;
	}

	// Internal methods -----------------------------------------------------------
	
	public JsonAbstractTypedMessage toJson() {
		JsonEquipActionResponse response = new JsonEquipActionResponse();
		response.setObjectId(objectId);
		response.setSuccess(success);
		return response;
	}

}
