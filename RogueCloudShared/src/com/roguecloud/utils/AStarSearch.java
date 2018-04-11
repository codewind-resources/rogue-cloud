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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import com.roguecloud.Position;
import com.roguecloud.map.IMap;
import com.roguecloud.map.Tile;

/** 
 * This class may be used to find the shortest path between two positions on the map. 
 * This allows you to write code to easily move your character between any two points on the map.
 * 
 * To find a path between two positions:
 * 
 * ```
 * Position startingPosition = (where your character is currently positioned, eg selfState.getPlayer().getPosition() ); 
 * Position goalPosition = (where you want your character to go);
 * List<Position> path = AStartSearch.findPath( startingPosition,  goalPosition, worldState.getMap());
 * ```
 * 
 * If AStarSearch was successful, and found a valid path between the two points:
 * 
 * - 'path' will contain a list of valid positions from the start to the goal
 * - The first entry in 'path' will be the start position
 * - The final entry in 'path' will be the goal position
 * - Passing these positions to StepAction(...) one at a time will allow the player creature to move to the destination  
 * 
 * If AStarSearch was not successful, and we could not find a path:
 * 
 * - 'path' will be a non-null list with zero elements.
 * 
 * If you wish to constrain the search area, use findPath(...) but specify the maximum number of tiles to search. 
 * This should not be necessary for game client code (but is used server side to reduce CPU load on the hot path). 
 *
 * This class implements the algorithm described at https://en.wikipedia.org/wiki/A*_search_algorithm
 **/
public final class AStarSearch {

	/** Find a path between two points on a map. */
	public final static List<Position> findPath(Position start, Position goal, IMap m) {
		return findPath(start, goal, Long.MAX_VALUE, m);
	}
	
	/** Find a path between two points on a map. Restrict search to at most 'maxTilesToSearch': use this option 
	 * to reduce CPU usage. */
	public final static List<Position> findPath(Position start, Position goal, long maxTilesToSearch, IMap m) {

		Node startNode = new Node(start);

		// The set of nodes already evaluated
		HashMap<Node, Boolean> closedSet = new HashMap<>();

		// The set of currently discovered nodes that are not evaluated yet.
		// Initially, only the start node is known.
		PriorityQueue<Node> openSet = new PriorityQueue<>(new FScoreComparator());

		HashMap<Node, Boolean> inOpenSet = new HashMap<>();

		openSet.add(startNode);

		// For each node, which node it can most efficiently be reached from.
		// If a node can be reached from many nodes, cameFrom will eventually contain
		// the
		// most efficient previous step.
		HashMap<Node, Node> cameFrom = new HashMap<>();

		// // For each node, the cost of getting from the start node to that node.
		// HashMap<Node, Long> gScore = new HashMap<>();

		// // The cost of going from start to start is zero.
		startNode.g_score = 0;

		// For each node, the total cost of getting from the start node to the goal
		// by passing by that node. That value is partly known, partly heuristic.
		// HashMap<Node, Long> fScore = new HashMap<>();

		// For the first node, that value is completely heuristic.
		startNode.f_score = heuristicCostEstimate(start, goal);

		Node[] neighbours = new Node[4];
		
		long c = 0;
		
		while (!openSet.isEmpty() && closedSet.size() < maxTilesToSearch) {

			c++;
			// current := the node in openSet having the lowest fScore[] value
			Node current = openSet.remove();
			inOpenSet.remove(current);

//			m.putTile(current.p, new Tile(TileType.BRICK_FENCE_STRAIGHT_VERT_LEFT));

			if (current.p.equals(goal)) {
				return reconstructPath(cameFrom, current);
			}

			closedSet.put(current, true);

			getNeighbours(neighbours, current, m, (int)(c%4));
//			System.out.println("neighbours: "+neighbours.size()+" "+m.getXSize()+" "+m.getYSize());
			for (Node neighbour : neighbours) {
				if(neighbour == null) { continue; }

				// Ignore the neighbor which is already evaluated.
				if (closedSet.containsKey(neighbour)) {
					continue;
				}
				
				if (!inOpenSet.containsKey(neighbour)) {

					Tile t = m.getTile(neighbour.p);
//					System.out.println(neighbour.p +" -> " +t.isPresentlyPassable());
					
					// Here we assume that if we haven't seen a tile yet, that it is passable.
					if(t == null || t.isPresentlyPassable()) {
						// Discover a new node
						openSet.add(neighbour);
						inOpenSet.put(neighbour, true);						
					} else {
						closedSet.put(neighbour, true);
						continue;
					}
				}

				// The distance from start to a neighbor
				// the "dist_between" function may vary as per the solution requirements.
				// tentative_gScore := gScore[current] + dist_between(current, neighbor)
				long tentative_gScore = current.g_score + distanceBetween(current, neighbour);
				
				if(tentative_gScore >= neighbour.g_score) {
					continue; // This is not a better path.
				}
				
				// This path is the best until now. Record it!
				
				// cameFrom[neighbor] := current
				cameFrom.put(neighbour, current);
				
				// gScore[neighbor] := tentative_gScore
				neighbour.g_score = tentative_gScore;
				
				// fScore[neighbor] := gScore[neighbor] + heuristic_cost_estimate(neighbor, goal)
				neighbour.f_score = neighbour.g_score + heuristicCostEstimate(neighbour.p, goal);
				

			}
		}

		return new ArrayList<>();
		
	}

	private final static void getNeighbours(Node[]  result, Node current, IMap m, int c) {
//		List<Node> list = new ArrayList<Node>(4);
//		Node[] result = new Node[4];

		c = (int)(Math.random()* 4);
		
		Position p = current.p;
		
		// Left
		Position left = new Position(p.getX() - 1, p.getY());
		if (left.isValid(m)) {
			result[c] = new Node(left);
//			list.add(new Node(left));
			c = (c + 1) % result.length;
		}

		// Right
		Position right = new Position(p.getX() + 1, p.getY());
		if (right.isValid(m)) {
			result[c] = new Node(right);
			c = (c + 1) % result.length;
		}

		// Up
		Position up = new Position(p.getX(), p.getY() - 1);
		if (up.isValid(m)) {
			result[c] = new Node(up);
			c = (c + 1) % result.length;
		}

		// Down
		Position down = new Position(p.getX(), p.getY() + 1);
		if (down.isValid(m)) {
			result[c] = new Node(down);
			c = (c + 1) % result.length;
		}
		
//		Collections.shuffle(list);
		
//		return result;
	}

	private final static List<Position> reconstructPath(HashMap<Node, Node> cameFrom, Node current) {

		List<Position> totalPath = new ArrayList<>();
		totalPath.add(current.p);

		while (current != null) {
			current = cameFrom.get(current);
			if (current != null) {
				totalPath.add(current.p);
			}
		}

		Collections.reverse(totalPath);
		
		return totalPath;
	}

	private static final long distanceBetween(Node one, Node two) {
		return heuristicCostEstimate(one.p, two.p);
	}

	private static final long heuristicCostEstimate(Position start, Position goal) {
		return Math.abs(goal.getX() - start.getX()) + Math.abs(goal.getY() - start.getY());
	}

	/** Node as defined by A-Star algorithm */
	private static final class Node {

		final Position p;

		long f_score = Long.MAX_VALUE;
		long g_score = Long.MAX_VALUE;

		Node(Position p) {
			this.p = p;
		}

		@Override
		public int hashCode() {
			return p.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return p.equals(((Node) obj).p);
		}

	}

	/** Sort a Node by f-score (ascending). */
	private static class FScoreComparator implements Comparator<Node> {

		@Override
		public int compare(Node o1, Node o2) {
			long val = o1.f_score - o2.f_score;

			if (val > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}

			if (val < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}

			return (int) val;
		}

	}
}
