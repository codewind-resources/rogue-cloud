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

package com.roguecloud.events;

import com.roguecloud.json.JsonAbstractTypedMessage;

/**
 * Interface providing additional event methods that are used to pass events between the client and server. 
 *  
 * Server-side interface, for internal use only! For the public client API, see IEvent. 
 **/
public interface IMutableEvent extends IEvent {

	public JsonAbstractTypedMessage toJson();

	public long getEventId();
}
