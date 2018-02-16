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

import com.roguecloud.map.TileType;

/**
 * This is the parent interface for all objects.  
 * 
 * There are currently three types of objects and these are the classes that implement this interface:
 * 
 * - Armour
 * - Weapon
 * - DrinkableItem
 *
 * An object may be either on the ground (in which case it is contained inside an IGroundObject) or in an player's
 * inventory (in which case it is inside an OwnableObject). 
 * 
 **/
public interface IObject {
	
	/** The type of the object, which can be used to determine which class it is: Armour, Weapon, or DrinkableItem. */
	public ObjectType getObjectType();
	
	/** Unique ID of the object */
	public long getId();
	
	/** The name of the object (for example, Short Sword) */
	public String getName();

	/** For internal use only: the graphical tile that represents the object. */
	public TileType getTileType();

	/** The type of the object, which can be used to determine which class it is: Armour, Weapon, or DrinkableItem. */
	public static enum ObjectType {
		WEAPON, ARMOUR, ITEM
	}

}
