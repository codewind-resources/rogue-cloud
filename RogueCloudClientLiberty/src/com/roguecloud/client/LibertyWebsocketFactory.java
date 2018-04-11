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

import com.roguecloud.client.ClientState;
import com.roguecloud.client.ISessionWrapper;
import com.roguecloud.client.IWebsocketFactory;

/** This class provides a session wrapper which is implemented using the Liberty WebSocket implementation.
 * 
 * See IWebsocketFactory for details. */
public class LibertyWebsocketFactory implements IWebsocketFactory {

	@Override
	public ISessionWrapper createSessionWrapper(ClientState cs) {
		LibertySessionWrapper result = new LibertySessionWrapper(cs);
		return result;
	}

}
