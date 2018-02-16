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

public final class PerfUtil {
	
	final long[] data;
	
	
	public PerfUtil(int size) {
		data = new long[size];
	}

	public final void increment(int pos, long val) {
		data[pos] += val;
	}
	
	public long[] getData() {
		return data;
	}
	
	public void reset() {
		for(int x = 0; x < data.length; x++) {
			data[x] = 0l;
		}
	}
}
