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

package com.roguecloud;

public class UniqueIdGenerator {
	
	public static enum IdType { CREATURE, OBJECT, EVENT, MESSAGE}; 
	
	private final long[] nextValue;

	private boolean frozen = false;

	public UniqueIdGenerator() {
		
		nextValue = new long[IdType.values().length];
		
	}
	
	public long getNextUniqueId(IdType i) {
		if(frozen) { throw new UnsupportedOperationException("Attempt to read frozen generator."); }
		
		return nextValue[i.ordinal()]++;
		
	}
	
	public UniqueIdGenerator fullClone() {
		
		UniqueIdGenerator result = new UniqueIdGenerator();
		
		for(int x = 0; x < result.nextValue.length; x++) {
			result.nextValue[x] = this.nextValue[x];
		}
		
		
		return result;
		
	}
	
	
	public void freeze() {
		frozen = true;
	}
	
}
