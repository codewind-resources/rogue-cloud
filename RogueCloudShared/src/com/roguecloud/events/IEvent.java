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

package com.roguecloud.events;

import com.roguecloud.Position;
import com.roguecloud.creatures.ICreature;

/** This object corresponds to an event that occurred in the world, such as an attack, a character move, the drinking of a potion, etc. 
 * 
 * The following classes implement this interface:
 * 
 * - StepActionEvent (EventType of STEP)
 * - CombatActionEvent (EventType of COMBAT)
 * - DrinkItemActionEvent (EventType of DRINK)
 * - EquipActionEvent (EventType of EQUIP)
 * - MoveInventoryItemActionEvent (EventType of MOVE_INVENTORY_ITEM) 
 * */
public interface IEvent {
	
	/** All the type of events that may occur in the world, corresponding to player/creature actions. */
	public static enum EventType { STEP, COMBAT, DRINK, EQUIP, MOVE_INVENTORY_ITEM };
	
	/** What type of event is it? This will match one of the event types in the com.roguecloud.events package. */	
	public EventType getActionType();

	/** What specific tile did this event occur at? (For example, where was the character when they were attacked?) */
	public Position getWorldLocation();
	
	/** Which frame (also known as a tick) of the game did the event occur at? */
	public long getFrame();
	
	/** Was the specified creature involed with this event? Returns true if the character was involed (attacked/defended/moved/etc), or false otherwise.  */
	public boolean isCreatureInvolved(ICreature creature);

	/** Return all creatures involved in this event: for an attack event this is both the attacker/defender, while for other actions this will only
	 * include the character that performed the action. */
	public ICreature[] getCreaturesInvolved();
	
}
