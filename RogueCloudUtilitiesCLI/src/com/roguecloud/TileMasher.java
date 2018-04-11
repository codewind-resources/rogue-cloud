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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/** 
 * Copy files from the tiles directory into the appropriate path in the resources project. 
 * - Only copy from 'in-use' directory of the input tiles directory
 * - Don't replace files that already exist; if a file exists and has differing file contents between src and dest, then print a warning.  
 **/
public class TileMasher {

	public static void main(String[] args) {
		
		try {
			innerRun();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void innerRun() throws IOException {
		
		File inputDir = new File("C:\\Hackathon\\tiles");
		
		File outputDir = new File("C:/Rogue-Cloud/Git/RogueCloudResources/src/com/roguecloud/resources/tiles");
		
		for(File tileSource : inputDir.listFiles()) {
			
			File inUse = new File(tileSource, "in-use");
			
			if(!inUse.exists()) { continue; }
			
			for(File f : inUse.listFiles()) {
				

				int number = -1;
				{
					String text = f.getName();
					if(text.contains("-")) {
						text = text.substring(0, text.indexOf("-"));
					} else {
						text = text.substring(0, text.indexOf(".png"));
					}
					
					try {
						number = Integer.parseInt(text);
					} catch(NumberFormatException nfe) {
						System.err.println("* Ignoring filename "+f.getName());
						continue;
					}
				}
								
				File outputFile = new File(outputDir, number+".png"); 
				
				if(outputFile.exists()) {
					 if(!isFileContentsEqual(f, outputFile)) {
						 System.err.println("Overwriting file w/ new contents: "+f.getName());	 
					 }
				} 
				
//				else {
					copyFile(f, outputFile);	
//				}
				
			}
			
			
		}
		
	}
	
	private static boolean isFileContentsEqual(File sourceFile, File destFile) throws IOException {
		FileInputStream sourceFis = new FileInputStream(sourceFile);
		FileInputStream destFis = new FileInputStream(destFile);
		
		try {
			while(true) {
				
				int a = sourceFis.read();
				int b = destFis.read();
				
				if(a != b) {
					return false;
				}
				
				if(a == -1) {
					break;
				}
				
			}
		
		} finally {
			sourceFis.close();
			destFis.close();
		}
		
		return true;
		
	}
	
	private static void copyFile(File sourceFile, File destFile) throws IOException {
		
		FileInputStream fis = new FileInputStream(sourceFile);
		
		FileOutputStream fos = new FileOutputStream(destFile);
		
		byte[] barr = new byte[32 * 1024];
		int c;
		
		while( -1 != (c = fis.read(barr))) {
			
			fos.write(barr, 0, c);
			
		}
		
		fis.close();
		
		fos.close();
		
		
	}
}
