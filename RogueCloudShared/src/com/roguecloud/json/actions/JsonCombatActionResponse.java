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

public class JsonCombatActionResponse extends JsonAbstractTypedMessage {
	
	public static final String TYPE = "JsonCombatActionResponse";
		
	
	private String result;
	
	private int damageDealt;
	
	private long targetCreatureId;

	public JsonCombatActionResponse() {
		setType(TYPE);
	}

	@SuppressWarnings("rawtypes")
	public JsonCombatActionResponse(JsonActionMessageResponse jamr) {
		Map hm = (Map) jamr.getActionResponse();
		
		result = (String) hm.get("result");
		
		damageDealt = (int) hm.get("damageDealt");
		
		targetCreatureId = RCRuntime.convertToLong( hm.get("targetCreatureId"));
		
	}
	
	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public int getDamageDealt() {
		return damageDealt;
	}

	public void setDamageDealt(int damageDealt) {
		this.damageDealt = damageDealt;
	}

	public long getTargetCreatureId() {
		return targetCreatureId;
	}

	public void setTargetCreatureId(long targetCreatureId) {
		this.targetCreatureId = targetCreatureId;
	}

	
}
