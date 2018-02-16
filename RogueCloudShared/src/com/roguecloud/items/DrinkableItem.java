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

import com.roguecloud.json.JsonDrinkableItem;
import com.roguecloud.map.TileType;

/** An item that can be drank, such as a potion, and that has a positive or negative effect on the affected character. 
 * 
 * The effect of drinking the potion may be retrieved by calling getEffect(...).
 **/
public class DrinkableItem implements IObject {

	private final TileType tileType;
	
	private final Effect effect;
	
	private final long id;
	
	public DrinkableItem(TileType tileType, Effect currentEffect, long id) {
		this.tileType = tileType;
		this.effect = currentEffect;
		this.id = id;
	}
	
	public DrinkableItem(JsonDrinkableItem jdi) {
		this.effect = new Effect(jdi.getEffect());
		this.id = jdi.getId();
		this.tileType = new TileType(jdi.getTile());
	}

	/**
	 * The effect of drinking the potion may be retrieved by calling this method.  
	 * (Internal note only: Object may be mutated through the returned reference) */
	public Effect getEffect() {
		return effect;
	}
	
	/** Return a user friendly description of the potion, it's effect, magnitude, and duration. */
	public String getName() {
		return "Vial of "+effect.getName()+" ("+effect.getMagnitude()+" for "+effect.getRemainingTurns()+" turn"+(effect.getRemainingTurns() != 1 ? "s" : "")+")";
	}
	
	@Override
	public ObjectType getObjectType() {
		return ObjectType.ITEM;
	}

	/** A unique ID for this specific item. */
	@Override
	public long getId() {
		return id;
	}

	
	// Internal methods ----------------------------------------------------------

	@Override
	public TileType getTileType() {
		return tileType;
	}
	
	public JsonDrinkableItem toJson() {
		JsonDrinkableItem result = new JsonDrinkableItem();
		result.setEffect(effect.toJson());
		result.setId(this.id);
		result.setTile(tileType.getNumber());
		
		return result;
	}

}
