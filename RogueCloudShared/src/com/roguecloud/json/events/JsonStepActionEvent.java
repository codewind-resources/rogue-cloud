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
import com.roguecloud.json.JsonPosition;

public class JsonStepActionEvent extends JsonAbstractTypedMessage {

	public static final String TYPE = "JsonStepActionEvent";

	private JsonPosition from;
	private JsonPosition to;
	private long creatureId;
	private long frame;
	private long eventId;

	public JsonStepActionEvent(Map<?, ?> map) {
		
		setType(TYPE);
		
		from = new JsonPosition((Map<?, ?>) map.get("from"));
		to = new JsonPosition((Map<?, ?>) map.get("to"));
		
		creatureId = RCRuntime.convertToLong(map.get("creatureId"));
		frame = RCRuntime.convertToLong(map.get("frame"));
		eventId  = RCRuntime.convertToLong(map.get("eventId"));
		
	}
	
	public JsonStepActionEvent() {
		setType(TYPE);
	}

	public JsonPosition getFrom() {
		return from;
	}

	public void setFrom(JsonPosition from) {
		this.from = from;
	}

	public JsonPosition getTo() {
		return to;
	}

	public void setTo(JsonPosition to) {
		this.to = to;
	}

	public long getCreatureId() {
		return creatureId;
	}

	public void setCreatureId(long creatureId) {
		this.creatureId = creatureId;
	}

	public long getFrame() {
		return frame;
	}

	public void setFrame(long frame) {
		this.frame = frame;
	}

	public long getEventId() {
		return eventId;
	}

	public void setEventId(long eventId) {
		this.eventId = eventId;
	}

}
