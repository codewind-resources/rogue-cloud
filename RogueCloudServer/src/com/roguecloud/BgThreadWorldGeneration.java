/*
 * Copyright 2019 IBM Corporation
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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.roguecloud.WorldGenFromFile.WorldGenFromFileResult;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.RCUtils;
import com.roguecloud.utils.RoomList;
import com.roguecloud.utils.ServerUtil;
import com.roguecloud.utils.UniverseParserUtil;
import com.roguecloud.utils.WorldGenFileMappings;

/** World generation can take a fair bit of CPU time, so we run it as a lower-priority background thread 
 * after the round end, or while the server is loading the initial database on startup.
 * 
 * The caller can either:
 * - Call ensureThreadIsRunning() which kicks off the world generator if it has not already started
 * - Or, call getOrWaitForResult(), which will return a result if one is already available, or kick one off and wait for it (if one was not already kicked off).
 * 
 **/
public class BgThreadWorldGeneration {
	
	private final Logger log = Logger.getInstance();
	
	private final Object lock = new Object();
	
	private final RoomList roomListFromParent;
	
	private AcquisitionThread acqThread_synch_lock;
	
	private WorldGenFromFileResult wgResult_synch_lock;
	
	public BgThreadWorldGeneration(RoomList roomListFromParent) {
		this.roomListFromParent = roomListFromParent;
	}
	
	public void ensureThreadIsRunning() {
		synchronized (lock) {
			if(acqThread_synch_lock == null && wgResult_synch_lock == null) {
				acqThread_synch_lock = new AcquisitionThread();
				acqThread_synch_lock.start();
			}
		}
	}
	

	/** Only called by thread run(...) method.
	 * @throws InterruptedException */
	private void acquire() throws IOException, InterruptedException {

		long startTimeInNanos = System.nanoTime();
		
		WorldGenFileMappings mappings = new WorldGenFileMappings(ServerUtil.getServerResource(UniverseParserUtil.class, "/universe/map-new-mappings.txt"));
		
		WorldGenFromFileResult wgResult = WorldGenFromFile.generateMapFromInputStream(roomListFromParent, 
				ServerUtil.getServerResource(UniverseParserUtil.class, "/universe/map-new.txt"), mappings);

		System.out.println(BgThreadWorldGeneration.class.getSimpleName()+" generation took "+TimeUnit.MILLISECONDS.convert(System.nanoTime()-startTimeInNanos, TimeUnit.NANOSECONDS));

		synchronized(lock) {
			wgResult_synch_lock = wgResult;
		}
	}
	
	public WorldGenFromFileResult getOrWaitForResult() {
		
		while(true) {
			
			// Reset the expire time counter 
			long expireTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(30 * 1000, TimeUnit.MILLISECONDS); 
			
			inner_while: while(true) {
				
				// Blank and return the result if present, otherwise wait.
				synchronized (lock) {
					if(wgResult_synch_lock != null) {
						WorldGenFromFileResult result = wgResult_synch_lock;
						wgResult_synch_lock = null;
						return result;
					} else {
						
						// If the time has expired, interrupt the generation thread and start again.
						if(System.nanoTime() > expireTimeInNanos) {
							if(acqThread_synch_lock != null) {
								acqThread_synch_lock.interrupt();
							}
							break inner_while;
						}

						// This will start a new thread if one dies w/o returning a value.
						ensureThreadIsRunning();
						
					}
				}
				RCUtils.sleep(100);
			}
			
		}
		
	}

	public Optional<WorldGenFromFileResult> getOrWaitForResultOld(long expireTimeInMsecs) {
		
		long expireTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(expireTimeInMsecs, TimeUnit.MILLISECONDS);
		
		while(System.nanoTime() < expireTimeInNanos) {
			
			// Blank and return the result if present, otherwise wait.
			synchronized (lock) {
				if(wgResult_synch_lock != null) {
					WorldGenFromFileResult result = wgResult_synch_lock;
					wgResult_synch_lock = null;
					return Optional.of(result);
				} else {					
					// This will start a new thread if one dies w/o returning a value.
					ensureThreadIsRunning(); 
				}
			}
			RCUtils.sleep(100);

		}
		
		return Optional.empty();
	}
	
	private class AcquisitionThread extends Thread  {
		
		public AcquisitionThread() {
			setDaemon(true);
			setPriority(getPriority()-3);
		}
		
		@Override
		public void run() {
			try {
				acquire();
			} catch (IOException e) {
				log.severe("Exception during world generation.", e, null);
			} catch (InterruptedException e) {
				log.severe("Interrupted exception during world generation.", e, null);
			} finally {
				synchronized (lock) {
					acqThread_synch_lock = null;
				}
			}
		}
	}

}
