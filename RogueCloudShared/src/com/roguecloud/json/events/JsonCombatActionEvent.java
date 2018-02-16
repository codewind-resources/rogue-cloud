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

public class JsonCombatActionEvent extends JsonAbstractTypedMessage {

	public static final String TYPE = "JsonCombatActionEvent";

	private long attackerId;
	private long defenderId;

	private boolean hit;
	private long damageDone;

	private long frame;

	private long id;

	public JsonCombatActionEvent() {
		setType(TYPE);
	}

	public JsonCombatActionEvent(Map<?, ?> map) {
		setType(TYPE);
		attackerId = RCRuntime.convertToLong(map.get("attackerId"));
		defenderId = RCRuntime.convertToLong(map.get("defenderId"));
		hit = (Boolean) map.get("hit");
		damageDone = RCRuntime.convertToLong(map.get("damageDone"));
		frame = RCRuntime.convertToLong(map.get("frame"));
		id = RCRuntime.convertToLong(map.get("id"));
	}

	public long getAttackerId() {
		return attackerId;
	}

	public void setAttackerId(long attackerId) {
		this.attackerId = attackerId;
	}

	public long getDefenderId() {
		return defenderId;
	}

	public void setDefenderId(long defenderId) {
		this.defenderId = defenderId;
	}

	public boolean isHit() {
		return hit;
	}

	public void setHit(boolean hit) {
		this.hit = hit;
	}

	public long getDamageDone() {
		return damageDone;
	}

	public void setDamageDone(long damageDone) {
		this.damageDone = damageDone;
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
