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

package com.roguecloud.server;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.JsonActionMessage;
import com.roguecloud.json.JsonMessageMap;
import com.roguecloud.json.actions.JsonCombatAction;
import com.roguecloud.json.actions.JsonDrinkItemAction;
import com.roguecloud.json.actions.JsonEquipAction;
import com.roguecloud.json.actions.JsonMoveInventoryItemAction;
import com.roguecloud.json.actions.JsonNullAction;
import com.roguecloud.json.actions.JsonStepAction;
import com.roguecloud.json.client.JsonHealthCheckResponse;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;

public final class ServerMessageReceiver {
	
	private static final Logger log = Logger.getInstance();

	private final LogContext logContext;
	
	public ServerMessageReceiver(LogContext logContext) {
		this.logContext = logContext;
	}
	
	public void receiveMessage(String str, ActiveWSClientSession session,  ActiveWSClient client) {
		log.info("Received message: "+str , client.getLogContext());
		
		ObjectMapper om = new ObjectMapper();
		String messageType;
		try {
			messageType = (String) om.readValue(str, Map.class).get("type");

			Class<?> c = JsonMessageMap.getMap().get(messageType);

			Object o = om.readValue(str, c);
			
			if(messageType.equals(JsonActionMessage.TYPE)) {
				receiveMessage((JsonActionMessage)o, session, client);
				
			} else if(messageType.equals(JsonHealthCheckResponse.TYPE)) {
				receiveMessage((JsonHealthCheckResponse)o, session, client);
				
			} else {
				log.severe("Unrecognized message: "+str, logContext);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void receiveMessage(JsonHealthCheckResponse o, ActiveWSClientSession session, ActiveWSClient client) {
		client.informReceivedHealthCheckResponse(o.getId());
	}

	@SuppressWarnings("rawtypes")
	private void receiveMessage(JsonActionMessage jam, ActiveWSClientSession session, ActiveWSClient client) {
		
		Map map = (Map) jam.getAction();
		
		String type = (String) map.get("type");
		
		JsonAbstractTypedMessage action = null;
		
		if(type.equals(JsonCombatAction.TYPE)) {
			
			action = new JsonCombatAction(map);
			
		} else if(type.equals(JsonStepAction.TYPE)) {
			
			action = new JsonStepAction(map);
			
		} else if(type.equals(JsonNullAction.TYPE)) {
			
			action = new JsonNullAction();
			
		} else if(type.equals(JsonMoveInventoryItemAction.TYPE)) {
			
			action = new JsonMoveInventoryItemAction(map);
			
		} else if(type.equals(JsonEquipAction.TYPE)) {
			
			action = new JsonEquipAction(map);
			
		} else if(type.equals(JsonDrinkItemAction.TYPE)) {
			action = new JsonDrinkItemAction(map);
		} else {
			log.severe("Unrecognized Json message type", logContext);
			action = null;
		}
		
		if(action != null) {
			client.addAction(action, jam.getMessageId());
		}
		
	}
		
}
