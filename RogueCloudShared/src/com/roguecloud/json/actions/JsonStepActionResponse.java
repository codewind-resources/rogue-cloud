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

import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.JsonActionMessageResponse;
import com.roguecloud.json.JsonPosition;

public class JsonStepActionResponse extends JsonAbstractTypedMessage {
	
	public static final String TYPE = "JsonStepActionResponse"; 
	
	private String failReason;
	
	private JsonPosition newPosition;
	
	private boolean success;

	public JsonStepActionResponse() {
		setType(TYPE);
	}

	@SuppressWarnings("rawtypes")
	public JsonStepActionResponse(JsonActionMessageResponse response) {
		Map m = (Map) response.getActionResponse();
		
		failReason = (String) m.get("failReason");
		success = (boolean) m.get("success");
		
		
		if(m.get("newPosition") != null) {
			newPosition = new JsonPosition((Map<?, ?>) m.get("newPosition"));	
		}
	}

	
	public String getFailReason() {
		return failReason;
	}

	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}

	public JsonPosition getNewPosition() {
		return newPosition;
	}

	public void setNewPosition(JsonPosition newPosition) {
		this.newPosition = newPosition;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}
}
