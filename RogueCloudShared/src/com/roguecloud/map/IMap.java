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

package com.roguecloud.map;

import com.roguecloud.Position;

/** The agent's view of the game world. This map includes both tiles that the agent can currently see, as well 
 * as tiles they have previously seen (the agent's "memory").
 * 
 * Tiles that are no longer visible to the agent will not be updated with the latest creatures or items on that tile,
 * as the agent can no longer see the tile. However, the structures (walls and buildings) are unlikely to move. 
 *  
 **/
public interface IMap {
	
	/** Get the tile at the specified position, or null if not found. A tile will be null if it has not yet been seen. */
	public Tile getTile(Position p);
	
	/** Get the tile at the specified position, or null if not found. A tile will be null if it has not yet been seen. */
	public Tile getTile(int x, int y);
		
	/** Get the total width of the world map */
	public int getXSize();
	
	/** Get the total height of the world map */
	public int getYSize();
	
}
