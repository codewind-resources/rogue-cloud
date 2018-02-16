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

/** A utility class that may be used to store/retrieve elements from a map-style grid of an arbitrary elements. */
public final class SimpleMap<T> {

	private final int xSize, ySize;

	private final T[] tileArray;

	@SuppressWarnings("unchecked")
	public SimpleMap(int xSize, int ySize) {
		this.xSize = xSize;
		this.ySize = ySize;
		tileArray = (T[]) new Object[xSize * ySize];
	}

	public final void putTile(int x, int y, T t) {
		tileArray[x * ySize + y] = t;
	}

	public final T getTile(int x, int y) {
		int index = x * ySize + y;

		if (tileArray.length <= index || index < 0) {
			return null;
		}

		T tile = tileArray[index];
		return tile;
	}

	public final int getXSize() {
		return xSize;
	}

	public final int getYSize() {
		return ySize;
	}

	
	@Override
	public String toString() {
		return dumpMap(new SimpleMapPrettyPrint<T>() {
			
			@Override
			public String convertTileToString(T t) {
				String letter = "";
				if(t == null) {
					letter = "!";
				} else {
					letter = t.toString();
				}
				return letter;
			}
		});
	}
	
	public String dumpMap(SimpleMapPrettyPrint<T> pp) {
		StringBuilder str = new StringBuilder();
		
		for(int y = 0; y < getYSize(); y++) {
			for(int x = 0; x < getXSize(); x++) {
				
				String letter = "";
				
				T t = getTile(x, y);
				letter = pp.convertTileToString(t);
				
				str.append(letter);
			}
			str.append("\n");
		}
		
		return str.toString();

	}
	
	/** See parent class for details. */
	public static interface SimpleMapPrettyPrint<T> {
		public String convertTileToString(T a);
	}
}
