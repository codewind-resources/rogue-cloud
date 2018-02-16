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

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.roguecloud.map.IMap;
import com.roguecloud.map.RCArrayMap;
import com.roguecloud.map.Tile;
import com.roguecloud.utils.AStarSearch;

public class PathFinderBenchmark {

	private static IMap setup() {
		RCArrayMap map = new RCArrayMap(1000, 1000);
		
		for(int x = 0; x < map.getXSize(); x++) {
			for(int y = 0; y < map.getYSize(); y++) {
				map.putTile(x,  y, new Tile(true, null, null));
			}
		}
		
		return map;
	}
	
	public static void main(String[] args) {
		
		IMap map = setup();
		
		long startTimeInNanos = System.nanoTime();
		
		int count = 0; 
		
		long expireTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(15, TimeUnit.SECONDS);
		
		final Random r = new Random(0);
		
		while(true) {
			
			Position start = new Position(r.nextInt(1000), r.nextInt(1000));
			
			Position goal = new Position(r.nextInt(1000), r.nextInt(1000));
			
			
//			List<Position> result = FastPathSearch.doSearchWithAStar(start, goal, map);
			
			List<Position> result = AStarSearch.findPath(start, goal, Long.MAX_VALUE, map);
			if(result.size() == 0) {
				throw new RuntimeException();
			}
			
			if(count % 512 == 0 && count > 0) {
				System.out.println(" curr: " +count);
				if(System.nanoTime() > expireTimeInNanos) {
					break;
				}
			}
			count++;
			
		}
		
		long elapsedTimeInNanos = System.nanoTime() - startTimeInNanos;
		
		long elapsedTimeInMsecs = TimeUnit.MILLISECONDS.convert(elapsedTimeInNanos, TimeUnit.NANOSECONDS);
		
		System.out.println();
		
		System.out.println("elapsed time: " +elapsedTimeInMsecs +" / count: " + count );

		System.out.println( "iterations per second: "+ (double)count/ ((double)elapsedTimeInMsecs/1000d));
		
		
		
		
//		System.out.println("msecs per item:"+(double)elapsedTimeInMsecs/(double)count);
//		System.out.println("items per item:"+(double)elapsedTimeInMsecs/(double)count);
		
	}
}
