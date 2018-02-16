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

package com.roguecloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.roguecloud.creatures.IMutableCreature;

public class EngineWebsocketState {

	private int width = -1;
	private int height = -1;
	private int currClientWorldX = -1;
	private int currClientWorldY = -1;

//	private int newClientWorldX = 0;
//	private int newClientWorldY = 0;

	private int nextFrame = 1;

	private IMutableCreature playerCreature;

	private final HashMap<Long /* object id */, Boolean> objectSeenByPlayer = new HashMap<>();

	private final HashMap<Long /* message id */, Boolean> mapReceivedMessageIds = new HashMap<>();

	private final HashMap<Long, /* message id */ String> mapResponseToMessage = new HashMap<>();

	// private boolean fullSent = false;

	public EngineWebsocketState() {
	}

	public IMutableCreature getPlayerCreature() {
		return playerCreature;
	}

	public void setPlayerCreature(IMutableCreature playerCreature) {
		this.playerCreature = playerCreature;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getCurrClientWorldX() {
		return currClientWorldX;
	}

	public void setCurrClientWorldX(int currClientWorldX) {
		this.currClientWorldX = currClientWorldX;
	}

	public int getCurrClientWorldY() {
		return currClientWorldY;
	}

	public void setCurrClientWorldY(int currClientWorldY) {
		this.currClientWorldY = currClientWorldY;
	}
	
	public void clearClientState() {
		objectSeenByPlayer.clear();
		mapReceivedMessageIds.clear();
		mapResponseToMessage.clear();
	}

	// public boolean isFullSent() {
	// return fullSent;
	// }
	//
	// public void setFullSent(boolean fullSent) {
	// this.fullSent = fullSent;
	// }

	public int getNextFrame() {
		return nextFrame;
	}

	public void setNextFrame(int nextFrame) {
		this.nextFrame = nextFrame;
	}


	
	
	public void putObjectSeenByPlayer(long id) {
		objectSeenByPlayer.put(id, true);
	}

	public boolean isObjectSeenByPlayer(long id) {
		return objectSeenByPlayer.get(id) != null;
	}

	public void putResponseToMessage(long messageId, String response) {
		mapResponseToMessage.put(messageId, response);

		if (messageId % 100 == 0) {
			for (Iterator<Entry<Long, String>> it = mapResponseToMessage.entrySet().iterator(); it.hasNext();) {
				Long l = it.next().getKey();
				if (l < messageId - 600) {
					it.remove();
				}
			}
		}

	}

	public boolean isMessageIdReceived(long messageId) {
		return mapReceivedMessageIds.containsKey((Long) messageId);
	}

	public void addMessageIdReceived(long messageId) {

		/** Every 100 messages received: Remove messages older than messageId-600 */
		if (messageId % 100 == 0) {
			for (Iterator<Entry<Long, Boolean>> it = mapReceivedMessageIds.entrySet().iterator(); it.hasNext();) {
				Long l = it.next().getKey();
				if (l < messageId - 600) {
					it.remove();
				}
			}
		}

		mapReceivedMessageIds.put((Long) messageId, true);
	}

	public List<String> getActionMessagesWithIdsGreaterThan(long id) {
		List<String> result = new ArrayList<>();
		for (Iterator<Entry<Long, String>> it = mapResponseToMessage.entrySet().iterator(); it.hasNext();) {
			Entry<Long, String> e = it.next();
			if (e.getKey() > id) {
				result.add(e.getValue());
			}
		}

		return result;
	}


}
