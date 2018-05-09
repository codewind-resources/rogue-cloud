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

public class JsonClientConnectResponse extends JsonAbstractTypedMessage {

	public static final String TYPE = "ClientConnectResponse";
	
	public static enum ConnectResult { SUCCESS, FAIL_INVALID_CREDENTIALS, FAIL_ROUND_OVER, FAIL_ROUND_NOT_STARTED, FAIL_OTHER, FAIL_INVALID_CLIENT_API_VERSION }
	
	private String connectResult;
	
	private Long roundEntered;
	
	public JsonClientConnectResponse() {
		setType(TYPE);
	}
	
	public JsonClientConnectResponse(String connectResult, Long roundEntered) {
		setType(TYPE);
		this.connectResult = connectResult;
		this.roundEntered = roundEntered;
	}	
	
	public String getConnectResult() {
		return connectResult;
	}
	
	public void setConnectResult(String connectResult) {
		this.connectResult = connectResult;
	}
	
	public Long getRoundEntered() {
		return roundEntered;
	}
	
	public void setRoundEntered(Long roundEntered) {
		this.roundEntered = roundEntered;
	}
}
