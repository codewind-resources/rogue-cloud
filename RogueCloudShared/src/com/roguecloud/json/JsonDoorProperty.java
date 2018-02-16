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

package com.roguecloud.json;

import java.util.Map;

public class JsonDoorProperty extends JsonAbstractTypedMessage{
	
	public static final String TYPE = "JsonDoorProperty";
	
	JsonPosition position;
	boolean isOpen = false;
	
	public JsonDoorProperty(Map<String, Object> map) {
		setType(TYPE);
		
		this.isOpen = ((Boolean)map.get("open")).booleanValue();
		this.position = new JsonPosition((Map<?, ?>) map.get("position"));
	}
	
	public JsonDoorProperty() {
		setType(TYPE);
	}
	
	public boolean isOpen() {
		return isOpen;
	}
	
	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}
	
	public JsonPosition getPosition() {
		return position;
	}
	
	public void setPosition(JsonPosition position) {
		this.position = position;
	}
	
	
}
