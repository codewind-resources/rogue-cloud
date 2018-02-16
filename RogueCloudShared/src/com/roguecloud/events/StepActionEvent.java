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
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.events.JsonStepActionEvent;

/** 
 * When a creature moves from one position to another, this move will be recorded by this class.
 **/
public class StepActionEvent extends AbstractEvent {

	final ICreature creature;

	final Position from;
	final Position to;
	
	public StepActionEvent(ICreature creature, Position from, Position to, long frame, long id) {
		super(frame, id);
		
		if(creature == null || from == null || to == null) { throw new IllegalArgumentException(); }
		
		this.creature = creature;
		this.from = from;
		this.to = to;
	}

	@Override
	public EventType getActionType() {
		return EventType.STEP;
	}

	public ICreature getCreature() {
		return creature;
	}

	/** The source position */
	public Position getFrom() {
		return from;
	}

	/** The destination position (usually up, down, left or right of the source position )*/
	public Position getTo() {
		return to;
	}

	/** The coordinates on the map at which that the event occurred. */
	@Override
	public Position getWorldLocation() {
		return to;
	}
	
	@Override
	public String toString() {
		return "("+id+") "+creature+" "+from+" -> "+to+" @ "+frame;
	}

	/** Returns true if the specified creature was involved in this event, or false otherwise. */
	@Override
	public boolean isCreatureInvolved(ICreature creature) {
		if(creature == null) { throw new IllegalArgumentException(); }
		
		return this.creature.equals(creature);
	}

	/** Return a list of all the creatures involved in the event. */
	@Override
	public ICreature[] getCreaturesInvolved() {
		return new ICreature[] {  creature};
	}

	// Internal methods ------------------------------------------------------------------------------
	
	@Override
	public JsonAbstractTypedMessage toJson() {
		JsonStepActionEvent event = new JsonStepActionEvent();
		event.setCreatureId(creature.getId());
		event.setFrom(from.toJson());
		event.setTo(to.toJson());
		event.setEventId(id);
		event.setFrame(frame);
		return event;
	}


}
