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

public class JsonFrameUpdate extends JsonAbstractTypedMessage {
	
	public static final String TYPE = "JsonFrameUpdate";

	long gameTicks;
	
	long frame;
	
	JsonSelfState selfState;

	JsonWorldState worldState;
	
	/** Whether or not the frame update is a full frame
	 * Note: even if this is false, the frame may still be a full frame. However, if it is true, then the frame is always full. */
	boolean isFull = false;

	
	public JsonFrameUpdate() {
		setType(TYPE);
	}
	
	
	public JsonSelfState getSelfState() {
		return selfState;
	}

	public void setSelfState(JsonSelfState selfState) {
		this.selfState = selfState;
	}

	public JsonWorldState getWorldState() {
		return worldState;
	}

	public void setWorldState(JsonWorldState worldState) {
		this.worldState = worldState;
	}
	
	public long getFrame() {
		return frame;
	}
	
	public void setFrame(long frame) {
		this.frame = frame;
	}

	public long getGameTicks() {
		return gameTicks;
	}
	
	public void setGameTicks(long gameTicks) {
		this.gameTicks = gameTicks;
	}
	
	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}
	
	public boolean isFull() {
		return isFull;
	}
}
