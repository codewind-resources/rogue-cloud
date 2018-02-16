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
import com.roguecloud.json.JsonPosition;

public class JsonStepAction extends JsonAbstractTypedMessage {

	public static final String TYPE = "JsonStepAction";
	
	JsonPosition destination;

	@SuppressWarnings("rawtypes")
	public JsonStepAction(Map m) {
		setType(TYPE);
		Map position = (Map) m.get("destination");
		
		this.destination = new JsonPosition(position);
	}

	public JsonStepAction() {
		setType(TYPE);
	}
	
	public JsonPosition getDestination() {
		return destination;
	}
	
	public void setDestination(JsonPosition destination) {
		this.destination = destination;
	}
		
}
