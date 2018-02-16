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
import com.roguecloud.items.IObject;
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.events.JsonEquipActionEvent;

/** 
 * When a character equips an item (weapon or armour), this will be recorded as a world event within this class. 
 */
public class EquipActionEvent extends AbstractEvent {

	private final IObject equippedObject;
	private final ICreature creature;

	public EquipActionEvent(IObject equippedObject, ICreature creature, long frame, long id) {
		super(frame, id);
		if (equippedObject == null || creature == null) {
			throw new IllegalArgumentException();
		}

		this.equippedObject = equippedObject;
		this.creature = creature;
	}

	/** Returns the specific item that was equipped. */
	public IObject getEquippedObject() {
		return equippedObject;
	}
	
	/** Returns the specific creature that equipped the item. */
	public ICreature getCreature() {
		return creature;
	}

	@Override
	public EventType getActionType() {
		return EventType.EQUIP;
	}

	/** The coordinates on the map at which that the event occurred. */
	@Override
	public Position getWorldLocation() {
		return creature.getPosition();
	}

	/** Returns true if the specified creature was involved in this event, or false otherwise. */
	@Override
	public boolean isCreatureInvolved(ICreature creature) {

		return creature.equals(this.creature);
	}

	/** Return a list of all the creatures involved in the event. */
	@Override
	public ICreature[] getCreaturesInvolved() {
		return new ICreature[] { creature };
	}

	
	// Internal methods only -----------------------------------------------------------------
	
	@Override
	public JsonAbstractTypedMessage toJson() {
		JsonEquipActionEvent result = new JsonEquipActionEvent();
		result.setCreatureId(creature.getId());
		result.setObjectId(equippedObject.getId());
		result.setFrame(frame);
		result.setId(id);
		return result;
	}

}
