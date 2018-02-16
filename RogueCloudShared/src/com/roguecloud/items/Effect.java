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

import com.roguecloud.json.JsonEffect;

/**
 * Some items, such as potions, have a direct effect on the user of that item. Effects can be positive (heal the player) or negative (hurt the player).
 * An effect may last a single turn, or linger for multiple turns.
 *
 * The aspects of an effect are:
 * 
 * - **Magnitude**: The strength of the effect. For example, if the effect is a heal, then magnitude is the number of HP healed per turn.
 * - **Remaining Turns**: For effects that last multiple turns, this is the number of remaining turns that the effect magnitude will apply.
 * - **Type**: The qualitative effect of the potion, such as healing the player, or reducing the damage taken.
 * 
 **/
public class Effect {
	
	private final Object lock = new Object();

	private final EffectType type;

	private int remainingTurns_synch_lock;

	private int magnitude_synch_lock;

	public Effect(EffectType type, int magnitude) {
		this.type = type;
		this.magnitude_synch_lock = magnitude;
	}

	public Effect(JsonEffect json) {
		this.remainingTurns_synch_lock = json.getRemainingTurns();
		this.magnitude_synch_lock = json.getMagnitude();
		this.type = EffectType.valueOf(json.getType());
	}

	/** How many turns will the effect last on the creature */
	public int getRemainingTurns() {
		synchronized (lock) {
			return remainingTurns_synch_lock;
		}
	}

	/** The strength of the effect: for positive effects, larger is better. */
	public int getMagnitude() {
		synchronized (lock) {
			return magnitude_synch_lock;
		}
	}

	public String getName() {
		synchronized (lock) {
			if (magnitude_synch_lock >= 0) {
				return type.positiveEffect;
			} else {
				return type.negativeEffect;
			}
		}
	}
	
	public EffectType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		synchronized(lock) {
			return getName()+" "+magnitude_synch_lock +" for "+remainingTurns_synch_lock+" turns.";
		}
	}
	
	/** The type of effect: does it heal or hurt, increase/decrease armour, etc. */
	public static enum EffectType {
		LIFE("Healing", "Poison"), VISION_RANGE("Eagle Sight", "Blindness"), DAMAGE_REDUCTION("Armour",
				"Britleness"), INVISIBILITY("Invisibility");
		private final String positiveEffect;
		private final String negativeEffect;

		private EffectType(String positiveEffect, String negativeEffect) {
			this.positiveEffect = positiveEffect;
			this.negativeEffect = negativeEffect;
		}

		private EffectType(String positiveEffect) {
			this.positiveEffect = positiveEffect;
			this.negativeEffect = null;
		}

		public String getPositiveEffect() {
			return positiveEffect;
		}

		public String getNegativeEffect() {
			return negativeEffect;
		}

	};
	
	// Internal methods --------------------------------------------------------------

	public void setRemainingTurns(int remainingTurns) {
		synchronized (lock) {
			this.remainingTurns_synch_lock = remainingTurns;
		}
	}

	public void setMagnitude(int magnitude) {
		synchronized (lock) {
			this.magnitude_synch_lock = magnitude;
		}
	}

	public JsonEffect toJson() {
		JsonEffect result = new JsonEffect();
		synchronized(lock) {
			result.setMagnitude(this.magnitude_synch_lock);
			result.setRemainingTurns(this.remainingTurns_synch_lock);
			result.setType(this.type.name());
		}
		return result;
	}
	
	public Effect fullClone() {
		synchronized(lock) {
			Effect result = new Effect(type, magnitude_synch_lock);
			result.setRemainingTurns(this.remainingTurns_synch_lock);
			return result;
		}
	}


}
