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

package com.roguecloud.json.actions;

import java.util.Map;

import com.roguecloud.RCRuntime;
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.JsonActionMessageResponse;

public class JsonMoveInventoryItemActionResponse extends JsonAbstractTypedMessage {

	public static final String TYPE = "JsonPickUpItemActionResponse";

	boolean success = false;
	
	boolean dropItem = false;
	
	long objectId;
	
	public JsonMoveInventoryItemActionResponse() {
		setType(TYPE);
	}
	
	public JsonMoveInventoryItemActionResponse(JsonActionMessageResponse o) {
		setType(TYPE);
		
		Map<?, ?> m = (Map<?, ?>)o.getActionResponse();
		
		success = (Boolean)m.get("success");
		objectId = RCRuntime.convertToLong(m.get("objectId"));
		dropItem = (Boolean)m.get("dropItem");
	}
	
	
	public long getObjectId() {
		return objectId;
	}
	
	public void setObjectId(long objectId) {
		this.objectId = objectId;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public boolean isSuccess() {
		return success;
	}
	
	public void setDropItem(boolean dropItem) {
		this.dropItem = dropItem;
	}
	
	public boolean isDropItem() {
		return dropItem;
	}
	
}
