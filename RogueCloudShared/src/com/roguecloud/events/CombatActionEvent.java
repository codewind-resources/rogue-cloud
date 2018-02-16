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

package com.roguecloud.events;

import com.roguecloud.Position;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.events.JsonCombatActionEvent;

/** An attack between two characters that occurred in the world. 
 * 
 * Note that not all attacks are successful: an attack will sometimes miss, or may succeed but but deal minimal or zero damage.   
 **/
public class CombatActionEvent extends AbstractEvent {

	private final ICreature attacker;
	private final ICreature defender;

	private final boolean hit;
	private final long damageDone;

	@Override
	public EventType getActionType() {
		return EventType.COMBAT;
	}

	public CombatActionEvent(ICreature attacker, ICreature defender, boolean hit, long damage, long frame, long id) {
		super(frame, id);
		if (attacker == null || defender == null) {
			throw new IllegalArgumentException();
		}

		this.attacker = attacker;
		this.defender = defender;
		this.hit = hit;
		this.damageDone = damage;

	}

	/** The coordinates on the map at which that the event occurred. */
	@Override
	public Position getWorldLocation() {
		return defender.getPosition();
	}

	/** The attacking creature */
	public ICreature getAttacker() {
		return attacker;
	}

	/** The defending creature. */
	public ICreature getDefender() {
		return defender;
	}

	/** Return false if the attack missed (and thus no damage was done), or true otherwise. */
	public boolean isHit() {
		return hit;
	}

	/** How much damage was done by the attack, if any. */
	public long getDamageDone() {
		return damageDone;
	}
	
	@Override
	public String toString() {
		return "CombatActionEvent: attacker: "+attacker.getId()+" defender: "+defender.getId()+" hit: "+hit+"  damageDone: "+damageDone;
	}
	
	/** Returns true if the specified creature was involved in this event (was the attacker or defender), or false otherwise. */
	@Override
	public boolean isCreatureInvolved(ICreature creature) {
		if(creature == null ) { throw new IllegalArgumentException(); }
		
		if(creature.equals(attacker)) { return true; }
		if(creature.equals(defender)) { return true; }
		
		return false;
	}

	/** Return a list of all the creatures involved in the event. */
	@Override
	public ICreature[] getCreaturesInvolved() {
		return new ICreature[] { attacker, defender };
	}
	
	// Internal methods ---------------------------------------
	
	public String userVisibleCombatResult() {
		if(!hit) {
			return "Miss";
		} else {
			return "Hit for "+damageDone+" hp";
		}
	}

	@Override
	public JsonAbstractTypedMessage toJson() {
		JsonCombatActionEvent result = new JsonCombatActionEvent();
		result.setAttackerId(attacker.getId());
		result.setDamageDone(damageDone);
		result.setDefenderId(defender.getId());
		result.setFrame(frame);
		result.setHit(hit);
		result.setId(id);

		return result;
	}

}
