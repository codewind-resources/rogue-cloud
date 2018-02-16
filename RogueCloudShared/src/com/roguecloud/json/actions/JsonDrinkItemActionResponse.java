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
import com.roguecloud.json.JsonEffect;

public class JsonDrinkItemActionResponse extends JsonAbstractTypedMessage {

	public static final String TYPE = "JsonDrinkItemActionResponse";

	private long id;

	private boolean success;

	private JsonEffect effect;

	public JsonDrinkItemActionResponse() {
		setType(TYPE);
	}

	public JsonDrinkItemActionResponse(JsonActionMessageResponse o) {
		Map<?, ?> m = (Map<?, ?>) o.getActionResponse();
		success = (Boolean)m.get("success");
		id = RCRuntime.convertToLong(m.get("id"));
		
		Map<?, ?> jsonEffect = (Map<?, ?>) m.get("effect");
		if(jsonEffect != null) {
			this.effect = new JsonEffect(jsonEffect);
		}
	}

	public final long getId() {
		return id;
	}

	public final void setId(long id) {
		this.id = id;
	}

	public final boolean isSuccess() {
		return success;
	}

	public final void setSuccess(boolean success) {
		this.success = success;
	}

	public final JsonEffect getEffect() {
		return effect;
	}

	public final void setEffect(JsonEffect effect) {
		this.effect = effect;
	}

}
