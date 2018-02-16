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

import com.roguecloud.json.JsonActionMessageResponse;
import com.roguecloud.json.JsonClientInterrupt;
import com.roguecloud.json.JsonFrameUpdate;
import com.roguecloud.json.JsonRoundComplete;
import com.roguecloud.json.browser.JsonUpdateBrowserUI;
import com.roguecloud.json.client.JsonHealthCheck;
import com.roguecloud.utils.Logger;

/** For internal use only */
public class ClientMessageReceiver {
	
	private static final Logger log = Logger.getInstance();
	

	public ClientMessageReceiver() {
	}
	
	
	// Called by endpoint message handler
	public void receiveJson(String messageType, Object o, String str, Session s, ClientState parent) {

		try {
			
			if(messageType.equals(JsonFrameUpdate.TYPE) ) {
				receiveMessage((JsonFrameUpdate)o, str, parent);
				
			} else if(messageType.equals(JsonActionMessageResponse.TYPE)) {
				receiveMessage((JsonActionMessageResponse)o, parent);
				
			} else if(messageType.equals(JsonRoundComplete.TYPE)) {
				receiveMessage((JsonRoundComplete)o, parent);
				
			} else if(messageType.equals(JsonUpdateBrowserUI.TYPE)) {
				receiveMessage((JsonUpdateBrowserUI)o, parent);
				
			} else if(messageType.equals(JsonHealthCheck.TYPE) ) {
				receiveMessage((JsonHealthCheck)o, parent);
				
			} else if(messageType.equals(JsonClientInterrupt.TYPE) ) {
				receiveMessage((JsonClientInterrupt)o, parent);
			} else {
				log.severe("Unrecognized JSON message type: "+str, parent.getLogContext());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			log.severe("Exception on receive json: "+str, e, null);
		}
		
	}

	
	private void receiveMessage(JsonClientInterrupt o, ClientState parent) {
		parent.informInterrupted(o.getRound(), o.getInterruptNumber());
	}

	private void receiveMessage(JsonHealthCheck o, ClientState parent) {
		parent.processHealthCheck(o);
	}


	public void receiveMessage(JsonFrameUpdate frameUpdate, String jsonText, ClientState state) {
		
		state.getClientWorldState().receiveFrameUpdate(frameUpdate);
	}

	public void receiveMessage(JsonActionMessageResponse o, ClientState state) {
		
		state.getClientWorldState().processActionResponse(o);
		
	}

	public void receiveMessage(JsonRoundComplete o, ClientState state) {
		state.informRoundIsComplete(o.getNextRoundInSeconds());
	}

	public void receiveMessage(JsonUpdateBrowserUI o, ClientState state) {
		
		state.getClientWorldState().receiveBrowserUIUpdate(o);
		
	}
	
}
