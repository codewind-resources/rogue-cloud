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
 * This class is principally for server-side use only.
 * 
 * This class has the same API behaviour as AStarSearch, but, unlike AStarSearch, the search algorithm used by this class
 * may be paused (by setting an expiration time) and resumed (calling continueSearch(...)) without losing previously completed work.
 * 
 * Since AStarSearch is computationally expensive, this class may be used to split the work across multiple attempts (for example,
 * in order to meet minimum framerate deadlines).
 * 
 * Client code should not need to use this class: FastPathSearch or AStarSearch are preferable under nearly all circumstances.
 *  
 **/
public final class AStarSearchInterruptible {

	// The set of nodes already evaluated
	private final HashMap<Node, Boolean> closedSet = new HashMap<>();

	// The set of currently discovered nodes that are not evaluated yet.
	// Initially, only the start node is known.
	private final PriorityQueue<Node> openSet = new PriorityQueue<>(new FScoreComparator());

	private final HashMap<Node, Boolean> inOpenSet = new HashMap<>();

	// For each node, which node it can most efficiently be reached from.
	// If a node can be reached from many nodes, cameFrom will eventually contain
	// the most efficient previous step.
	private final HashMap<Node, Node> cameFrom = new HashMap<>();
	
	private final Position start;
	private final Position goal;

	private final IMap m;
	
	private long elapsedTimeInNanos = 0;

	private List<Position> result = null;
	
	public AStarSearchInterruptible(Position start, Position goal, IMap m) {
		this.start = start;
		this.goal = goal;
		this.m = m;
	}
	
	/** Returns true if search complete, false otherwise */
	public final boolean startSearch(long expireTimeInNanos) {

		Node startNode = new Node(start);

		openSet.add(startNode);

		// // For each node, the cost of getting from the start node to that node.
		// HashMap<Node, Long> gScore = new HashMap<>();

		// // The cost of going from start to start is zero.
		startNode.g_score = 0;

		// For each node, the total cost of getting from the start node to the goal
		// by passing by that node. That value is partly known, partly heuristic.
		// HashMap<Node, Long> fScore = new HashMap<>();

		// For the first node, that value is completely heuristic.
		startNode.f_score = heuristicCostEstimate(start, goal);
		
		boolean searchFinished = doThing(expireTimeInNanos);		
		
		return searchFinished;
	}
	
	/** Returns true if search complete, false otherwise */
	public final boolean continueSearch(long expireTimeInNanos) {
		boolean result = doThing(expireTimeInNanos);
		
		return result;
	}

	/** Returns true if search complete, false otherwise */
	private final boolean doThing(long expireTimeInNanos) {
		
		long count = 0;
		
		long startTimeInNanos = System.nanoTime();
		
		while (!openSet.isEmpty()) {

			// Check if time is expired
			count++;
			if(count == 8192) {
				count = 0;
				if(System.nanoTime() > expireTimeInNanos) {
					elapsedTimeInNanos += System.nanoTime() - startTimeInNanos;
					return false;
				}
			}
			
			// current := the node in openSet having the lowest fScore[] value
			Node current = openSet.remove();
			inOpenSet.remove(current);

//			m.putTile(current.p, new Tile(TileType.BRICK_FENCE_STRAIGHT_VERT_LEFT));
						
			if (current.p.equals(goal)) {
				result = reconstructPath(cameFrom, current);
				elapsedTimeInNanos += System.nanoTime() - startTimeInNanos;
				return true;
			}

			closedSet.put(current, true);

			for (Node neighbour : getNeighbours(current, m)) {

				// Ignore the neighbor which is already evaluated.
				if (closedSet.containsKey(neighbour)) {
					continue;
				}

				if (!inOpenSet.containsKey(neighbour)) {

					Tile t = m.getTile(neighbour.p);
					if(t.isPresentlyPassable()) {
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
		
		elapsedTimeInNanos += System.nanoTime() - startTimeInNanos;
		
		return true;

	}
	
	public long getElapsedTimeInNanos() {
		return elapsedTimeInNanos;
	}
	
	public final List<Position> getResult() {
		return result;
	}
	
	private static final List<Node> getNeighbours(Node current, IMap m) {
		List<Node> list = new ArrayList<Node>();

		Position p = current.p;

		// Left
		Position left = new Position(p.getX() - 1, p.getY());
		if (left.isValid(m)) {
			list.add(new Node(left));
		}

		// Right
		Position right = new Position(p.getX() + 1, p.getY());
		if (right.isValid(m)) {
			list.add(new Node(right));
		}

		// Up
		Position up = new Position(p.getX(), p.getY() - 1);
		if (up.isValid(m)) {
			list.add(new Node(up));
		}

		// Down
		Position down = new Position(p.getX(), p.getY() + 1);
		if (down.isValid(m)) {
			list.add(new Node(down));
		}

		return list;
	}

	private static final List<Position> reconstructPath(HashMap<Node, Node> cameFrom, Node current) {

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

	private static final class FScoreComparator implements Comparator<Node> {

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
