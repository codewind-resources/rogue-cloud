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

package com.roguecloud.creatures;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.roguecloud.map.TileType;
import com.roguecloud.resources.Resources;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.UniverseParserUtil;

/**
 * Parses monsters from the monsters.txt file, and stores them in this class as an immutable list.
 */
public class MonsterTemplateList {

	private final Logger log = Logger.getInstance(); 
	
	private final List<MonsterTemplate> list;	 
	
	public MonsterTemplateList(LogContext lc) throws IOException {
		
		final List<MonsterTemplate> parsedMonsterTemplates = new ArrayList<>();

		UniverseParserUtil.parseItemFile( (currType, commaSepArr, line) -> {
			
			if(commaSepArr.size() != 4) {
				log.severe("Unable to parse MonsterTemplate line, due to not enough values: "+line, lc);
				return;
			}
			
			try {
								
				MonsterTemplate mt = new MonsterTemplate(
						commaSepArr.get(0).trim(),
						Integer.parseInt(commaSepArr.get(1).trim()),
						Integer.parseInt(commaSepArr.get(2).trim()), 
						new TileType(Integer.parseInt(commaSepArr.get(3).trim()))
						);
				
				if(Resources.getInstance().isValidTile(mt.getTileType().getNumber())) {
					parsedMonsterTemplates.add(mt);					
				} else {
					log.severe("Unable to find tile for monster "+mt.getTileType().getNumber(), null);
				}
				
			} catch(NumberFormatException e) {
				log.severe("Unable to parse MonsterTemplate line: "+line, e, lc);
				return;
			}
			
		}, "/universe/monsters.txt");

		log.interesting("Parsed "+parsedMonsterTemplates.size()+" monsters.", lc);

		this.list = Collections.unmodifiableList(parsedMonsterTemplates);

	}

	public List<MonsterTemplate> getList() {
		return list;
	}
	
	public MonsterTemplate getByName(String str) {
		if(list == null) { return null; }
		
		return list.stream().filter(e -> e.getName() != null && e.getName().equalsIgnoreCase(str)).findFirst().orElse(null);
				
	}
}
