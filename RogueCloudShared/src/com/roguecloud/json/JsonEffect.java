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

public class JsonEffect {
	
	private String type;
	
	private int remainingTurns;
	
	private int magnitude;
	
	public JsonEffect() {
	}
	
	public JsonEffect(Map<?, ?> map) {
		type = (String) map.get("type");
		remainingTurns = (Integer)map.get("remainingTurns");
		magnitude = (Integer)map.get("magnitude");
	}
	
	public final String getType() {
		return type;
	}

	public final void setType(String type) {
		this.type = type;
	}

	public final int getRemainingTurns() {
		return remainingTurns;
	}

	public final void setRemainingTurns(int remainingTurns) {
		this.remainingTurns = remainingTurns;
	}

	public final int getMagnitude() {
		return magnitude;
	}

	public final void setMagnitude(int magnitude) {
		this.magnitude = magnitude;
	}

}
