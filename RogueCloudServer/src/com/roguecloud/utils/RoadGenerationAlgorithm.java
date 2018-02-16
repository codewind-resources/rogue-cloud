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
import java.util.List;

public class RoadGenerationAlgorithm {

	final int X_SIZE;
	final int Y_SIZE;
	
	final int GRID_SIZE;

	final int mainRoadLengthInEdges;
	
	private int[][] resultMap;
	
	private final List<DirectedEdge> resultEdges = new ArrayList<>();
	
	public RoadGenerationAlgorithm(int xSize, int ySize, int gridSize, int mainRoadLengthInEdges) {
		X_SIZE = xSize;
		Y_SIZE = ySize;
		GRID_SIZE = gridSize;
		this.mainRoadLengthInEdges = mainRoadLengthInEdges;
	}
	
	public static void main(String[] args) {
		RoadGenerationAlgorithm a = new RoadGenerationAlgorithm(161, 81, 10, 21);
	
		a.runAlgorithm();
		int[][] map = a.getResultMap();  
		
		a.printMap(map);
		
	}
	
	
	public void runAlgorithm() {
		
		int[][] map = new int [X_SIZE][Y_SIZE];
		
		int iterations = 0;
		List<DirectedEdge> roadEdge;
		do {
			roadEdge = generate();
			iterations++;
		} while(roadEdge.size() < mainRoadLengthInEdges );
		

		System.out.println("* Found in "+iterations +" iterations.");

		for(int x = 0; x < 5; x++) {
			System.out.println("Adding new edge");
			List<DirectedEdge> newEdge;
			do {
				newEdge = extend(roadEdge);
			} while(newEdge.size() < 2);
			
			System.out.println("-----------------------");
			
			for(DirectedEdge e : newEdge) {
				System.out.println(" * "+e);
			}
			
			roadEdge.addAll(newEdge);
		}
		
		
		for(DirectedEdge de : roadEdge) {
			
			System.out.println(de);
			
			if(de.isHorizontal()) {
				int y = de.getSrc().getY()* GRID_SIZE;
				
				int startX = de.getSrc().getX()*GRID_SIZE;
				int endX = de.getDest().getX()* GRID_SIZE;
				
				resultEdges.add(new DirectedEdge(new Coord(startX, y), new Coord(endX-1, y)));
				
				for(int x = startX; x < endX; x++) {
					map[x][y] = 1;
				}
				
			} else {
				int startY =  de.getSrc().getY() * GRID_SIZE;
				int endY =  de.getDest().getY() * GRID_SIZE;
				
				if(startY > endY) {
					int c = startY;
					startY = endY;
					endY = c;
				}
				
				int x = de.getSrc().getX() * GRID_SIZE;
				
				
				resultEdges.add(new DirectedEdge(new Coord(x, startY), new Coord(x, endY)));
				
				for(int y = startY; y <= endY; y++) {
					map[x][y] = 1;
				}
				
			}
		}
		
//		printMap(map);
		
		resultMap = map;
		
	}
	
	private List<DirectedEdge> extend(List<DirectedEdge> currEdges) {
		
		final List<DirectedEdge> result = new ArrayList<DirectedEdge>();
		
		int edgeToStartAt = (int)(Math.random() *  (currEdges.size()-2))+1;
		
		DirectedEdge start = currEdges.get(edgeToStartAt);
		
		int currX = start.getDest().getX();
		int currY = start.getDest().getY();
		
		int lastX, lastY;
		
		while(true) {
		
			lastX = currX;
			lastY = currY;

			// pick a direction
			int dir = (int)(Math.random()*3d);
			if(dir == 0) {
				// up
				currY--;
			} else if(dir == 1) {
				// right
				currX++;
			} else if(dir == 2) {
				// down
				currY++;
			}
			
			if(result.size() > 0) {
				Coord src = result.get(result.size()-1).src;
				if(src.getX() == currX && src.getY() == currY) {
					currX = lastX;
					currY = lastY;
					continue;
				}
			} else {
				if(currX == start.src.getX() && currY == start.src.getY()) {
					continue;
				}
			}
			
			
			if(currX == X_SIZE/GRID_SIZE+1) {
				// hit a side, so done.
				break;
			}
			
			
			
			if(currY  < 0 || currY > (Y_SIZE/GRID_SIZE)) {
				// hit a side, so done.
				break;
			}

//			final int finalCurrX = currX, finalCurrY = currY;
//			if(currEdges.stream().anyMatch( e -> e.src.x == finalCurrX && e.src.y == finalCurrY)) {
//				break;
//			}
			
			
			{
				Coord src = new Coord(lastX, lastY);
				Coord dest = new Coord(currX, currY);
				result.add(new DirectedEdge(src, dest));
			}

			final int finalCurrX = currX, finalCurrY = currY;
			if(currEdges.stream().anyMatch( e -> e.src.x == finalCurrX && e.src.y == finalCurrY)) {
				break;
			}

		}
		
		return result;
		
	}

	private List<DirectedEdge> generate() {
		int lastX = -1, lastY = -1;
		
		int currX = 0, currY = 2;
		
		List<DirectedEdge> roadEdge = new ArrayList<>();
		
		
		while(true) {
		
			lastX = currX;
			lastY = currY;
			
			// pick a direction
			int dir = (int)(Math.random()*3d);
			if(dir == 0) {
				// up
				currY--;
			} else if(dir == 1) {
				// right
				currX++;
			} else if(dir == 2) {
				// down
				currY++;
			}
			
			if(roadEdge.size() > 0) {
				Coord src = roadEdge.get(roadEdge.size()-1).src;
				if(src.getX() == currX && src.getY() == currY) {
					currX = lastX;
					currY = lastY;
					continue;
				}
			}
			
			if(currX == X_SIZE/GRID_SIZE+1) {
				// hit a side, so done.
				break;
			}
			
			if(currY  < 0 || currY > (Y_SIZE/GRID_SIZE)) {
				// hit a side, so done.
				break;
			}

			
			{
				Coord src = new Coord(lastX, lastY);
				Coord dest = new Coord(currX, currY);
				roadEdge.add(new DirectedEdge(src, dest));
			}
			
		}
		
		return roadEdge;

	}
	
	public void printMap(int[][] map) {
		for (int y = 0; y < Y_SIZE; y++) {

			for (int x = 0; x < X_SIZE; x++) {

				if(map[x][y] == 1) {
					System.out.print("#");
				} else {
					System.out.print(".");
				}

			}

			System.out.println();

		}

	}
	
	public List<DirectedEdge> getResultEdges() {
		return resultEdges;
	}
	
	public int[][] getResultMap() {
		return resultMap;
	}
	
	public static class DirectedEdge {
		private final Coord src;
		
		private final Coord dest;

		public DirectedEdge(Coord src, Coord dest) {
			this.src = src;
			this.dest = dest;
		}
		
		public Coord getDest() {
			return dest;
		}
		
		public Coord getSrc() {
			return src;
		}
		
		
		public boolean isHorizontal() {
			if(src.getY() == dest.getY()) {
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		public String toString() {
			return src+" -> "+" "+dest;
		}
		
		
	}
	
	public static class Coord {
		private final int x;
		private final int y;
		
		
		public Coord(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public int getX() {
			return x;
		}
		public int getY() {
			return y;
		}
		
		@Override
		public String toString() {
			return "( "+x+", "+y+")";
		}
		
		
	}
}
