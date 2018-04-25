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

import java.io.File;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roguecloud.ActionResponseFuture;
import com.roguecloud.actions.CombatAction;
import com.roguecloud.actions.DrinkItemAction;
import com.roguecloud.actions.EquipAction;
import com.roguecloud.actions.IAction;
import com.roguecloud.actions.IAction.ActionType;
import com.roguecloud.actions.IActionResponse;
import com.roguecloud.actions.MoveInventoryItemAction;
import com.roguecloud.actions.NullAction;
import com.roguecloud.actions.StepAction;
import com.roguecloud.client.utils.PersistentKeyValueStore;
import com.roguecloud.json.JsonActionMessage;
import com.roguecloud.json.actions.JsonCombatAction;
import com.roguecloud.json.actions.JsonDrinkItemAction;
import com.roguecloud.json.actions.JsonEquipAction;
import com.roguecloud.json.actions.JsonMoveInventoryItemAction;
import com.roguecloud.json.actions.JsonNullAction;
import com.roguecloud.json.actions.JsonStepAction;
import com.roguecloud.json.client.JsonHealthCheck;
import com.roguecloud.json.client.JsonHealthCheckResponse;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;

/** 
 * This class encompasses all state data that is used to provide support for the API 
 * that the player uses to play the game (IClient/RemoteClient), and also handles the lifecycle of a player's individual AI character. 
 * 
 * A single instance of this class is used per round, and disposed of at the end of the round, unless 
 * the player dies before the round is over (as is often the case).
 * 
 * Whenever a player dies, this class enters an 'interrupted' state, which in gameplay terms means that the character's microservice
 * has restarted, which prevents them from using any previous unsaved state data. For more information on what happens on character
 * death, and how to code around it, see the game wiki.
 * 
 * After being interrupted (due to player character death), the instance of this class will be dispose()d of, and a new instance of this
 * class will be created and told to enter the same round. Thus even when your character dies, your player AI code still continues playing
 * in the same round it died in, but it is now running in a new "context" without any previously saved data.
 * 
 * This class is for internal server-side use only. 
 **/
public class ClientState {	
	
	private static final Logger log = Logger.getInstance();
	
	private final String username;
	
	private final String password;
	
	private final String uuid;
	
	private final Object lock = new Object();
	
	private final ClientMessageReceiver messageReceiver;

	private final LogContext logContext;
	
	private final ClientWorldState clientWorldState;
	
	private final RemoteClient remoteClient;

	/* Messages to the server have a unique message ID; this counter keeps track of the next one */
	private long nextSynchronousMessageId_synch_lock = 0; 
	
	/** When we send actions to the server, we expect a response w/ the original message ID; this keeps track
	 * of actions we are waiting for responses for. */
	private final HashMap<Long /* synchronous message id*/, ActionResponseFuture> mapMessageIdToFuture_synch = new HashMap<>();
	
	private final ISessionWrapper sessionWrapper;
	
	private Long currentRound_synch_lock;
	
	private boolean roundComplete_synch_lock = false;
	
	/** If the player dies, their agent is restarted;*/
	private boolean clientInterrupted_synch_lock = false;
	
	private final IWebsocketFactory factory;

	private Integer nextRoundInXSeconds_synch_lock;
	
	private final PersistentKeyValueStore keyValueStore;
	
	private final int numberOfTimesPreviouslyInterrupted;
	
	public ClientState(String username, String password, String uuid, RemoteClient remoteClient, IWebsocketFactory websocketFactory, int numberOfTimesPreviouslyInterrupted) {
		this.factory = websocketFactory;
		this.username = username;
		this.password = password;
		this.uuid = uuid;
		this.messageReceiver = new ClientMessageReceiver();
		this.logContext = LogContext.client(uuid);
		this.clientWorldState = new ClientWorldState(this, logContext);
		this.remoteClient = remoteClient;
		
		this.sessionWrapper = factory.createSessionWrapper(this);
		if(this.sessionWrapper == null) {
			throw new IllegalStateException("Unable to create websocket session.");
		}
		
		this.keyValueStore = new PersistentKeyValueStore(new File(new File(System.getProperty("user.home"), ".roguecloud"), "client-store"));
		
		this.numberOfTimesPreviouslyInterrupted = numberOfTimesPreviouslyInterrupted;
		
	}
	
	
	public void initialConnect(String url) {
		this.sessionWrapper.initialConnect(url);
	}

	public boolean isClientInterrupted() {
		synchronized (lock) {
			return clientInterrupted_synch_lock;			
		}
	}
	
	public void informInterrupted(long round, int interrupt) { 
		synchronized(lock) {
			// Ignore the interrupt if the round doesn't match.
			if(currentRound_synch_lock == null || currentRound_synch_lock != round) {
				return;
			}
			
			// Ignore if we have already signaled an interrupt
			if(clientInterrupted_synch_lock == true) {
				return;
			}
			
			if(interrupt > numberOfTimesPreviouslyInterrupted) {
				clientInterrupted_synch_lock = true;
			}
		}		
	}
	
	public void informRoundIsComplete(int nextRoundInXSeconds) {
		
		synchronized(lock) {
			this.nextRoundInXSeconds_synch_lock = nextRoundInXSeconds;

			if(roundComplete_synch_lock != true) {
				log.interesting("ClientState was first informed that round is complete: "+currentRound_synch_lock, logContext);
				
				roundComplete_synch_lock = true;
			}
		}

		clientWorldState.informRoundComplete(nextRoundInXSeconds);
		
	}
	
	public Integer getNextRoundInXSeconds() {
		synchronized(lock) {
			return nextRoundInXSeconds_synch_lock;
		}
	}
	
	public LogContext getLogContext() {
		return logContext;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public RemoteClient getRemoteClient() {
		return remoteClient;
	}
	
	public ClientMessageReceiver getMessageReceiver() {
		return messageReceiver;
	}

	public ClientWorldState getClientWorldState() {
		return clientWorldState;
	}
	
	public long getNextSynchronousMessageId() {
		synchronized(lock) {
			return nextSynchronousMessageId_synch_lock;
		}
	}
	
	public long getAndIncrementNextSynchronousMessageId() {
		synchronized(lock) {
			return nextSynchronousMessageId_synch_lock++;
		}
	}
	
	public boolean isRoundComplete() {
		synchronized(lock) {
			return roundComplete_synch_lock;
		}
	}
	
	public Long getCurrentRound() {
		synchronized(lock) {
			return currentRound_synch_lock;
		}
	}
	
	public void setCurrentRound(long round) {
		synchronized(lock) {
			currentRound_synch_lock = round;
		}
	}

	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void dispose() {
		
		clientWorldState.dispose();
		remoteClient.internalDispose();
		synchronized(mapMessageIdToFuture_synch) {
			mapMessageIdToFuture_synch.clear();
		}
				
		try {
			sessionWrapper.dispose();
		} catch(Exception e) {
			/* ignore*/
		}
		
	}
	
	public void processHealthCheck(JsonHealthCheck o) {
		
		JsonHealthCheckResponse response = new JsonHealthCheckResponse(o.getId());
		
		ObjectMapper om = new ObjectMapper();
		try {
			sessionWrapper.write(om.writeValueAsString(response));
		} catch (JsonProcessingException e) {
			log.severe("Unexpected exception", e, null);
			e.printStackTrace();
		}
	}

	
	public void forwardActionResponseToClient(long messageId, IActionResponse response) {
		
		ActionResponseFuture arf;
		synchronized(mapMessageIdToFuture_synch) {
			arf = mapMessageIdToFuture_synch.get(messageId);
			mapMessageIdToFuture_synch.remove(messageId);
		}
		
		if(arf == null) {
			// TODO: LOW - Uncomment this and investigate.
//			log.severe("Unable to find message id in message id map:"+messageId+" "+response.getClass().getName(), logContext);
			return;
		}
		
		arf.internalSetResponse(response);
		
	}
	
	public ActionResponseFuture sendAction(IAction action) throws JsonProcessingException {
		
		long id = getAndIncrementNextSynchronousMessageId();
		
		JsonActionMessage jam = new JsonActionMessage();
		jam.setMessageId(id);
		
		if(action.getActionType() == ActionType.COMBAT) {
			JsonCombatAction jca = ((CombatAction)action).toJson();
			jam.setAction(jca);
			
		} else if(action.getActionType() == ActionType.NULL) {
			JsonNullAction jna = ((NullAction)action).toJson();
			jam.setAction(jna);
			
		} else if(action.getActionType() == ActionType.STEP) {
			JsonStepAction jsa  = ((StepAction)action).toJson();
			jam.setAction(jsa);
			
		} else if(action.getActionType() == ActionType.MOVE_INVENTORY_ITEM) {
			JsonMoveInventoryItemAction jpuia = ((MoveInventoryItemAction)action).toJson();
			jam.setAction(jpuia);
			
		} else if(action.getActionType() == ActionType.EQUIP) {
			JsonEquipAction jea = ((EquipAction)action).toJson();
			jam.setAction(jea);
			
		} else if(action.getActionType() == ActionType.DRINK) {
			JsonDrinkItemAction jda = ((DrinkItemAction)action).toJson();
			jam.setAction(jda);
			
		} else {
			log.severe("Unrecognized action type: "+action, logContext);
			throw new IllegalArgumentException("Unrecognized action type"+action.getActionType());
		}
		
		ObjectMapper om = new ObjectMapper();
		
		synchronized (mapMessageIdToFuture_synch) {
			
			ActionResponseFuture arf = new ActionResponseFuture();
			arf.internalSetMessageId(id);
			
			mapMessageIdToFuture_synch.put(id, arf);
			
			String str = om.writeValueAsString(jam);
			
			sessionWrapper.write(str);
						
			return arf;
		}
	}
	
	
	public IKeyValueStore getKeyValueStore() {
		return keyValueStore;
	}
		
}
