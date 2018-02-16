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
import java.util.HashMap;

import com.roguecloud.creatures.ICreature;

public class MonsterRuntimeStats {

	public MonsterRuntimeStats() {
		arrayOfMaps = new ArrayList<HashMap<Long, MonsterRuntimeEntry>>();
		
		for(int x = 0; x < 20; x++) {
			arrayOfMaps.add(new HashMap<Long, MonsterRuntimeEntry>());
		}
	}
	
	private final ArrayList<HashMap<Long /* creatue id*/, MonsterRuntimeEntry>> arrayOfMaps;
			
	
	public void addThreadTimeUsed(ICreature c, long deltaInNanos) {
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
		long id = c.getId();
		
		HashMap<Long, MonsterRuntimeEntry> map = arrayOfMaps.get((int)( id % arrayOfMaps.size()));
		synchronized(map) {
			map.remove((Long)id);
		}
	}
	
	public HashMap<Long, MonsterRuntimeEntry> getAll() {
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
		long id = c.getId();
		
		HashMap<Long, MonsterRuntimeEntry> map = arrayOfMaps.get((int)( id % arrayOfMaps.size()));
		synchronized(map) {
			MonsterRuntimeEntry mre = map.get(id);
			if(mre == null) { return null; }
			
			return mre.deepClone();
		}
	}
	
	
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
