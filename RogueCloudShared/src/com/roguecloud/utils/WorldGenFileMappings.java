package com.roguecloud.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** See github documentation for a description of mappings, their purpose, and the 
 * file format. */
public class WorldGenFileMappings {
	
	private final List<WorldGenFileMappingEntry> mappings; 

	public WorldGenFileMappings(InputStream is) throws IOException {
	
		List<WorldGenFileMappingEntry> localMappings = new ArrayList<>();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String str;
		while(null != ( str = br.readLine())) {
			
			String trimmedStr = str.trim();
			
			if(trimmedStr.startsWith("#")) { continue; } // Skip comments
			
			List<String> tokens = tokenizeInto3Blocks(str);
			
			if(tokens == null || tokens.isEmpty()) { continue; }
			
			String letter = tokens.get(0);
			String colour = tokens.get(1);
			String name = tokens.size() > 2 ? tokens.get(2) : null;
			
			if(name != null && !name.trim().isEmpty() && !name.trim().startsWith("#")) {
				// valid name
			} else {
				name = null; 
			}

			localMappings.add(new WorldGenFileMappingEntry(letter, colour, name));
			
		}
		
		br.close();
		
		this.mappings = Collections.unmodifiableList(localMappings);
		
	}
	

	public WorldGenFileMappingEntry getByColour(String colour) {
		return mappings.stream().filter( e -> e.getColour().equalsIgnoreCase(colour)).findFirst().orElse(null);
	}

	public WorldGenFileMappingEntry getByLetter(String letter) {
		return mappings.stream().filter( e -> e.getLetter().equals(letter)).findFirst().orElse(null);
	}
	
	public WorldGenFileMappingEntry getByRoomName(String roomName) {
		return mappings.stream().filter( e -> e.getRoomName().equals(roomName)).findFirst().orElse(null);
	}
	
//	public static void main(String[] args) {
//		
//		List<String> tokens = tokenizeInto3Blocks("a  \t 4d08ff  \t  New House SE");
//		
//		tokens.forEach(e -> { System.out.println(e);});
//		
//	}
	
	
	/*
	 * A tokenization that preserves whitespace after the first two tokens.
	 * 
	 * Convert "a 	4d08ff 	New House SE" to { "a", "4d08ff", "New House SE" }
	 */
	private static List<String> tokenizeInto3Blocks(String str) {
		
		str = str.trim();

		String currToken = null;
		
		List<String> tokens = new ArrayList<>();
		
		for(int x = 0; x < str.length(); x++) {
			char ch = str.charAt(x);
			
			if(Character.isWhitespace(ch) && tokens.size() <= 1) {
				
				if(currToken != null) {
					tokens.add(currToken.trim());
					currToken = null;
				}
			} else {
				if(currToken == null) { currToken = ""; }
				currToken += ch;
			}
		}
		
		if(currToken != null) {
			tokens.add(currToken.trim());
		}
		
		return tokens;
		
	}
	
	
	public static class WorldGenFileMappingEntry {
		private String letter;
		private String colour;
		private String roomName;
		
		public WorldGenFileMappingEntry(String letter, String colour, String roomName) {
			this.letter = letter;
			this.colour = colour;
			this.roomName = roomName;
		}

		public String getLetter() {
			return letter;
		}

		public String getColour() {
			return colour;
		}

		public String getRoomName() {
			return roomName;
		}
		
	}
	
	
}
