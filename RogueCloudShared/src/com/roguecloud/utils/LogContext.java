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

package com.roguecloud.utils;

/**
 * An immutable, ubiquitous object which indicates the context in which a log statement is written, for example:
 * - Which instance of a server is writing the log statement
 * - What the UUID and ID of the client are.
 * 
 * This kind of context can be helpful when examining logs containing multiple simultaneous entities working independently
 * on different threads.
 * 
 * This class is an internal class, for server use only. 
 */
public class LogContext {
	
	private int serverInstance = -1;
	
	private long clientId = -1;
	
	private String clientUuid = null;
	
	
	private LogContext() {

	}
	
	public static LogContext client(String clientUuid) {
		LogContext lc = new LogContext();
		lc.clientUuid = clientUuid;
		return lc;
	}
	
	public static LogContext serverInstance(int serverInstance) {
		LogContext lc = new LogContext();
		lc.serverInstance = serverInstance;
		return lc;
	}
	
	public static LogContext serverInstanceWithClientId(int serverInstance, long clientId) {
		LogContext lc = new LogContext();
		lc.serverInstance = serverInstance;
		lc.clientId = clientId;
		return lc;
	}

	public String getContext() {
		StringBuilder sb = new StringBuilder();
		
		if(clientUuid != null) {
			sb.append("[client: "+clientUuid+"] ");
		}
		
		if(serverInstance != -1) {
			sb.append("[server: "+serverInstance+"] ");
		}
		
		if(clientId != -1) {
			sb.append("[clientId:"+clientId+"]");			
		}
				
		return sb.toString().trim();
	}
	
	public String getClientUuid() {
		return clientUuid;
	}
	
	@Override
	public String toString() {
		return getContext();
	}
	
}
