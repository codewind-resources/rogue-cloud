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

/** 
 * This class is used by ResourceLifecycleUtil. 
 * 
 * A managed resource is any abstract thing that:
 * - can expire
 * - can be disposed of
 * - when it expires the it should be disposes of.
 * - has a unique ID
 * - if a resource with the same ID is added to ResourceLifeCycleUtil, whether it should replace this resource (if not, an error is thrown)  
 *
 * This is currently used to automatically close websockets that are open for too long.
 * 
 * This class is an internal class, for server use only. 
 *  
 **/
public interface IManagedResource {

	void dispose();

	boolean isDisposed();

	boolean isExpired();

	public boolean allowReplace(IManagedResource resource);
	
	public String getId();
}
