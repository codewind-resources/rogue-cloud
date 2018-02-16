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
import com.roguecloud.items.DrinkableItem;
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.events.JsonDrinkItemActionEvent;

/** when a character drinks a potion, this is recorded in this class as a world event. 
 * You can see who drank what, where they were, and what the effect was. */
public class DrinkItemActionEvent extends AbstractEvent {

	private final ICreature creature;
	private final DrinkableItem di;
	
	public DrinkItemActionEvent(ICreature creature, DrinkableItem di, long frame, long id) {
		super(frame, id);
		if(creature == null || di == null) { throw new IllegalArgumentException(); }
		
		this.creature = creature;
		this.di = di;
	}
	
	@Override
	public EventType getActionType() {
		return EventType.DRINK;
	}

	
	/** The coordinates on the map at which the event occurred. */
	@Override
	public Position getWorldLocation() {
		return creature.getPosition();
	}

	
	/** Return the item that was drank by the character. */
	public DrinkableItem getDrinkableItem() {
		return di;
	}
	
	/** Get the creature that drank the item. */
	public ICreature getCreature() {
		return creature;
	}

	/** Returns true if the specified creature was involved in this event, or false otherwise. */
	@Override
	public boolean isCreatureInvolved(ICreature creature) {
		if(creature == null) { throw new IllegalArgumentException(); }
		
		if(creature.equals(this.creature)) {
			return true;
		}
		
		return false;
		
	}

	/** Return a list of all the creatures involved in the event. */
	@Override
	public ICreature[] getCreaturesInvolved() {
		return new ICreature[] { creature };
	}

	// Internal methods ---------------------------------------
	
	@Override
	public JsonAbstractTypedMessage toJson() {
		JsonDrinkItemActionEvent result = new JsonDrinkItemActionEvent();
		result.setFrame(frame);
		result.setId(id);
		result.setPlayerId(creature.getId());
		result.setObjectId(di.getId());
		
		return result;
	}


}
