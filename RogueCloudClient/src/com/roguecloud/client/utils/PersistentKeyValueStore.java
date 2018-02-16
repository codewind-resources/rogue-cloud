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

package com.roguecloud.client.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.roguecloud.client.IKeyValueStore;
import com.roguecloud.utils.RCUtils;

/** For internal use only */
public class PersistentKeyValueStore implements IKeyValueStore {

	private final File dir;
	
	public PersistentKeyValueStore(File dir) {
		this.dir = dir;
		if(!dir.exists()) {
			this.dir.mkdirs();
		}
		
		if(!dir.exists()) {
			throw new RuntimeException("Unable to write to create directory: "+dir.getPath());
		}
	}
	
	@Override
	public void writeString(String key, String value) {
		writeObject(key, value);
	}
	
	@Override
	public void writeObject(String key, Object value) {
		
		ObjectOutputStream oos = null;
		try {
			try {
				oos  = new ObjectOutputStream(new FileOutputStream(new File(dir, "key-"+key)));
				oos.writeObject(value);
			} catch (IOException e) {
				throw new RuntimeException("Unable to write to file", e);
			}
		} finally {
			RCUtils.safeClose(oos);
		}
	}
	
	@Override
	public String readString(String key) {
		return (String) readObject(key);
	}
	
	@Override
	public Object readObject(String key)  {
		
		ObjectInputStream ois = null;
		try {
			File f = new File(dir, "key-"+key);
			if(!f.exists()) { return null; }
			
			ois = new ObjectInputStream(new FileInputStream(f));
			
			try {
				return ois.readObject();
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			
		} catch(IOException e) {
			throw new RuntimeException("Unable to read from file", e);
		} finally {
			RCUtils.safeClose(ois);
		}
	}
	
	
}
