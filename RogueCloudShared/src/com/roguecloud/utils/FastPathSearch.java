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
import java.util.Iterator;
import java.util.List;

import com.roguecloud.Position;
import com.roguecloud.map.IMap;
import com.roguecloud.map.ITerrain;
import com.roguecloud.map.RCArrayMap;
import com.roguecloud.map.Tile;

/**
 * For the vast majority of cases, the AStarSearch class is preferable to this class for game client code.
 *  
 * This class may be used to find the shortest path between two positions on the map. 
 * This allows you to write code to easily move your character between any two points on the map.
 * 
 * To find a path between two positions:
 * ```
 * Position startingPosition = (where your character is currently positioned, eg selfState.getPlayer().getPosition() ); 
 * Position goalPosition = (where you want your character to go);
 * List<Position> path = FastPathSearch.doSearchWithAStar( startingPosition,  goalPosition, worldState.getMap());
 * ```
 * 
 * If search was successful (we found a valid path):
 * 
 * - 'path' will contain a list of valid positions from the start to the goal
 * - The first entry in 'path' will be the start position
 * - The final entry in 'path' will be the goal position
 * - Passing these positions to StepAction(...) one at a time will allow the player creature to move to the destination  
 * 
 * If search was not successful (we could not find a valid path):
 * 
 * - 'path' will be a non-null list with zero elements.
 * 
 * This class differs from the AStarSearch class in that for simple search scenarios FastPathSearch is much less CPU intensive. 
 * FastPathSearch attempts to get as close a possible to the goal using a simple traversal algorithm, and then falls back
 * to the AStar algorithm when the simple traversal algorithm fails.
 * 
 * To use only the Fast Path algorithm, without falling back to AStar, use doSearch(...)
 * 
 * (This class is used extensively on the server side to allow for increased scalability of monster AIs) 
 * 
 */
public class FastPathSearch {

	public final static List<Position> doSearchWithAStar(Position start, Position goal, IMap m) {
		return doSearchWithAStar(start, goal, Long.MAX_VALUE, m);
	}
	
	public final static List<Position> doSearchWithAStar(Position start, Position goal, long maxTilesToSearch, IMap m) {
		
		List<Position> currentList = doSearch(start, goal, m);
		
		if(currentList.size() > 0) {
			
			Position lastPos = currentList.get(currentList.size()-1);
			if(lastPos.equals(goal)) {
				return removeDeadPathIfNecessary(currentList);
			}
			
			List<Position> astarList = AStarSearch.findPath(lastPos, goal, maxTilesToSearch, m);
			if(astarList.size() > 0) {
				// A* got (somewhat) farther...
				astarList.remove(0); 
				currentList.addAll(astarList);
				
				// But did it get all the way? If not, return empty list
				lastPos = currentList.get(currentList.size()-1);
				if(lastPos.equals(goal)) {
					return removeDeadPathIfNecessary(currentList);
				} else {
					return Collections.emptyList();
				}
				
			} else {
				// A* had no luck either
				return Collections.emptyList();
			}
			
		} else {
			// Our fast path algorithm had no luck, so just A* the whole thing.
			return AStarSearch.findPath(start, goal, maxTilesToSearch, m);
		}
	}
	
	
	/** When "bridging" between results from the fast path algorithm into results from the
	 * a-star algorithm, there can be duplicate "dead path" elements, which can be removed.
	 *   
	 * Example: (0, 0), (1, 0), (0, 0), (1, 0), (1, 1) --> (0, 0), (1, 0), (1, 1)*/
	private static final List<Position> removeDeadPathIfNecessary(List<Position> currentList) {
		int firstDupePos = -1;
		int secondDupePos = -1;
		
		outer: for(int x = 0; x < currentList.size(); x++) {
			for(int y = x+1; y < currentList.size(); y++) {
				
				Position xPos = currentList.get(x);
				Position yPos = currentList.get(y);
				
				if(xPos.equals(yPos)) {
					firstDupePos = x;
					secondDupePos = y;
					break outer;
				}
			}
		}
		
		if(firstDupePos != -1 && secondDupePos != -1) {
			
			int index = 0;
			for(Iterator<Position> it = currentList.iterator(); it.hasNext();) {
				it.next();
				if(index > firstDupePos && index <= secondDupePos) {
					it.remove();
				}
				
				index++;
			}
			
		}
				
		return currentList;
	}


	public final static List<Position> doSearch(Position start, Position goal, IMap m) {
		
		List<Position> result = new ArrayList<Position>();

		Position currPosition = start;
		
		result.add(currPosition);
		
		int currDirectionX = 0;
		int currDirectionY = 0;
		
		
		while(!currPosition.equals(goal)) {

			int deltaX = goal.getX() - currPosition.getX();
			int deltaY = goal.getY() - currPosition.getY();

			int[] coord = nextCoord(deltaX, deltaY, currDirectionX, currDirectionY, currPosition, m);
			if(coord == null) {
				return result;
			}
			currDirectionX = coord[0];
			currDirectionY = coord[1];

			
			Position newPos = new Position(currPosition.getX()+currDirectionX, currPosition.getY()+currDirectionY );
			result.add(newPos);
			
			currPosition = newPos;
			
		}
		
		return result;
	}
	
	private static final int[] nextCoord(int deltaX, int deltaY, int currDirectionX, int currDirectionY, Position currPos, IMap map) {
		
		boolean canDoX = Math.abs(deltaX) > 0 && isValidTile(currPos.getX()+(deltaX > 0 ? 1 : -1), currPos.getY(), map);
		boolean canDoY = Math.abs(deltaY) > 0 && isValidTile(currPos.getX(), currPos.getY()+(deltaY > 0 ? 1 : -1), map);
		
		boolean doX = false;
		
		if(!canDoX && !canDoY) {
			return null;
		} else if(canDoX && !canDoY) {
			// can only do X
			doX = true;
		} else if(canDoY && !canDoX) {
			// can only do Y
			doX = false;
		} else {
			if(currDirectionX == 0 && currDirectionY == 0) {
				// we can do both, but have no previous direction, so pick at random
				// According to my benchmarking, this math.random() does not affect performance in the slightest.
				doX = Math.random()*1 < 0.5 ? true : false; 
			} else {
				// We can do both, but we have a previous direction, so do the opposite
				if(Math.abs(currDirectionX) > 0) {
					doX = false;
				} else {
					doX = true;
				}						
			}
		}

		if(doX) {
			currDirectionX = deltaX > 0 ? 1 : -1;
			currDirectionY = 0;
			
		} else {
			currDirectionX = 0;
			currDirectionY = deltaY > 0 ? 1 : -1;
		}
		
		return new int[] { currDirectionX, currDirectionY };

	}
		
	private static final boolean isValidTile(int x, int y, IMap map) {
		Tile t = map.getTile(x, y);
		if(t != null && t.isPresentlyPassable()) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		RCArrayMap map = new RCArrayMap(100, 100);	
		
		for(int x = 0; x < map.getXSize(); x++) {
			for(int y = 0; y < map.getYSize(); y++) {
				
				map.putTile(x,  y, new Tile(true, (ITerrain)null));
				
			}
		}
		
//		map.putTile(9, 9, new Tile(false, null, null));
//		map.putTile(9, 8, new Tile(false, null, null));
//		map.putTile(8, 9, new Tile(false, null, null));
		
		
		for(int x = 0; x < map.getYSize(); x++) {
//			map.putTile(x, 10, new Tile(false, null, null));
		}
		
		Position start = new Position(1, 1);
		
		Position goal = new Position(20, 20);
		
//		List<Position> result = doSearch(start, goal, map);
		List<Position> result = doSearchWithAStar(start, goal, map);
		
		result.forEach( e -> {
			System.out.println(e);
		});
		
		// Need to add check if we the path on the map is traversable, otherwise bail.
	}
	

	
}
