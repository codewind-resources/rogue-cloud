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

package com.roguecloud;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import com.roguecloud.creatures.IMutableCreature;
import com.roguecloud.items.Weapon;
import com.roguecloud.items.Effect.EffectType;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;

public class Combat {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getInstance();

	public static CombatResult doCombat(IMutableCreature attacker, IMutableCreature defender, LogContext lc) {
		
		SecureRandom sr = new SecureRandom();
		Random r = new Random(sr.nextLong());
		
		Weapon w = attacker.getWeapon();
		
		int totalDefense = defender.getArmour().getAll().stream().mapToInt( (e) -> e.getDefense()).sum();
		
		int damage = 0;
		for(int x = 0; x < w.getNumAttackDice(); x++) {
			
			damage += r.nextInt(w.getAttackDiceSize());
		}
		damage += w.getAttackPlus();
		
		final AtomicReference<Double> damageReduction = new AtomicReference<Double>(1d);
		{
			defender.getActiveEffects().stream().filter(e -> e.getType() == EffectType.DAMAGE_REDUCTION).forEach( e -> {
				damageReduction.set( damageReduction.get() * (1 -  ((double)e.getMagnitude()/100d)   ));
			});
		}
		damage = (int)(damage * damageReduction.get());		
		
//		log.info("Reducing damage by "+(int)(damageReduction.get()*100), lc);
		
		boolean hit = false;
		double hitChance = ((double)w.getHitRating() / (double)totalDefense);
		
		if(r.nextDouble()*1d <= hitChance) {
			hit = true;
			// hit, no damage mitigation
		} else {
			// miss
			damage = 0;
		}
		
		defender.setCurrHp( defender.getHp()- damage);
		
		log.info("Combat: "+attacker.getId()+" -> "+defender.getId()+"(d:"+totalDefense+") = "+(hit ? damage : "MISS")+", now at "+defender.getHp()+" hp", null); 
		
		return new CombatResult(hit, damage);
	}
	
	
	public static class CombatResult {
		private final boolean hit;
		private final int damageDealt;
		
		public CombatResult(boolean hit, int damageDealt) {
			this.hit = hit;
			this.damageDealt = damageDealt;
		}
		
		public int getDamageDealt() {
			return damageDealt;
		}
		
		public boolean isHit() {
			return hit;
		}
		
	}
}
