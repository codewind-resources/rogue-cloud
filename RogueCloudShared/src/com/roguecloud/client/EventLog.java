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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.roguecloud.creatures.ICreature;
import com.roguecloud.events.IEvent;
import com.roguecloud.events.IMutableEvent;

/** 
 * See IEventLog for a description of the public interface of this method.
 * 
 * Thread-safe.
 **/
public class EventLog implements IEventLog {
	
	private final Object lock = new Object();

	private final List<IEvent> events_synch_lock = new ArrayList<>(); 
	
	private final HashMap<Long /* event id*/, IEvent> eventMap_synch_lock = new HashMap<>();
	
	private final int numberOfFramesOfEventsToKeep;
	
	public EventLog(int numberOfFramesOfEventsToKeep) {
		this.numberOfFramesOfEventsToKeep = numberOfFramesOfEventsToKeep;
	}
	
	public void internalClearOldEvents(long currentFrame) {
		synchronized(lock) {
			
			for(Iterator<IEvent> it = events_synch_lock.iterator(); it.hasNext(); ) {
				
				IEvent ev = it.next();
				
				if(ev.getFrame() < currentFrame - numberOfFramesOfEventsToKeep) {
					it.remove();
					eventMap_synch_lock.remove( ((IMutableEvent)ev).getEventId());
				}
			}
		}
	}
	
	public void internalAddEvents(List<IEvent> list) {
		synchronized(lock) {
			for(IEvent e : list) {
			
				long eventId = ((IMutableEvent)e).getEventId();
				
				if(!eventMap_synch_lock.containsKey(eventId)) {
					events_synch_lock.add(e);
					eventMap_synch_lock.put( eventId, e);
				}
			}
		}
	}
	
	public void dispose() {
		synchronized (lock) {
			events_synch_lock.clear();
			eventMap_synch_lock.clear();
		}
	}
	
	@Override
	public List<IEvent> getAllEvents() {
		List<IEvent> result = new ArrayList<>();
		
		synchronized (lock) {
			result.addAll(events_synch_lock);
		}
		
		return Collections.unmodifiableList(result);
	}
	
	@Override
	public List<IEvent> getLastTurnSelfEvents(ICreature us, WorldState worldState) {
		synchronized (lock) {
			return events_synch_lock
					.stream()
					.filter( e -> e.getFrame() == worldState.getCurrentGameTick()-1 
					&& e.isCreatureInvolved(us))
					.collect(Collectors.toList());
		}
	}
	
	@Override
	public List<IEvent> getLastXTurnsSelfEvents(int x, ICreature us, WorldState worldState) {
		synchronized (lock) {
			return events_synch_lock
					.stream()
					.filter( e -> e.getFrame() > worldState.getCurrentGameTick()-x 
							&& e.isCreatureInvolved(us))
					.collect(Collectors.toList());
		}	
	}
	
	@Override
	public List<IEvent> getLastTurnWorldEvents(WorldState worldState) {
		synchronized (lock) {
			return events_synch_lock
					.stream()
					.filter( e -> e.getFrame() == worldState.getCurrentGameTick()-1)
					.collect(Collectors.toList());
		}
	}
	
	@Override
	public List<IEvent> getLastXTurnsWorldEvents(int x, WorldState worldState) {
		synchronized (lock) {
			return events_synch_lock
					.stream()
					.filter( e -> e.getFrame() > worldState.getCurrentGameTick()-x)
					.collect(Collectors.toList());
		}		
	}
	
}
