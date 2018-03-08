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

import com.roguecloud.server.ActiveWSClientList;

public class RoundScope {

	private final Object lock = new Object();
	
	private final ActiveWSClientList activeClients;
	
	private final long roundId;
	
	private final long nextRoundId;
	
	/** This is only set if isRoundComplete  is FALSE*/
	private Long nextRoundStartInNanos_synch_lock = null;
	
	private Long currentRoundEndInNanos_synch_lock = null;
	
	private final long roundLengthInNanos;
	
	private boolean isRoundComplete_synch_lock = false;
	
	private boolean isDisposed_synch_lock = false;
	
	public RoundScope(long roundId, long roundLengthInNanos, long nextRoundId, ServerInstance parent) {
		this.roundId = roundId;
		this.nextRoundId = nextRoundId;
		
		this.roundLengthInNanos = roundLengthInNanos;
		
		this.activeClients = new ActiveWSClientList(parent);
	}

	public long getRoundId() {
		return roundId;
	}
	
	public ActiveWSClientList getActiveClients() {
		return activeClients;
	}
	
	public boolean isRoundComplete() {
		synchronized(lock) {
			return isRoundComplete_synch_lock;
		}
	}
	
	public boolean isRoundCompleteUnsynchronized() {
		return isRoundComplete_synch_lock;
	}
	
	public void setRoundComplete(boolean isRoundComplete) {
		synchronized(lock) {
			this.isRoundComplete_synch_lock = isRoundComplete;
		}
	}
	
	public long getNextRoundId() {
		return nextRoundId;
	}
	
	public void beginAbsoluteRoundEndTimeInNanos() {
		synchronized (lock) {
			currentRoundEndInNanos_synch_lock = System.nanoTime() + roundLengthInNanos;
		}
	}
	
	public Long getCurrentRoundEndInNanos() {
		synchronized(lock) {
			return currentRoundEndInNanos_synch_lock;
		}
	}
	
	public void setNextRoundStartInNanos(Long nextRoundStartInNanos) {
		synchronized(lock) {
			this.nextRoundStartInNanos_synch_lock = nextRoundStartInNanos;
		}
	}
	
	public Long getNextRoundStartInNanos() {
		synchronized(lock) {
			return nextRoundStartInNanos_synch_lock;
		}
	}
	
	public void dispose() {
		synchronized(lock) {
			if(!isDisposed_synch_lock) {
				isDisposed_synch_lock = true;
			} else {
				return;
			}
		}
		
		new Thread() {
			public void run() {
				activeClients.dispose();
			}
		}.start();
	}
	
}