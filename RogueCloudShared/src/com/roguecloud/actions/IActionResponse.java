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

/** 
 * When an action (a creature is attacked, a potion is drank, a creature moves) is sent to the server, the server
 * will respond with whether or the action succeeded.
 * 
 * Actions can fail for a variety of reasons, for example:
 * 
 * - Attack actions can fail if the creature moved out of range.
 * - Move actions can fail if you tried to move into impassable terrain, such as a wall/building.
 * - Drink actions can fail if you try to drink the same potion twice.
 **/
public interface IActionResponse {
	
	/** Returns true if the action was successfully performed, false otherwise. */
	public boolean actionPerformed();

}
