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

import java.util.List;

import com.roguecloud.creatures.ICreature;
import com.roguecloud.events.IEvent;

/** This class may be used to retrieve all the events that occurred in the last one or more game engine frames. 
 * 
 * This class may be used to answer questions such as:
 * 
 * - Who just attacked me?
 * - Did someone just loot that item from the ground?
 * - Who is attacking other creatures around me?
 * - When was the last time I was attacked?
 * - Are other creatures moving closer to me?
 * 
 * Events in the event log will only include events that were directly witnessed by the agent themselves. Events that 
 * occur "off screen" will thus not be available. 
 * 
 * Implementations of this interface are thread safe.
 **/
public interface IEventLog {
	
	/** Return a list of all events in the log. Note this may not return all game events, depending on memory constraints. */
	public List<IEvent> getAllEvents();
	
	/** Return a list of all the events that occurred in the last turn that involved a specific creature (us). */
	public List<IEvent> getLastTurnSelfEvents(ICreature c, WorldState worldState);
	
	/** Return a list of all the events that involved a specific creature (us), that occurred in the last X turns. */
	public List<IEvent> getLastXTurnsSelfEvents(int x, ICreature us, WorldState worldState);
	
	/** Return a list of all the events that occurred in the last turn (Note this only includes those that were visible to the agent) */
	public List<IEvent> getLastTurnWorldEvents(WorldState worldState);
	
	/** Return a list of all the events that occurred in the last X turns (Note this only includes those that were visible to the agent) */
	public List<IEvent> getLastXTurnsWorldEvents(int x, WorldState worldState);
	
}
