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

package com.roguecloud.server;

import javax.websocket.Session;

import com.roguecloud.RCSharedConstants;
import com.roguecloud.utils.IManagedResource;

/**
 * Utilities for managing Websocket client resources. The managed resource methods are used by ResourceLifecycleUtil, see that class
 * for details.
 */
public class ServerWsClientUtil {
	
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
