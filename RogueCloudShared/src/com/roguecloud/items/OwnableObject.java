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

import com.roguecloud.json.JsonOwnableObject;

/** 
 * An object that is in a players inventory (ie unlike a GroundObject, an OwnableObject has no physical coordinates.)
 **/
public class OwnableObject {

	final long id;
	
	final IObject containedObject;

	public OwnableObject(IObject containedObject, long objectId) {
		if(containedObject == null || objectId < 0) { throw new IllegalArgumentException(); }
		
		this.containedObject = containedObject;
		this.id = objectId;
	}
	
	/** Unique ID for the object*/
	public long getId() {
		return id;
	}
	
	/** The object that is owned by the player, and in their inventory: an instance of Armour, Weapon or DrinkableItem. */
	public IObject getContainedObject() {
		return containedObject;
	}
	
	@Override
	public String toString() {
		return id+" containing "+containedObject.getId();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof OwnableObject)) {
			return false;
		}
		OwnableObject oo = (OwnableObject)obj;
		
		return oo.getId() == id;
	}
	
	// Internal methods ----------------------------------------------------------------------------------
	
	@Override
	public int hashCode() {
		return (int)(id);
	}
	
	public JsonOwnableObject toJson() {
		JsonOwnableObject result = new JsonOwnableObject();
		result.setId(this.id);
		result.setContainedObject(containedObject.getId());
		return result;
	}


}
