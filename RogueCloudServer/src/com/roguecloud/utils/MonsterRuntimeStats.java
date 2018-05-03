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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.roguecloud.RCRuntime;
import com.roguecloud.creatures.ICreature;

/** 
 * This class is used to keep track of how much CPU/thread time is used by the AI logic for each Monster object. This value
 * can vary widely, due to different Monsters using different AI classes, or because a Monster is in a more complex position for
 * the path-finding algorithm.
 * 
 * Generally speaking, most of the time spent in AI logic will be in the path-finding code (AStarSearch and, 
 * to a lesser extent, FastPathSearch).
 * 
 **/
public class MonsterRuntimeStats {

	private static final boolean ENABLED = RCRuntime.CHECK;
	
	// TODO: What's this for?
	
	public MonsterRuntimeStats() {
		arrayOfMaps = new ArrayList<HashMap<Long, MonsterRuntimeEntry>>();
		
		for(int x = 0; x < 20; x++) {
			arrayOfMaps.add(new HashMap<Long, MonsterRuntimeEntry>());
		}
	}
	
	private final ArrayList<HashMap<Long /* creatue id*/, MonsterRuntimeEntry>> arrayOfMaps;
			
	public void addThreadTimeUsed(ICreature c, long deltaInNanos) {
		if(!ENABLED) { return; }
		
		long id = c.getId();
		
		HashMap<Long, MonsterRuntimeEntry> map = arrayOfMaps.get((int)(id%arrayOfMaps.size()));
		synchronized(map) {
			MonsterRuntimeEntry mre = map.get(id);
			if(mre == null) {
				mre = new MonsterRuntimeEntry();
				map.put(id,  mre);
			}
			mre.incrementThreadTimeUsedInNanos(deltaInNanos);
		}
	}
	
	public void removeEntry(ICreature c) {
		if(!ENABLED) { return; }
		
		long id = c.getId();
		
		HashMap<Long, MonsterRuntimeEntry> map = arrayOfMaps.get((int)( id % arrayOfMaps.size()));
		synchronized(map) {
			map.remove((Long)id);
		}
	}
	
	public Map<Long, MonsterRuntimeEntry> getAll() {
		if(!ENABLED) { return Collections.emptyMap(); }
		
		HashMap<Long, MonsterRuntimeEntry> result = new HashMap<Long, MonsterRuntimeEntry>();
		
		for(int x = 0; x < arrayOfMaps.size(); x++) {
			HashMap<Long, MonsterRuntimeEntry> m = arrayOfMaps.get(x);
			synchronized(m) {
				m.entrySet().stream().forEach( e -> {
					MonsterRuntimeEntry mreClone = e.getValue().deepClone();
					result.put(e.getKey(), mreClone);
				});
			}
			
		}
		
		return result;
	}
	
	public MonsterRuntimeEntry getEntry(ICreature c) {
		if(!ENABLED) { return null; }
		
		long id = c.getId();
		
		HashMap<Long, MonsterRuntimeEntry> map = arrayOfMaps.get((int)( id % arrayOfMaps.size()));
		synchronized(map) {
			MonsterRuntimeEntry mre = map.get(id);
			if(mre == null) { return null; }
			
			return mre.deepClone();
		}
	}
	

	/** The statistics for a specific creature, which are stored in the  ArrayList->HashMap above. */
	public static class MonsterRuntimeEntry {
		
		public MonsterRuntimeEntry() {
		}
		
		private long threadTimeUsedInNanos = 0;
		private long numberOfIncrements =0; 
		
		
		public long getThreadTimeUsedInNanos() {
			return threadTimeUsedInNanos;
		}
		
		public long getNumberOfIncrements() {
			return numberOfIncrements;
		}
		
		public void incrementThreadTimeUsedInNanos(long deltaInNanos) {
			this.threadTimeUsedInNanos += deltaInNanos;
			this.numberOfIncrements++;
		}
		
		public long calculateAverageNanosPerIncrement() {
			return this.threadTimeUsedInNanos / numberOfIncrements;
		}
		
		public MonsterRuntimeEntry deepClone() {
			MonsterRuntimeEntry result = new MonsterRuntimeEntry();
			result.threadTimeUsedInNanos = this.threadTimeUsedInNanos;
			result.numberOfIncrements = this.numberOfIncrements;
			return result;
		}
	}
	
}
