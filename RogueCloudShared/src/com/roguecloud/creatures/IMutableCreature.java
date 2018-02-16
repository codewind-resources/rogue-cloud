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

import com.roguecloud.Position;
import com.roguecloud.items.Effect;
import com.roguecloud.items.OwnableObject;
import com.roguecloud.items.Weapon;

/** Server-side interface for internal use only, see ICreature for the public client API */
public interface IMutableCreature extends ICreature {

	public void setPosition(Position p);

	public void setMaxHp(int maxHp);
	
	public void setCurrHp(int currHp);
	
	public void setWeapon(Weapon weapon);

	public void addToInventory(OwnableObject o);
	public boolean removeFromInventory(OwnableObject o);
	
	public IMutableCreature fullClone();
	
	public void addEffect(Effect e);
	public boolean removeEffect(Effect e);
	
}
