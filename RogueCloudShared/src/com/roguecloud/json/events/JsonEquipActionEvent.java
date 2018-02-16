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

package com.roguecloud.json.events;

import java.util.Map;

import com.roguecloud.RCRuntime;
import com.roguecloud.json.JsonAbstractTypedMessage;

public class JsonEquipActionEvent extends JsonAbstractTypedMessage {

	public static final String TYPE = "JsonEquipActionEvent";

	private long creatureId;
	private long objectId;

	private long frame;
	private long id;

	public JsonEquipActionEvent() {
		setType(TYPE);
	}

	public JsonEquipActionEvent(Map<?, ?> map) {
		setType(TYPE);
		creatureId = RCRuntime.convertToLong(map.get("creatureId"));
		objectId = RCRuntime.convertToLong(map.get("objectId"));
	}

	public long getCreatureId() {
		return creatureId;
	}

	public void setCreatureId(long creatureId) {
		this.creatureId = creatureId;
	}

	public long getObjectId() {
		return objectId;
	}

	public void setObjectId(long objectId) {
		this.objectId = objectId;
	}

	public long getFrame() {
		return frame;
	}

	public void setFrame(long frame) {
		this.frame = frame;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

}
