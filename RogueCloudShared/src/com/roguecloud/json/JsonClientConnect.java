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

public class JsonClientConnect extends JsonAbstractTypedMessage {
	
	public static final String TYPE = "ClientConnect"; 

	private String uuid;
	
	private String username;
	
	private String password;
	
	private String clientVersion;
	
	private Long roundToEnter;

	Long lastActionResponseReceived = null;
	
	private boolean initialConnect = false;

	
	public JsonClientConnect() {
		setType(TYPE);
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getClientVersion() {
		return clientVersion;
	}
	
	public void setClientVersion(String clientVersion) {
		this.clientVersion = clientVersion;
	}

	public Long getLastActionResponseReceived() {
		return lastActionResponseReceived;
	}
	
	public void setLastActionResponseReceived(Long lastActionResponseReceived) {
		this.lastActionResponseReceived = lastActionResponseReceived;
	}
	
	public Long getRoundToEnter() {
		return roundToEnter;
	}
	
	public void setRoundToEnter(Long roundToEnter) {
		this.roundToEnter = roundToEnter;
	}

	public void setInitialConnect(boolean initialConnect) {
		this.initialConnect = initialConnect;
	}
	
	public boolean isInitialConnect() {
		return initialConnect;
	}
}
