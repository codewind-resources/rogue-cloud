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

import com.roguecloud.json.JsonDoorProperty;

/** For internal use */
public class DoorTileProperty implements ITilePropertyMutable {

	
	public DoorTileProperty() {
	}
	
	public DoorTileProperty(JsonDoorProperty jdp ) {
			this.isOpen_synch_lock = jdp.isOpen();
	}

	public DoorTileProperty(boolean isOpen) {
		this.isOpen_synch_lock = isOpen;
	}

	
	private final Object lock = new Object();
	
	private boolean isOpen_synch_lock;
	
	
	public boolean isOpen() {
		synchronized(lock) {
			return isOpen_synch_lock;
		}
	}
	
	public void setOpen(boolean isOpen) {
		synchronized(lock) {
			this.isOpen_synch_lock = isOpen;
		}
	}

	public DoorTileProperty fullClone() {
		DoorTileProperty dtp = new DoorTileProperty();
		dtp.isOpen_synch_lock = isOpen();
		return dtp;
	}
	
	@Override
	public TilePropertyType getType() {
		return TilePropertyType.DOOR;
	}

}
