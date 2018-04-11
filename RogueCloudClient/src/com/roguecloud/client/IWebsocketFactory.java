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

package com.roguecloud.client;

/**
 * This is the entry point for the client WebSocket API. This interface is a factory method for the creation of
 * a connection to a remote Rogue Cloud server. 
 * 
 * This interface also allows Rogue Cloud to support multiple implementations of the Java-EE WebSocket API, including:
 * 
 * - WebSphere Liberty/OpenLiberty's WebSocket implementation (for use when running inside Liberty)
 * - Tyrus Java-EE WebSocket reference implementation (for use when running outside Liberty)
 *
 *
 * For internal use only.
 */
public interface IWebsocketFactory {
	
	/** Create a session wrapper, which will be used to connect to a remote Rogue Cloud server. See ISessionWrapper
	 * for details.*/
	ISessionWrapper createSessionWrapper(ClientState cs);
}
