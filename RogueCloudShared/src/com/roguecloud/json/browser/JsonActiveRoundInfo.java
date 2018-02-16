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

public class JsonActiveRoundInfo extends JsonAbstractTypedMessage {

	public static final String TYPE = "ACTIVE";

	long roundId;
	long timeLeftInSeconds;

	public JsonActiveRoundInfo() {
		setType(TYPE);
	}

	public JsonActiveRoundInfo(long roundId, long timeLeftInSeconds) {
		setType(TYPE);
		this.roundId = roundId;
		this.timeLeftInSeconds = timeLeftInSeconds;
	}

	public long getRoundId() {
		return roundId;
	}

	public void setRoundId(long roundId) {
		this.roundId = roundId;
	}

	public long getTimeLeftInSeconds() {
		return timeLeftInSeconds;
	}

	public void setTimeLeftInSeconds(long timeLeftInSeconds) {
		this.timeLeftInSeconds = timeLeftInSeconds;
	}

}
