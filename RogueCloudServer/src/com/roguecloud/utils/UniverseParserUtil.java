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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.roguecloud.utils.RCUtils;

public class UniverseParserUtil {
	
	public static void parseItemFile(ItemParserHandler handler, String resourceUrl) throws IOException {
		
		InputStream inputStream = UniverseParserUtil.class.getClassLoader().getResourceAsStream(resourceUrl);
		
		String[] arr = RCUtils.readIntoString(inputStream).split("\\r?\\n");
		
		String currType = null;
		
		for(String line : arr) {
			
			line = line.trim();
			
			if(line.startsWith("#") || line.startsWith("//")) {
				continue;
			}  else if(line.endsWith(":")) {
				currType = line.substring(0, line.length()-1);
				
			} else if(line.contains(",")) {
				
				List<String> commaSepArr = splitByCommas(line);
				handler.line(currType, commaSepArr, line);
			}
			
		}
		
	}

	
	public static List<String> splitByCommas(String str) {
		List<String> result = new ArrayList<>();
		
		StringBuilder curr = new StringBuilder();
		
		for(int x = 0; x < str.length(); x++) {
			char ch = str.charAt(x);
			if(ch == ',') {
				result.add(curr.toString());
				curr = new StringBuilder();
			} else {
				curr.append(ch);
			}
		}
		
		result.add(curr.toString());
		
		return result;
	}
	
	public static interface ItemParserHandler {
		
		public void line(String currType, List<String> commaSepArr, String line);
	}
}
