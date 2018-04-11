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
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import javax.websocket.Session;

import com.roguecloud.RCSharedConstants;
import com.roguecloud.utils.RCUtils;
import com.roguecloud.utils.IManagedResource;
import com.roguecloud.utils.Logger;

/** 
 * Utilities for managing Websocket client resources. The managed resource methods 
 * are used by ResourceLifecycleUtil.
 *   
 * For internal use only.
 **/
public class ClientUtil {
	
	public static final Logger log = Logger.getInstance();

	private static final Object lock = new Object(); 
	
	public static String getOrCreateClientUuid() {
		synchronized(lock) {
			File dir = new File(System.getProperty("user.home"), ".roguecloud");
			
			if(!dir.exists()) { dir.mkdirs(); }
			if(!dir.exists()) { log.severe("Unable to create settings directory: "+dir, null); return UUID.randomUUID().toString(); } 

			String uuid = null;
			
			File info = new File(dir, "uuid.txt");
			if(info.exists()) {
				
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(info);
					String str = RCUtils.readIntoString(fis);
					
					if(str != null && str.trim().length() > 0) {
						uuid = str.trim();
					}
					
				} catch (IOException e) {
					throw new RuntimeException("Unable to read file: "+info.getPath(), e);
				} finally {
					RCUtils.safeClose(fis);
				}				
				
			} 
			
			if(uuid != null) {
				return uuid;
			}
			
			uuid = UUID.randomUUID().toString();
			
			FileWriter fw = null;
			try {
				fw  = new FileWriter(info);
				fw.write(uuid);
			} catch (IOException e) {
				throw new RuntimeException("Unable to writed file: "+info.getPath(), e);
			} finally {
				RCUtils.safeClose(fw);
			}
			
			return uuid;
			
			
		} // end lock
	}
	
	public static IManagedResource convertSessionToManagedResource(Session s) {
		return new SessionManagedResource(s, RCSharedConstants.MAX_ROUND_LENGTH_IN_NANOS);
	}

	public static IManagedResource convertSessionToManagedResource(Session s, long expireDurationInNanos) {
		return new SessionManagedResource(s, expireDurationInNanos);
	}
	
	/** Implements a IManagedResource for a WebSocket Session object. */
	private static class SessionManagedResource implements IManagedResource {
		
		private final Session s;
		private final long expireTimeInNanos;
		
		private final Object lock = new Object();
		
		private boolean disposed_synch_lock = false;
		
		
		public SessionManagedResource(Session s, long expireDurationInNanos) {
			this.s = s;
			expireTimeInNanos = System.nanoTime() + expireDurationInNanos;
		}
		
		@Override
		public boolean isExpired() {
			return System.nanoTime() > expireTimeInNanos;
		}
		
		@Override
		public boolean isDisposed() {
			synchronized (lock) {
				return disposed_synch_lock;
			}
			
		}
		
		@Override
		public String getId() {
			return s.getId();
		}
		
		@Override
		public void dispose() {
			synchronized (lock) {
				if(disposed_synch_lock) { return; }
				disposed_synch_lock = true;
			}

			try {
			
				s.close();
				
			} catch(Exception e)  { /* ignore */ }
		}
		
		@Override
		public boolean allowReplace(IManagedResource resource) {
			return true;
		}
		
		@Override
		public int hashCode() {
			return s.getId().hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof SessionManagedResource)) {
				return false;
			}
			SessionManagedResource smr = (SessionManagedResource)obj;
			
			return smr.s.getId().equals(s.getId());
		}
	}

	
}
