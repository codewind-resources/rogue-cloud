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

package com.roguecloud.actions;

/** 
 * The IAction interface, and the *Action classes (CombatAction/DrinkItemAction/etc) correspond to actions that your character 
 * performs in the world.
 * 
 * To perform an action, create the appropriate action object, and call sendAction(..) from the client class:
 * ```
 *   // where somePlayer is a nearby player from the worldState object.
 *   CombatAction ca = new CombatAction (somePlayer); 
 *   sendAction(ca);
 * ```
 **/
public interface IAction {

	/** To determine which type/class the action is, call getActionType() on an IAction. */
	public static enum ActionType { STEP, COMBAT, MOVE_INVENTORY_ITEM, EQUIP,  DRINK, NULL };
	
	public ActionType getActionType();
	
}
