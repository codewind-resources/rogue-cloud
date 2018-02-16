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

import com.roguecloud.creatures.IExistsInWorld;
import com.roguecloud.map.IMap;

/** 
 * Represents an object laying on the ground at an in-world coordinate (eg it is not in someone's inventory, or in a container).
 * 
 * The armor/weapon/potion object itself can be retrieved by calling the get() method. 
 * 
 **/
public interface IGroundObject extends IExistsInWorld {

	/** The object laying on the ground. The return value will be of type Armour, Weapon, or DrinkableItem. See IObject.getObjectType().  */
	public IObject get();
	
	/** Unique object id */
	public long getId();
	

	/** If a ground object has not been recently seen by the character, it is called 'stale'. 
	 * More information on staleness is available from the Github documentation. */
	public long getLastTickUpdated(IMap map);
}
