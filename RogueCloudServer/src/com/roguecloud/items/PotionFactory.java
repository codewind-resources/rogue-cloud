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

import com.roguecloud.items.Effect;
import com.roguecloud.items.Effect.EffectType;

/** Generate random Effect objects, for use in potions. */
public class PotionFactory {

	private static enum Magnitude {
		POSITIVE, NEGATIVE;
		
	};

	private static final Distribution[] effectDistribution = new Distribution[] {
			new Distribution(EffectType.LIFE, Magnitude.POSITIVE, 30),
			new Distribution(EffectType.DAMAGE_REDUCTION, Magnitude.POSITIVE, 30),
//			new Distribution(EffectType.VISION_RANGE, Magnitude.POSITIVE, 10)
			};

	public static Effect generate() {
		
		int random;
		{
			int total = 0;
			for(Distribution d : effectDistribution) {
				total += d.getRatio();
			}
			
			random = (int)(Math.random() * total);
		}
		
		
		Distribution match = null;
		
		int curr = 0;
		for(Distribution d : effectDistribution) {
			if(random > curr && random <  curr + d.getRatio()) {
				match = d;
				break;
			}
		}
		
		if(match == null) {
			match = effectDistribution[effectDistribution.length-1];
		}
		
		int magnitude = 0;
		int duration = 0;
		
		if(match.getType() == EffectType.LIFE) {
			magnitude = (int)(Math.random() * 100);
			duration = 1;
			
		} else if(match.getType() == EffectType.DAMAGE_REDUCTION) {
			magnitude = (int)(Math.random()*30)+10;
			duration = (int)(Math.random() * 200);
			
		} else if(match.getType() == EffectType.VISION_RANGE) {
			magnitude = 10;
			duration = (int)( Math.random() * 400);
			
		}
		
		Effect e = new Effect(match.type, magnitude);
		e.setRemainingTurns(duration);
		
		return e;
	}

	/** The effect types that this potion factory will generate, whether they are positive/negative, and how
	 * strong they are (magnitude). These are hardcoded in 'effectDistribution' above.  */
	private static class Distribution {

		private final EffectType type;
		private final Magnitude magnitude;
		private final int ratio;

		public Distribution(EffectType type, Magnitude magnitude, int ratio) {
			this.type = type;
			this.magnitude = magnitude;
			this.ratio = ratio;
		}

		public EffectType getType() {
			return type;
		}

		@SuppressWarnings("unused")
		public Magnitude getMagnitude() {
			return magnitude;
		}

		public int getRatio() {
			return ratio;
		}

	}

}
