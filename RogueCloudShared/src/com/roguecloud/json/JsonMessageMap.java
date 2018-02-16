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

import java.util.HashMap;

import com.roguecloud.json.browser.JsonUpdateBrowserUI;
import com.roguecloud.json.client.JsonHealthCheck;
import com.roguecloud.json.client.JsonHealthCheckResponse;

public final class JsonMessageMap {

	private static final HashMap<String, Class<?>> map = new HashMap<>();
	
	static {
		map.put(JsonClientConnect.TYPE, JsonClientConnect.class);
		map.put(JsonClientConnectResponse.TYPE, JsonClientConnectResponse.class);
		map.put(JsonFrameUpdate.TYPE, JsonFrameUpdate.class);
		map.put(JsonActionMessage.TYPE, JsonActionMessage.class);
		map.put(JsonActionMessageResponse.TYPE, JsonActionMessageResponse.class);
		map.put(JsonUpdateBrowserUI.TYPE, JsonUpdateBrowserUI.class);
		map.put(JsonRoundComplete.TYPE, JsonRoundComplete.class);
		map.put(JsonHealthCheck.TYPE, JsonHealthCheck.class);
		map.put(JsonHealthCheckResponse.TYPE, JsonHealthCheckResponse.class);
		map.put(JsonClientInterrupt.TYPE, JsonClientInterrupt.class);
	}
	
	public static HashMap<String, Class<?>> getMap() {
		return map;
	}
	
}
