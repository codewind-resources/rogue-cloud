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

import javax.websocket.Session;

/** 
 * The primary purpose of this interface (and the classes that implement it) is to provide uninterrupted WebSocket communication
 * between the client and server, by detecting when the WebSocket connection has unnaturally broken/disconnected and 
 * attempting to re-establish it as needed. Thus other classes that call this class do not need to handle connection interruption,
 * as this class hides this re-establishing process as an implementation detail. 
 * 
 * This interface also allows Rogue Cloud to support multiple implementations of the Java-EE WebSocket API, including:
 * 
 * - WebSphere Liberty/OpenLiberty's WebSocket implementation (for use when running inside Liberty)
 * - Tyrus Java-EE WebSocket reference implementation (for use when running outside Liberty)
 * 
 * This class is for internal server use only.
 **/
public interface ISessionWrapper  {
	
	/** Connect to a WebSocket at the specific URL */
	void initialConnect(String url);

	/** Write a (usually JSON) message to the WebSokcet */
	void write(String str);

	/** This method is called when a message is received on the WebSocket wrapped by this class */
	void receiveJson(String str, Session session);

	/** Close the WebSocket and dispose of any other class resources*/
	void dispose();

	/** Whether or not the class has completed the dispose lifecycle.*/
	boolean isDisposed();

}
