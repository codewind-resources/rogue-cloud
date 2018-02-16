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
import com.roguecloud.items.Weapon;
import com.roguecloud.map.TileType;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.UniverseParserUtil;

public class WeaponList {

	private final Logger log = Logger.getInstance(); 
	
	private final List<Weapon> list;	 
	
	public WeaponList(UniqueIdGenerator generator, LogContext lc) throws IOException {
		
		final List<Weapon> parsedWeapons = new ArrayList<>();

		UniverseParserUtil.parseItemFile( (currType, commaSepArr, line) -> {
			if(commaSepArr.size() != 6) {
				log.severe("Unable to parse weapon line, due to not enough values: "+line, lc);
				return;
			}
			
			try {
				
				//  name, numAttackDice, attackDiceSize, attackPlus, hitRating, weapon type, tile type
				Weapon weapon = new Weapon(
						generator.getNextUniqueId(IdType.OBJECT),
						commaSepArr.get(0).trim(),
						Integer.parseInt(commaSepArr.get(1).trim()), // numAttackDice
						Integer.parseInt(commaSepArr.get(2).trim()), // size
						Integer.parseInt(commaSepArr.get(3).trim()), // plus
						Integer.parseInt(commaSepArr.get(4).trim()), // hit
						Weapon.WeaponType.getByName(currType), 
						new TileType(Integer.parseInt(commaSepArr.get(5).trim()))
				);
				
				parsedWeapons.add(weapon);
				
			} catch(NumberFormatException e) {
				log.severe("Unable to parse weapon line: "+line, e, lc);
				return;
			}
			
		}, "/universe/weapons.txt");

		log.interesting("Parsed "+parsedWeapons.size()+" weapons.", lc);

		this.list = Collections.unmodifiableList(parsedWeapons);

	}

	public List<Weapon> getList() {
		return list;
	}
}
