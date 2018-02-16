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

public class JsonActionMessageResponse extends JsonAbstractTypedMessage {

	public static final String TYPE = "JsonActionMessageResponse"; 
	
	public JsonActionMessageResponse() {
		setType(TYPE);
	}
	
	private long messageId;
	
	public Object actionResponse;

	
	public long getMessageId() {
		return messageId;
	}
	
	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}
	
	public Object getActionResponse() {
		return actionResponse;
	}
	
	public void setActionResponse(Object actionResponse) {
		this.actionResponse = actionResponse;
	}
}
