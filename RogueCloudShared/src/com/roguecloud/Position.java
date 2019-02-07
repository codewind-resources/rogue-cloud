/*
 * Copyright 2018, 2019 IBM Corporation
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

import com.roguecloud.json.JsonPosition;
import com.roguecloud.map.IMap;

/** 
 * A simple (x, y) coordinate. This class is immutable and thus will not change. 
 * 
 * In this class, there are a number of helper utility methods, such as: finding the distance between two positions, whether or not a position
 * is contained within a (x1, y1) to (x2, y2) rectangle, among others. 
 **/
public final class Position {

	private final int x;
	private final int y;
	
	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public final int getX() {
		return x;
	}

	public final int getY() {
		return y;
	}
	
	@Override
	public final int hashCode() {
		return 32768*x + y;
	}
	
	public static final boolean isValid(int x, int y, IMap m) {
		if(x < 0) { return false; }
		if(y < 0) { return false; } 
		
		if(x > m.getXSize()-1) {
			return false;
		}
		
		if(y > m.getYSize()-1) {
			return false;
		}
		
		
		return true;
	}
	
	/** Whether the position is within the bounds of the world. */
	public final boolean isValid(IMap m) {
		return Position.isValid(this.x, this.y, m);
//		if(x < 0) { return false; }
//		if(y < 0) { return false; } 
//		
//		if(x > m.getXSize()-1) {
//			return false;
//		}
//		
//		if(y > m.getYSize()-1) {
//			return false;
//		}
//		
//		
//		return true;
	}
	
	
	@Override
	public final boolean equals(Object obj) {
		Position p = (Position)obj;
		
		if(x != p.x) { return false; }
		if(y != p.y) { return false; }
		
		return true;
		
	}

	@Override
	public final String toString() {
		return "("+x+", "+y+")";
	}

	/** Returns the Manhattan distance between two points: this is the distance without traveling diagonally. */
	public static final int manhattanDistanceBetween(int x1, int y1, int x2, int y2) {
		return Math.abs(x2 - x1) + Math.abs(y2 - y1);
	}
	
	/** Returns the Manhattan distance between two points: this is the distance without traveling diagonally. */
	public final int manhattanDistanceBetween(Position other) {
		return Math.abs(getX() - other.getX()) + Math.abs(getY() - other.getY());
	}
	
	/** Returns the absolute distance between two points */
	public final int distanceBetween(Position other) {
		return (int)Math.sqrt( Math.pow( getX() - other.getX() , 2) + Math.pow( getY() - other.getY(), 2) );
	}

	/** Whether the given position is contained with a box: (startX, startY) -> (startX+width-1, startY+height-1)*/
	public final static boolean containedInBox(Position p, int startX, int startY, int width, int height) {
		if(p.getX() < startX || p.getY() < startY) {
			return false;
		}
		
		if(p.getX() > startX+width-1) {
			return false;
		}
		
		if(p.getY() > startY+height-1) {
			return false;
		}
		
		return true;
	}

	/** Whether the given position is contained with a box: (startX, startY) -> (endX, endY), inclusive of endX and endY. */
	public final static boolean containedInBoxCoords(Position p, int startX, int startY, int endX, int endY) {

		if(p.getX() < startX || p.getX() > endX) {
			return false;
		}
		
		if(p.getY() < startY || p.getY() > endY) {
			return false;
		}
		
		return true;
	}

	
	/** Internal method onlu*/
	public final JsonPosition toJson() {
		JsonPosition jp = new JsonPosition();
		jp.setX(x);
		jp.setY(y);
		return jp;
	}
	
}
