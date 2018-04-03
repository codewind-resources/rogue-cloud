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

/** 
 * Throughout the codebase, many entities must have short, unique ids in order to allow them to be tracked between the
 * client and server. This class is used to generate those IDs.
 * 
 * A separate ID counter is maintained for different types of entities (see IdType enum).
 * 
 * The generator may be "frozen", which prevents any calls  to getNextUniqueId(...) after the freeze() call is issued. 
 * Clones of a frozen generator are not frozen. The purpose of freeze is to create an immutable generator, which can
 * then be cloned for use as an immutable generator by child methods. 
 * 
 **/
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
