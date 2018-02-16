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

package com.roguecloud.actions;

import com.roguecloud.Position;
import com.roguecloud.json.actions.JsonStepActionResponse;

/**
 * This object is returned when the player's code attempts to move their character.
 * 
 * This action can fail for the following reasons:
 * 
 * - An attempt was made to move more than one tile at a time: you can only move a single square (tile) per turn. 
 * - An attempt was made to move diagonally: you can only move up, down, left, or right.
 * - An attempt was made to move into impassable terrain: you cannot move into squares that contain walls/buildings or other impassable terrain.
 * 
 * Tips: 
 * 
 * - To find the path from your current position to another point on the map, consider using FastPathSearch or AStarSearch.
 * 
 * See the IActionResponse class for more information on action responses.
 *  
 **/
public class StepActionResponse implements IActionResponse {
	
	/** If the step action failed, this will indicate the reason. */
	public static enum StepActionFailReason { BLOCKED, OTHER };
	
	private final StepActionFailReason failReason;
	
	private final Position newPosition;
	
	private final boolean success;

	public StepActionResponse(Position newPosition) {
		this.success = true;
		this.newPosition = newPosition;
		this.failReason = null;
	}
	
	public StepActionResponse(StepActionFailReason failReason) {
		this.success = false;
		this.newPosition = null;
		this.failReason = failReason;
	}
	
	public StepActionFailReason getFailReason() {
		return failReason;
	}

	/** If the step succeeded, what is our new character position? */
	public Position getNewPosition() {
		return newPosition;
	}
	

	@Override
	public boolean actionPerformed() {
		return success;
	}

	// Internal Methods -----------------------------------------------------------------
	
	public JsonStepActionResponse toJson() {
		JsonStepActionResponse result = new JsonStepActionResponse();
		result.setFailReason(failReason != null ? failReason.name() : null);
		result.setNewPosition(this.newPosition != null ? this.newPosition.toJson() : null);
		result.setSuccess(this.success);
		
		return result;
	}

}
