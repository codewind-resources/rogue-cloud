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

import com.roguecloud.Position;
import com.roguecloud.map.IMap;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;

/** An object that exists on the ground in the world. See IGroundObject for public interface.  */
public class GroundObject implements IGroundObject {

	private final Object lock = new Object();
	
	private Position position_synch_lock;
	
	private final IObject o;
	
	private final long id;
	
	public GroundObject(long id, IObject o, Position p) {
		if(o == null || p == null) { throw new IllegalArgumentException(); };
		
		this.id = id;
		this.position_synch_lock = p;
		this.o = o;
	}
	
	@Override
	public Position getPosition() {
		synchronized(lock) {
			return position_synch_lock;
		}
	}

	@Override
	public IObject get() {
		return o;
	}

	@Override
	public TileType getTileType() {
		return o.getTileType();
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return "ground-object-id: "+id+" pos: "+position_synch_lock+" obj: "+o;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof GroundObject)) {
			return false;
		}
		
		GroundObject other = (GroundObject)obj;
		if(other.getId() != this.id)  { return false; }
		
		if(!other.getPosition().equals(this.getPosition())) { return false; }
		
//		if(!other.o.equals(this.o)) { return false; }
		
		return true;
		
	}
	
	@Override
	public long getLastTickUpdated(IMap map) {
		Position p = getPosition();
		if(p == null) { return Long.MAX_VALUE; }
		
		Tile t = map.getTile(p);
		if(t == null) {
			return Long.MAX_VALUE;
		} else {
			return t.getLastTickUpdated();
		}
	}
	
	// Internal methods ---------------------------------------
	
	@Override
	public int hashCode() {
		return (int)(getId() + getPosition().hashCode());
	}

	public GroundObject shallowClone() {
		// does not clone the contained object, but that SHOULD be immutable
		return new GroundObject(id, o, getPosition());
	}
	
	public void setPosition(Position p) {
		synchronized(lock) {
			this.position_synch_lock = p;
		}
	}

}
