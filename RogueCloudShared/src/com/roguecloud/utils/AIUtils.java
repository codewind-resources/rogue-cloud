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
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.items.IGroundObject;
import com.roguecloud.map.IMap;
import com.roguecloud.map.Tile;

/** Helper utility methods that coders may use to implement their agents.  */

public class AIUtils {
	
	/**
	 * Find a random position on the map, between the coordinates (x1, y1) to (x2, y2).
	 * 
	 * @param allowUnknownTiles Whether to return unknown tiles -- unknown tiles are those that the agent has not seen yet (and thus they might not be valid to move to/on)
	 * @param map The map object from worldState
	 * 
	 * @return A valid random position inside the coordinates, or null if one could not be found.
	 */
	public static Position findRandomPositionOnMap(int x1, int y1, int x2, int y2, boolean allowUnknownTiles, IMap map) {
		int attempts = 0;
		
		// Try up to 100 times to find a valid random position within the coordinates
		while(attempts < 100) {
			
			int x = (int)(Math.random() * (x2 - x1) ) + x1;
			int y = (int)(Math.random() * (y2 - y1) ) + y1;
			
			Position p = new Position(x, y);

			if(p.isValid(map)) {
								
				Tile t = map.getTile(p);
				if(t != null) {
					if(t.isPresentlyPassable() == true) {
						return p;
					} else {
						/* ignore */
					}
				} else {
					if(allowUnknownTiles) {
						return p;
					}					
				}
				
			}
			
			attempts++;
		}
		
		return null;
	}
		
	/** Locate ground objects around the given position, and return them as a sorted list (closest are first in the list). */
	public static List<IGroundObject> findAndSortGroundObjectsInRange(int clientViewWidth, int clientViewHeight, Position myPos, IMap map) {

		int startX = Math.max(0, myPos.getX()-(clientViewWidth/2));
		int startY = Math.max(0, myPos.getY()-(clientViewHeight/2));
		
		int endX = Math.min(map.getXSize()-1, myPos.getX()+(clientViewWidth/2));
		int endY = Math.min(map.getYSize()-1, myPos.getY()+(clientViewHeight/2));
		
		List<IGroundObject> result = new ArrayList<>();		

		for(int x = startX; x <= endX; x++) {
			
			for(int y = startY; y <= endY; y++) {
				
				Tile t = map.getTile(x, y);
				if(t != null) {					
					t.getGroundObjects().stream().forEach( e -> {
						result.add(e);
					});
				}
				
			}
		}

		Collections.sort(result,  (a, b) -> {
			long aDistance = myPos.manhattanDistanceBetween(a.getPosition());
			long bDistance = myPos.manhattanDistanceBetween(b.getPosition());
			
			return (int)(aDistance - bDistance);
			
		});
		
		return result;
		
	}

	/** Remove creatures that represent other players from the given list. Other player's creatures cannot be attacked,
	 * so this method will let you filter them out. */
	public static void removePlayerCreaturesFromList(List<ICreature> creatures) {
		for(Iterator<ICreature> it = creatures.iterator(); it.hasNext();) {
			ICreature c = it.next();
			if(c == null || c.isPlayerCreature()) {
				it.remove();
			}
		}
		
	}
	
	/** Sort a list of creatures by which is closest, with the closest being first in the list (sorted ascending by distance) */
	public static void sortClosestCreatures(Position myPos, List<ICreature> creatures) {
		
		Collections.sort(creatures, (a, b) -> { 
			long aDistance = myPos.manhattanDistanceBetween(a.getPosition());
			long bDistance = myPos.manhattanDistanceBetween(b.getPosition());
			
			return (int)(aDistance - bDistance);

		});
		
	}
	
	/** Returns true if a creature at position 'src' can "reach" a player at position 'dest', and false otherwise.
	 * NOTE: Reach means that the two positions are within 0 or 1 tiles of each other (and they are not diagonal)
	 * 
	 * A creature can only attack another creature if they can reach.
	 * A creature can only move to a tile that they can reach.
	 * 
	 * A creature can reach a tile that is at most one tile up, down, left, or right of them (no diagonals).
	 * A creature may reach her or his own tile.
	 * 
	 * Reach does NOT refer to whether or not there is a valid path between the two positions. For this, see AStarSearchJob.
	 **/
	public static boolean canReach(Position src, Position dest, IMap map) {
		if(!src.isValid(map) || !dest.isValid(map)) {
			return false;
		}
		
		int deltaX = Math.abs(dest.getX() - src.getX());
		int deltaY = Math.abs(dest.getY() - src.getY());
		
		if(deltaX + deltaY == 1 || deltaX + deltaY == 0) {
			return true;
		}
		
		return false;
	}

	
	/**  Return an unsorted list of alive creatures that are within range of the viewWidth/viewHeight of myPos. */
	public static List<ICreature> findCreaturesInRange(int clientViewWidth, int clientViewHeight, Position myPos, IMap map) {

		int startX = Math.max(0, myPos.getX()-(clientViewWidth/2));
		int startY = Math.max(0, myPos.getY()-(clientViewHeight/2));
		
		int endX = Math.min(map.getXSize()-1, myPos.getX()+(clientViewWidth/2));
		int endY = Math.min(map.getYSize()-1, myPos.getY()+(clientViewHeight/2));
		
		List<ICreature> result = new ArrayList<>();		

		for(int x = startX; x <= endX; x++) {
			
			for(int y = startY; y <= endY; y++) {
				
				Tile t = map.getTile(x, y);
				if(t != null) {
					
					// Exclude ourselves, and dead creatures
					t.getCreatures().stream().filter( e -> !e.isDead() && (myPos == null || !e.getPosition().equals(myPos))   ).forEach( (e) -> {
						result.add(e);
					});					
				}
				
			}
			
		}
	
		return result;
	}


	/** Return an unsorted list of creatures that are in a specific range (measured in # of tiles) from myPos on the map. */
	public static List<ICreature> findCreaturesInRange(int range, IMap map, Position myPos) {
		if(myPos == null) { return Collections.emptyList(); }
		
		List<ICreature> result = new ArrayList<>();
		
		int startX = myPos.getX();
		int startY = myPos.getY();
		
		int endX = myPos.getX();
		int endY = myPos.getY();
		
		
		startX = Math.max(0, startX-range);
		startY = Math.max(0, startY-range);
		
		endX = Math.min(map.getXSize()-1, endX+range);
		endY = Math.min(map.getYSize()-1, endY+range);

		for(int x = startX; x <= endX; x++) {
			
			for(int y = startY; y <= endY; y++) {
				
				Tile t = map.getTile(x, y);
				if(t != null) {
					
					// Exclude ourselves, and dead creatures
					t.getCreatures().stream().filter( e -> !e.isDead() && (myPos == null || !e.getPosition().equals(myPos))  ).forEach( (e) -> {
						result.add(e);
					});					
				}
				
			}
			
		}
	
		return result;
	}
	
	/** Return an unsorted list of creatures that are in the bounds of the rectangle (startX, startY) to (endX, endY) inclusive, on the map. */
	public static List<ICreature> findCreaturesInRectangle(int startX, int startY, int endX, int endY, Position myPos, IMap map) {
		List<ICreature> result = new ArrayList<>();
		
		for(int x = startX; x <= endX; x++) {
			
			for(int y = startY; y <= endY; y++) {
				
				Tile t = map.getTile(x, y);
				if(t != null) {
					
					// Exclude ourselves, and dead creatures
					t.getCreatures().stream().filter( e -> !e.isDead() && (myPos == null || !e.getPosition().equals(myPos)) ).forEach( (e) -> {
						result.add(e);
					});					
				}
				
			}
			
		}
	
		return result;
		
	}
	
	/** Returns the closest accessible ground objects from your current position. */
	public static FindClosestResult<IGroundObject> findClosestGroundObjectThatCanBeReached(IMap map, WorldState worldState, SelfState selfState) {
		
		List<IGroundObject> goList = findAndSortGroundObjectsInRange(worldState.getViewWidth(), worldState.getViewHeight(), selfState.getPlayer().getPosition(), map);
		
		// Find the closest creature that we can reach
		while(goList.size() > 0) {
			IGroundObject curr = goList.remove(0);
			List<Position> routeToItem = FastPathSearch.doSearchWithAStar(selfState.getPlayer().getPosition(), curr.getPosition(), map);
			if(routeToItem.size() > 0) {
				routeToItem.remove(0);
				return new FindClosestResult<IGroundObject>(routeToItem, curr);
				
			} 
		}
		
		return null;
	}


	/** Returns the closest accessible creature from your current position. */
	public static FindClosestResult<ICreature> findClosestCreatureThatCanBeReached(IMap map, WorldState worldState, SelfState selfState) {
		
		List<ICreature> creatures = AIUtils.findCreaturesInRange(worldState.getViewWidth(), worldState.getViewHeight(), selfState.getPlayer().getPosition(), map);
		
		AIUtils.sortClosestCreatures(selfState.getPlayer().getPosition(), creatures);
		
		// Find the closest creature that we can reach
		while(creatures.size() > 0) {
			ICreature curr = creatures.remove(0);
			List<Position> routeToCreature = FastPathSearch.doSearchWithAStar(selfState.getPlayer().getPosition(), curr.getPosition(), map);
			if(routeToCreature.size() > 0) {
				routeToCreature.remove(0);
				return new FindClosestResult<ICreature>(routeToCreature, curr);
				
			} 
		}
		
		return null;
	}
	
	
	/** Return a valid list of Positions that can be reached, or moved to, from the position 'p' on the map. */
	public static List<Position> getValidNeighbouringPositions(Position p, IMap m) {
		
		List<Position> result = new ArrayList<>();
		
		Position[] pos = new Position[] {
				new Position(p.getX(), p.getY()-1),
				new Position(p.getX(), p.getY()+1),
				new Position(p.getX()+1, p.getY()),
				new Position(p.getX()-1, p.getY()),	
		};
		
		for(Position x : pos) {
			if(x.isValid(m)) {
				result.add(x);
			}
		}
		
		return result;
	}

	
	/** Simple utility class containing route data to creature or object */
	public static class FindClosestResult<T> {
		
		public FindClosestResult(List<Position> route, T creatureOrObject) {
			if(route == null) { throw new IllegalArgumentException(); }
			if(creatureOrObject == null) { throw new IllegalArgumentException(); }
			
			this.route = route;
			this.creatureOrObject = creatureOrObject;
		}
		
		private final List<Position> route;
		private final T creatureOrObject;
		
		public T get() {
			return creatureOrObject;
		}
		
		public List<Position> getRoute() {
			return route;
		}
	}

}
