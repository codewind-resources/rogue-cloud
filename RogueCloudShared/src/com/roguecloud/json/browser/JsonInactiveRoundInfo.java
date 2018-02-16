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
package com.roguecloud.json.browser;

import com.roguecloud.json.JsonAbstractTypedMessage;

public class JsonInactiveRoundInfo extends JsonAbstractTypedMessage {

	public static final String TYPE = "INACTIVE";

	long nextRoundId;

	long nextRoundStartInSecs;

	public JsonInactiveRoundInfo() {
		setType(TYPE);
	}

	public JsonInactiveRoundInfo(long nextRoundId, long nextRoundStartInSecs) {
		setType(TYPE);
		this.nextRoundId = nextRoundId;
		this.nextRoundStartInSecs = nextRoundStartInSecs;
	}

	public long getNextRoundId() {
		return nextRoundId;
	}

	public void setNextRoundId(long nextRoundId) {
		this.nextRoundId = nextRoundId;
	}

	public long getNextRoundStartInSecs() {
		return nextRoundStartInSecs;
	}

	public void setNextRoundStartInSecs(long nextRoundStartInSecs) {
		this.nextRoundStartInSecs = nextRoundStartInSecs;
	}

}
