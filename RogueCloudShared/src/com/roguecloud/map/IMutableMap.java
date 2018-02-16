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

/** 
 * The mutable interface for a map implementation.
 * 
 * For internal use - see IMap for the public API of map.
 **/
public interface IMutableMap extends IMap {
	
	public IMap cloneForRead();

	public Tile getTileForWrite(int x, int y);

	public Tile getTileForWrite(Position p);
	
	public Tile getTileForWriteUnchecked(Position p);

	public void putTile(int x, int y, Tile t);

	public void putTile(Position p, Tile t);
}
