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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.roguecloud.utils.SimpleMap;
import com.roguecloud.utils.WorldGenFileMappings;
import com.roguecloud.utils.WorldGenFileMappings.WorldGenFileMappingEntry;

/** Convert a .PNG file into a map.txt file, based on coloured pixels that appear in the PNG file. */
public class PngMain {

	public static void main(String[] args) {
		
		try {
			
			if(args.length != 1) {
				System.out.println("* One argument required: (path to universe directory of RogueCloudServer) ");
				System.out.println("  Example: C:\\Rogue-Cloud\\Git\\RogueCloudServer\\WebContent\\universe\\");
				return;
			}
			
			System.out.println("* Generating from "+args[0]);
			
			File serverDir = new File(args[0]);
			
			File pngFile = new File(serverDir, "map-new.png");
			
			File outputFile = new File(serverDir, "map-new.txt");

			WorldGenFileMappings mappings = new WorldGenFileMappings(new FileInputStream(new File(serverDir, "map-new-mappings.txt"))); 
			
			PngState state = new PngState(outputFile, pngFile, mappings);
			
			doInner(state);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void doInner(PngState state) throws IOException {
		BufferedImage bi = ImageIO.read(state.pngFile);

		SimpleMap<PngMainEntry> map = new SimpleMap<PngMainEntry>(bi.getWidth(), bi.getHeight());

		for(int y = 0; y < bi.getHeight(); y++) {
			for(int x = 0; x < bi.getWidth(); x++) {
				
				PngMainEntry e = new PngMainEntry();
				
				e.letter = ".";
				
				map.putTile(x, y, e);
				
				int val = bi.getRGB(x, y);
				
				if(val != -1) {
					String hex = Integer.toHexString(val).substring(2);
					
					if(hex.equalsIgnoreCase("fdfdff")) { continue; } // This colour is known ignorable
					
					WorldGenFileMappingEntry mapping = state.mappings.getByColour(hex); 
					
					if(mapping == null) {
						System.err.println("Unrecognized colour: "+hex+" @ ("+x+", "+y+")");
						
					} else {
						
						String letter = mapping.getLetter();
						if(letter == null) {
							System.out.println("Unrecognized colour: "+hex+" @ ("+x+", "+y+")");
						} else {
							e.letter = letter;
						}					
					}
					
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
		
		FileWriter fw = new FileWriter(state.outputFile); 
		fw.write(str.toString());
		fw.close();
				
	}

	/** Various fields used by the algorithm*/
	private static class PngState {
		
		private final File outputFile;

		private final File pngFile;

		private final WorldGenFileMappings mappings;
		
		public PngState(File outputFile, File pngFile, WorldGenFileMappings mappings) {
			this.outputFile = outputFile;
			this.pngFile = pngFile;
			this.mappings = mappings;
		}
		
	}

	/** The letter at a specific coordinate on the map. */
	private static class PngMainEntry {
		String letter;
	}
}
