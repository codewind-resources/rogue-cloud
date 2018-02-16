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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import javax.imageio.ImageIO;

public class PngMain {

	public static void main(String[] args) {
		
		try {
			
			PngState state = new PngState();
			
			String[][] pairs = new String[][] {
					{"916f21", "r"},
					{"4d08ff", "a"},
					{"00ff90", "b"},
					{"fdfdff", "."}, 
					{"ff2100", "c"},
					{"cf60ff", "d"},
					{"ffb459", "e"},
					{"c55bff", "f"},
					{"a0a0a0", "g"},
					{"00ff43", "h"}
			};
			
			Arrays.asList(pairs).stream().forEach( e -> {  state.colourToLetter.put(e[0], e[1]); });
			
			doInner(state);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void doInner(PngState state) throws IOException {
		BufferedImage bi = ImageIO.read(new File("c:\\delme\\map.png"));

		SimpleMap<Entry> map = new SimpleMap<Entry>(bi.getWidth(), bi.getHeight());

//		HashMap<String, Boolean> coloursSeenMap = new HashMap<>();
		
//		Raster r = bi.getData();
		for(int y = 0; y < bi.getHeight(); y++) {
			for(int x = 0; x < bi.getWidth(); x++) {
				
				Entry e = new Entry();
				
				e.letter = ".";
				
				map.putTile(x, y, e);
				
				int val = bi.getRGB(x, y);
				
				if(val != -1) {
					String hex = Integer.toHexString(val).substring(2);
					
					String letter = state.colourToLetter.get(hex);
					if(letter == null) {
						System.out.println("Unrecognized colour: "+hex);
					} else {
						e.letter = letter;
					}
					
//					coloursSeenMap.put(hex, true);
//					System.out.println(val);
				}
			}
		}
		
		
		StringBuilder str = new StringBuilder();
		for(int y = 0; y < map.getYSize(); y++) {
			for(int x = 0; x < map.getXSize(); x++) {
				String letter =map.getTile(x, y).letter;;
				
				if(letter == null) {
					System.err.println("null letter at "+x+" "+y);
					return;
				} 
				
				str.append(letter);
			}
			str.append("\n");
		}
		System.out.println(str);
		
		FileWriter fw = new FileWriter("C:/Hackathon/Git/RogueCloudServer/WebContent/universe/map.txt");
		fw.write(str.toString());
		fw.close();
		
		
//		coloursSeenMap.keySet().stream().sorted().forEach(e -> {
//			System.out.println(e);
//		});
		
		
	}

	public static final class SimpleMap<T> {

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

	}

	public static class PngState {
		HashMap<String, String> colourToLetter = new HashMap<>();
				
	}
	
	public static class Entry {

		String letter;
	}
}
