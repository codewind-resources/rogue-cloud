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

package com.roguecloud.client;

/** 
 * Provides a simple key-value store that may be used to store Strings or Java objects for use by future runs of your code.
 * 
 * This will help you store/retrieve/share data between program restarts or interrupts.
 * 
 * This is useful because:
 * - When your character dies, their code is restarted and all previous program state is lost. 
 * - This class can then be used to store objects across code restarts, so as to preserve that program state.
 * - This is similar to how a microservice must store data in an external database, which then must be reacquired on microservice restart.
 *    
 **/
public interface IKeyValueStore {
	
	public void writeString(String key, String value);
	
	public void writeObject(String key, Object value);
	
	public String readString(String key);
	
	public Object readObject(String key);

}
