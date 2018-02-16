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

public final class MonsterMachine {
	
	private final Logger log = Logger.getInstance();

	private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors() * 10;
	
	private final RoundScope round;
	
	private final Object lock = new Object();
	
	private final Map<Long /*creature id*/, WorkEntry> mapWork_synch_lock = new HashMap<>();

	private final Queue<Long /* creature id*/> workListNew_synch_lock = new ArrayDeque<>();
	
	private final Map<Long /* creature id*/, Boolean /*is thread active*/> hasActiveThread_synch_lock = new HashMap<>();
	
	private final LogContext lc;
	
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
						
						if(mapWork_synch_lock.size() == 0) {
							lock.wait(1000);
						}
						
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
