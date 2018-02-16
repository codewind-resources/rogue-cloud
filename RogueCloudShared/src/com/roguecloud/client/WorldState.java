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

import com.roguecloud.map.IMap;

/** 
 * The current state of the world, as witnessed by the agent. 
 * 
 * This class may be used to answer questions like:
 * 
 * - What can my agent see in the world? 
 * - How large is the world? 
 * - How large is my agent's vision range (view width), and what are the world coordinates that it includes?
 * - How many frames (or ticks) has the game engine processed since the start of the round? 
 * 
 **/
public final class WorldState {

	long currentGameTick = 0;

	private IMap map;

	private int worldWidth;
	private int worldHeight;

	private int viewXPos;
	private int viewYPos;

	private int viewWidth;

	private int viewHeight;

	private int remainingSecondsInRound;
	
	public WorldState(IMap map) {
		this.map = map;
	}

	/** Return the agent's view of the game world. This map includes both tiles that the agent can currently see, as well 
	 * as tiles they have previously seen (the agent's "memory"). */
	public IMap getMap() {
		return map;
	}
	
	/** How many frames (ticks) has the game engine processed? This starts at 0 and increases monotonically, with no duplicates. */
	public long getCurrentGameTick() {
		return currentGameTick;
	}

	/** The top left x coordinate of the view rectangle box, of what the agent can see in the world. */
	public int getViewXPos() {
		return viewXPos;
	}

	/** The top left y coordinate of the view rectangle box, of what the agent can see in the world. */
	public int getViewYPos() {
		return viewYPos;
	}

	/** The overall height of the view of the world that the agent can see. */
	public int getViewHeight() {
		return viewHeight;
	}

	/** The overall width of the view of the world that the agent can see. */
	public int getViewWidth() {
		return viewWidth;
	}

	/** The width of the full world. */
	public int getWorldWidth() {
		return worldWidth;
	}

	/** The height of the full world. */
	public int getWorldHeight() {
		return worldHeight;
	}
	
	/** The number of seconds remaining in the round. */
	public int getRemainingSecondsInRound() {
		return remainingSecondsInRound;
	}
	
	// Internal methods only ---------------------------------------

	public void setWorldWidth(int worldWidth) {
		this.worldWidth = worldWidth;
	}

	public void setWorldHeight(int worldHeight) {
		this.worldHeight = worldHeight;
	}


	public void setViewYPos(int yPos) {
		this.viewYPos = yPos;
	}

	public void setViewHeight(int viewHeight) {
		this.viewHeight = viewHeight;
	}

	public void setViewWidth(int viewWidth) {
		this.viewWidth = viewWidth;
	}

	public void setViewXPos(int xPos) {
		this.viewXPos = xPos;
	}

	public void setCurrentGameTick(long currentGameTick) {
		this.currentGameTick = currentGameTick;
	}

	public void setRemainingSecondsInRound(int remainingSecondsInRound) {
		this.remainingSecondsInRound = remainingSecondsInRound;
	}

}
