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
import com.roguecloud.map.TileType;

/** A simple interface that exists on various entity classes in the universe. */
public interface IExistsInWorld {

	/** Return the position of the entity in the world */
	public Position getPosition();
	
	/** Return the tile sprite data values. */
	public TileType getTileType();
}
