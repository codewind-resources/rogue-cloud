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
import com.roguecloud.json.events.JsonMoveInventoryItemActionEvent;

/**
 * When a creature picks up an item from the ground, or drops an item to the ground, this will be recorded
 * in this class as a world event.
 */
public class MoveInventoryItemActionEvent extends AbstractEvent {

	private final ICreature creature;
	private final IObject object;
	private final boolean dropItem;

	public MoveInventoryItemActionEvent(ICreature creature, IObject object, boolean dropItem, long frame, long id) {
		super(frame, id);
		this.creature = creature;
		this.object = object;
		this.dropItem = dropItem;
	}

	/** The creature that picked up or drooped the item. */
	public ICreature getCreature() {
		return creature;
	}
	
	/** The object that was picked up or dropped. */
	public IObject getObject() {
		return object;
	}
	
	/** Returns true if the character was dropping an item, and false if they were picking up an item  */
	public boolean isDropItem() {
		return dropItem;
	}
	
	@Override
	public EventType getActionType() {
		return EventType.MOVE_INVENTORY_ITEM;
	}

	/** The coordinates on the map at which that the event occurred. */
	@Override
	public Position getWorldLocation() {
		return creature.getPosition();
	}

	/** Returns true if the specified creature was involved in this event, or false otherwise. */
	@Override
	public boolean isCreatureInvolved(ICreature creature) {
		return this.creature.equals(creature);
	}

	/** Return a list of all the creatures involved in the event. */
	@Override
	public ICreature[] getCreaturesInvolved() {
		return new ICreature[] { creature };
	}

	// Internal methods ----------------------------------------------------------------------------
	
	@Override
	public JsonAbstractTypedMessage toJson() {
		JsonMoveInventoryItemActionEvent result = new JsonMoveInventoryItemActionEvent();
		result.setCreatureId(creature.getId());
		result.setDropItem(dropItem);
		result.setFrame(frame);
		result.setId(id);
		result.setObjectId(object.getId());
		return result;
	}

}
