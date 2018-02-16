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

package com.roguecloud;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.roguecloud.actions.IActionResponse;


/**  
 * When you send an action to the server, an instance of this class is immediately returned to you, that is, sending an action does not synchronously
 * block the calling method. 
 *  
 * When the action is received and completed by the server, the server will then send back an IActionResponse to us, and that response will 
 * be available to be read from this class.
 *  
 * However, until the server sends back a response:
 * 
 * - isResponseReceived() will return false
 * - getOrReturnNullIfNoResponse() will return null
 * - getOrWaitForResponse() will block (forever) until a response is received
 * - getOrWaitForResponse(long, TimeUnit) will block for a given time period until a response is received.
 *  
 * Finally, once the server has sent back a response to your action:
 * 
 * - isResponseReceive() will return true;
 * - getOrReturnNullIfNoResponse() will return a non-null IActionResponse
 * - getOrWaitForResponse(...) will return a non-null IActionResponse
 * 
 * This class works similarly to Java's java.util.concurrent.Future class.
 *  
 **/
public class ActionResponseFuture  {

	private final Object lock = new Object();
	
	private IActionResponse response_synch_lock = null; 
	
	private long messageId;
	
	public ActionResponseFuture() {
	}
	
	/** Has the server processed and responded to our action? */
	public boolean isResponseReceived() {
		synchronized (lock) {
			return response_synch_lock != null;
		}
	}

	/** Returns an IActionResponse if one has been received from the server, or null otherwise. */
	public IActionResponse getOrReturnNullIfNoResponse() {
		synchronized (lock) {
			return response_synch_lock;
		}
	}

	/** Blocks until an IActionResponse is received from the server. If no response is received, this method
	 * will wait until either the end of the universe, or program termination, whichever occurs first. */
	public IActionResponse getOrWaitForResponse() throws InterruptedException, ExecutionException {
		synchronized (lock) {
			if(response_synch_lock != null) {
				return response_synch_lock;
			}
			
			while(true) {
				lock.wait();
				if(response_synch_lock != null) {
					return response_synch_lock;
				}
			}
			
		}
	}

	/** Blocks until an IActionResponse is received from the server, or until timeout (in which case null is returned). */
	public IActionResponse getOrWaitForResponse(long timeout, TimeUnit unit) {
		
		long expireTimeInNanos = System.nanoTime()+unit.toNanos(timeout);
		
		while(expireTimeInNanos < System.nanoTime()) {
			synchronized(lock) {
				try { lock.wait(1000); } catch (InterruptedException e) { return null; }
				if(response_synch_lock != null) {
					return response_synch_lock;
				}			
			}
		}
		
		return null;
	}
	
	
	// Methods for internal use only -------------------------------
	
	public void internalSetMessageId(long messageId) {
		this.messageId = messageId;
	}
	
	public long internalGetMessageId() {
		return messageId;
	}
	
	public void internalSetResponse(IActionResponse result) {
		synchronized(lock) {
			response_synch_lock = result;
			lock.notify();
		}
	}
	

}