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

import com.roguecloud.creatures.ICreature;

/** The current state of the player, which may be used to answer questions like:
 * 
 * - How much HP (life) do I have left?
 * - What positive/negative potion effects are active on my character? 
 * - What weapons/armour do I have equipped?
 * - What items are in my inventory?
 * 
 * To get your character's creature object, call getPlayer(...).
 **/
public class SelfState {

	private final ICreature player;
	
	public SelfState(ICreature player) {
		this.player = player;
	}
	
	/** Get an object representing the most recent state of the player character */
	public ICreature getPlayer() {
		return player;
	}
	
}
