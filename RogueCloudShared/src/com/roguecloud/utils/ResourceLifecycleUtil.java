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

package com.roguecloud.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * See IManagedResource for a description of managed resources.
 * 
 * This class will automatically dispose of managed resources once they expire, based on 
 * the implementation of the IManageResource class. 
 * 
 * This class may handle multiple different types of IManageResource classes at a time, though
 * the dispose() of an IManagedResource is called on the main ResourceLifeCycleThread thread, such that a long
 * dispose() method implementation of a resource can block other dispose methods of other resources from being called. 
 * 
 * This class is currently used to expire websockets that are open for too long.
 * 
 * This class is an internal class, for server use only.
 *
 */
public class ResourceLifecycleUtil {
	/** For internal use only */

	private static final Logger log = Logger.getInstance();

	private static final ResourceLifecycleUtil instance = new ResourceLifecycleUtil(); 
	
	private ResourceLifecycleUtil() {
		thread = new ResourceLifeCycleThread();
		thread.start();
	}
	
	public static ResourceLifecycleUtil getInstance() {
		return instance;
	}
	
	// ----------------------------

	private final ResourceLifeCycleThread thread;
	
	private final HashMap<String /* id*/, WrapperEntry> idToResourceMap_synch = new HashMap<>();
	
	public void addNewSession(IManagedResource wrapper) {
		if(wrapper == null) { return; }
		
		synchronized (idToResourceMap_synch) {

			WrapperEntry previousValue = idToResourceMap_synch.get(wrapper.getId());
			if(previousValue != null) {
				IManagedResource mr = previousValue.wrapper;
				if(!mr.allowReplace(wrapper)) {
					return;
				}
			}
		
			WrapperEntry we = new WrapperEntry();
			we.wrapper = wrapper;

			idToResourceMap_synch.put(wrapper.getId(), we);
		}
	
	}
	
	public void removeSession(IManagedResource wrapper) {
		synchronized (idToResourceMap_synch) {

			WrapperEntry previousValue = idToResourceMap_synch.get(wrapper.getId());
			if(previousValue != null && previousValue.wrapper.equals(wrapper)) {
				idToResourceMap_synch.remove(wrapper.getId());
			}
			
		}
	
	}

	/** Every X seconds, check if any of the resources are disposed (in which case remove them from the list) 
	 * or are expired (in which case call dispose and remove them from the list) */
	private class ResourceLifeCycleThread extends Thread {
		
		public ResourceLifeCycleThread() {
			setDaemon(true);
			setName(ResourceLifecycleUtil.class.getName());
		}
		
		@Override
		public void run() {
			while(true) {

				try { Thread.sleep(10 * 1000); } catch (InterruptedException e) { /* ignore */ }

				try {
					List<WrapperEntry> toRemoveList = new ArrayList<>();
					
					synchronized (idToResourceMap_synch) {
						toRemoveList.addAll(idToResourceMap_synch.values());
					}
					
					for(Iterator<WrapperEntry> it = toRemoveList.iterator(); it.hasNext(); ) {
						WrapperEntry we = it.next();
						
						// Remove items from the list if they are not disposed, and not expired.
						if(!we.wrapper.isExpired() && !we.wrapper.isDisposed()) {
							it.remove();
						}
					}
					
					// Dispose of the wrappers if not already disposed
					toRemoveList.forEach( e -> {
						try {
							if(!e.wrapper.isDisposed()) {
								log.info("Disposing of wrapper "+e.wrapper, null);
								e.wrapper.dispose();
							}
						} catch(Throwable t) { /* ignore */ }
					});
					
					// Remove all the expired/disposed entries
					synchronized (idToResourceMap_synch) {
						toRemoveList.forEach( e -> {
							idToResourceMap_synch.remove(e.wrapper.getId());
						});
					}
					
				} catch(Exception e) {
					log.info("Exception in lifecycle thread:", e, null);
				}
				
			}
		}
		
	}
	
	
	/** An actively managed resource inside the resource lifecycle map. */
	private static class WrapperEntry {
		
		IManagedResource wrapper;
	}
}
