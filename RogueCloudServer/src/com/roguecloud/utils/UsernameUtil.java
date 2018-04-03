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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Disallow specific offensive words from usernames; rather than storing the specific banned words in this class, they are stored as a tuple: (number of letters, java hashcode() of string)  */
public class UsernameUtil {

	private final Map<Integer, List<Integer>> map = new HashMap<>();

	public UsernameUtil() {
		int[][] x = new int[][] {
			new int[] { 5, 104817638 }, new int[] { 6, -1045620280 }, new int[] { 4, 3065272 },
			new int[] { 4, 3083181 }, new int[] { 5, 113107569 }, new int[] { 5, 93747762 },
			new int[] { 4, 3529280 }, new int[] { 4, 3154295 }, new int[] { 5, 107034100 },
			new int[] { 7, -704690495 }, new int[] { 4, 3533496 }, new int[] { 4, 3059156 },
			new int[] { 4, 3002947 }, new int[] { 4, 3262655 }, new int[] { 4, 3441177 },
			new int[] { 6, -1325957354 }, new int[] { 7, 1917367956 }, new int[] { 6, -930921753 },
			new int[] { 6, -898839792 } 
		};
			
		for(int[] pair : x) {
			
			List<Integer> r = map.get(pair[0]);
			if(r == null) {
				r = new ArrayList<>();
				map.put(pair[0], r);
			}
			r.add(pair[1]);
		}

	}

	public boolean isValidUserName(String username) {
		List<Integer> sizes = new ArrayList<Integer>(map.keySet().stream().distinct().collect(Collectors.toList()));
		
		for(int x = 0; x < username.length(); x++) {
			String remainingText = username.substring(x);
			
			for(int size : sizes) {
				if(size > remainingText.length()) {
					continue;
				}
				
				int hashcode = remainingText.substring(0, size).hashCode();
				if(map.get(size).contains(hashcode)) {
					return false;
				}
			}
			
		}
		
		return true;
		
	}

}
