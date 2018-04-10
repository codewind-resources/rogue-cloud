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

package com.roguecloud.items;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.roguecloud.UniqueIdGenerator;
import com.roguecloud.UniqueIdGenerator.IdType;
import com.roguecloud.items.Armour;
import com.roguecloud.map.TileType;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.UniverseParserUtil;

/**
 * Parses armour from the armour.txt file, and stores them in this class as an immutable list.
 */
public class ArmourList {

	private final Logger log = Logger.getInstance(); 
	
	private final List<Armour> list;	 
	
	public ArmourList(UniqueIdGenerator idGenerator, LogContext lc) throws IOException {
		
		final List<Armour> parsedArmours = new ArrayList<>();

		UniverseParserUtil.parseItemFile( (currType, commaSepArr, line) -> {
			if(commaSepArr.size() != 3) {
				log.severe("Unable to parse item line: "+line, lc);
				return;
			}
			
			try {
				
				Armour armour = new Armour(idGenerator.getNextUniqueId(IdType.OBJECT), 
						commaSepArr.get(0).trim(), 
						Integer.parseInt(commaSepArr.get(1).trim()), 
						Armour.ArmourType.getByName(currType),
						new TileType(Integer.parseInt(commaSepArr.get(2).trim()))
				);
				parsedArmours.add(armour);
				
			} catch(NumberFormatException e) {
				log.severe("Unable to parse item line: "+line, lc);
				return;
			}
			
		}, "/universe/armour.txt");

		log.interesting("Parsed "+parsedArmours.size()+" armour.", lc);

		this.list = Collections.unmodifiableList(parsedArmours);

	}
	
	public List<Armour> getList() {
		return list;
	}
		
}
