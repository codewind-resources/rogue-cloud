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

import java.util.List;

import com.roguecloud.Position;
import com.roguecloud.items.ArmourSet;
import com.roguecloud.items.Effect;
import com.roguecloud.items.OwnableObject;
import com.roguecloud.items.Weapon;
import com.roguecloud.map.IMap;

/** 
 * This object represents a character in the world, whether it be your character, another player's character, or a enemy monster.
 * 
 * With this class, you can answer questions such as: 
 * 
 * - How much HP does this creature have? What is their maximum HP?
 * - What potion or positive/negative effects are active on them? 
 * - What weapons/armour are they using?
 * - Where are they located on the map?
 * - What experience level are they? (monsters only) 
 * 
 **/
public interface ICreature extends IExistsInWorld  {

	/** The maximum health of a character  */
	public int getMaxHp();
	
	/** The current health of a character; is this value is 0 then the character is dead. */
	public int getHp();
	
	/** What armour does the creature have equipped? */
	public ArmourSet getArmour();
	
	/** What weapon does the creature have equipped? */
	public Weapon getWeapon();

	/** Return a list of objects are in the creature's inventory (NOTE: You can only see _your own_ inventory, not other creatures!) */
	public List<OwnableObject> getInventory();

	/** A unique id for the creature; this will not change, and no other creatures will have this same ID. */
	public long getId();
	
	/** (Monsters only) What is the level of this character? A higher level implies they are better equipped to give and receive damage. */
	public int getLevel();
	
	/** Returns true if the character is no longer alive, and false otherwise. Note: Some characters (such as other players) may come back to life. */
	public boolean isDead();
	
	/** Whether or not the character is controlled by another player. */
	public boolean isPlayerCreature();
	
	/** The position in the world of the creature. */
	public Position getPosition();
	
	/** Return the active effects that are on the creature. */
	public List<Effect> getActiveEffects();

	/** Returns the name of the creature, for example, the name of another player. */
	public String getName();
	
	
	/** If a creature has not been recently seen by the character, it is called 'stale'. 
	 * More information on staleness is available from the Github documentation. */
	public long getLastTickUpdated(IMap map);
}
