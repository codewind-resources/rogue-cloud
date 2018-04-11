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

package com.roguecloud.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.roguecloud.AIContext;
import com.roguecloud.RoundScope;
import com.roguecloud.client.EventLog;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;

/** 
 * This class is responsible for distributing new map information to monsters after each tick, for running the threads that execute AI logic, and 
 * for returning monster actions back to the game loop.
 * 
 * This class is designed to prevent monster AIs that have intensive CPU requirements (ie they lots of pathfinding per tick) from 
 * starving out monsters that use minimal CPU per game tick.
 * 
 * Only one instance of this class will exist per round; that instance is destroyed at 
 * the end of every round (as well as all the child threads), and a new instance is created.
 **/
public final class MonsterMachine {
	
	private final Logger log = Logger.getInstance();

	private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors() * 10;
	
	private final RoundScope round;
	
	private final Object lock = new Object();
	
	/*** The next world/self state update (as a unit of work) that is available for a creature */
	private final Map<Long /*creature id*/, WorkEntry> mapWork_synch_lock = new HashMap<>();

	/** Each time informMonster(...) is called, the creature ID is */
	private final Queue<Long /* creature id*/> workListNew_synch_lock = new ArrayDeque<>();

	/** Whether or not the specific creature has work already running on another thread (a monster's AI logic must only ever 
	 * be called by one thread at a time) */
	private final Map<Long /* creature id*/, Boolean /*is thread active*/> hasActiveThread_synch_lock = new HashMap<>();
	// TODO: The thread contention on the hasActiveThread_synch_lock map could be easily improved.
	
	private final LogContext lc;
	
	/** References to all the active threads created by this class */
	private final List<MonsterMachineThread> threads_synch_lock = new ArrayList<>();
	
	private final MonsterRuntimeStats runtimeStats;
	
	public MonsterMachine(RoundScope round, MonsterRuntimeStats runtimeStats, LogContext lc) {
		this.round = round;
		this.lc = lc;
		this.runtimeStats = runtimeStats;
		
		for(int x = 0; x < NUM_THREADS; x++) {
			
			MonsterMachineThread t = new MonsterMachineThread();
			t.start();
			threads_synch_lock.add(t);
		}
	}
	
	/** This method is called for each living monster, per game tick. */
	public final void informMonster(long ticks, SelfState selfState, WorldState worldState, AIContext aicontext, EventLog eventLog) {

		if(selfState.getPlayer() == null || worldState.getMap() == null || aicontext == null || eventLog == null) {
			throw new IllegalArgumentException();
		}
		
		WorkEntry we = new WorkEntry(ticks, selfState, worldState, aicontext, eventLog);
		
		long creatureId = selfState.getPlayer().getId();
		
		synchronized(lock) {
			mapWork_synch_lock.put(creatureId, we);
			workListNew_synch_lock.offer(creatureId);
			lock.notify();
		}
		
		
//		synchronized(workList_synch) {
//			workList_synch.offer(we);
//			workList_synch.notify();
//		}
		
	}
	
	public void dispose() {
		synchronized(lock) {
			threads_synch_lock.forEach( e -> {
				e.interrupt();
			});
			threads_synch_lock.clear();
		}
	}

	/** 
	 * Many instances of this class will run as threads, per round (see NUM_THREADS). The purpose of 
	 * this thread is to extract a piece of work to perform (WorkEntry), and to call 
	 * receiveStatusUpdate(...) on the monster specified by the work entry.  
	 * */
	private class MonsterMachineThread extends Thread {
		
		public MonsterMachineThread() {
			setDaemon(true);
			setPriority(Thread.NORM_PRIORITY-2);
			setName(MonsterMachineThread.class.getName());
		}
		
		@Override
		public void run() {
			
			List<WorkEntry> weList = new ArrayList<>();
			
			while(!round.isRoundCompleteUnsynchronized()) {
				
				try {
					
					weList.clear();
					
					synchronized(lock) {
						
						// If no work, wait for work.
						if(mapWork_synch_lock.size() == 0) {
							lock.wait(1000);
						}
						
						// If there IS WORK...
						if(mapWork_synch_lock.size() > 0) {
							
							int eventsToPull = 1;
							
//							int eventsToPull = Math.min(12, Math.max(1, mapWork_synch_lock.size()/NUM_THREADS));
							
							while(weList.size() < eventsToPull && mapWork_synch_lock.size() > 0 ) {
								
								Long nextCreatureId = workListNew_synch_lock.poll();
								WorkEntry we = mapWork_synch_lock.remove(nextCreatureId);
								boolean hasActiveThreadAlready = hasActiveThread_synch_lock.getOrDefault(nextCreatureId, false);
								if(we != null && !hasActiveThreadAlready) {
									weList.add(we);
									hasActiveThread_synch_lock.put(nextCreatureId, true);
								}
							}
							
						} 

					}
					
					
//					synchronized(workList_synch) {
//						
//						if(workList_synch.size() == 0) {
//							workList_synch.wait(1000);
//						}
//						
//						if(workList_synch.size() > 0) {
//							int eventsToPull = Math.max(1, weList.size()/NUM_PROCESSORS); 
//							for(int x = 0; x < eventsToPull; x++) {
//								weList.add(workList_synch.poll());
//							}
//							
//						} 
//						
//					}
					
					for(WorkEntry we : weList) {
						try {
							long startTimeInNanos = System.nanoTime();
							we.aicontext.getClient().receiveStateUpdate(we.selfState, we.worldState, we.eventLog);
							runtimeStats.addThreadTimeUsed(we.selfState.getPlayer(), System.nanoTime() - startTimeInNanos);
						} catch(Exception e) {
							// Prevent bad monster implementations from interfering with others in the list
							log.severe("Exception thrown from monster logic", e, lc);
							e.printStackTrace();							
						}
						
//						monstersProcessed++;
					}
					synchronized(lock) {
						for(WorkEntry we : weList) {
							hasActiveThread_synch_lock.remove(we.selfState.getPlayer().getId());
						}
					}
				} catch(InterruptedException e) {
					break;
				} catch(Exception e) {
					// Nothing should kill the thread.
					log.severe("Exception thrown from outer monster logic", e, lc);
					e.printStackTrace();
				}
			}
			
		}
		
	}
	
	/** Work to be performed by the MonsterMachineThread. An instance of this class will exist for each monster, per game tick. This class corresponds
	 * to a single call of the receiveStateUpdate(...) method of the MonsterClient. 
	 * 
	 * This class contains the current state of a monster, the world state, the AIContext for the monster, and the event log (all for use by the monster AI class) */
	private static final class WorkEntry {
		@SuppressWarnings("unused")
		final long ticks;
		final SelfState selfState;
		final WorldState worldState;
		final AIContext aicontext;
		final EventLog eventLog;
		
		public WorkEntry(long ticks, SelfState selfState, WorldState worldState, AIContext aicontext, EventLog eventLog) {
			this.ticks = ticks;
			this.selfState = selfState;
			this.worldState = worldState;
			this.aicontext = aicontext;
			this.eventLog = eventLog;
		}
		
	}

}
