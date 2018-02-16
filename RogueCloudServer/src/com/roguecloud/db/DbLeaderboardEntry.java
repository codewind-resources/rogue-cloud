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

package com.roguecloud.db;

public class DbLeaderboardEntry implements IDBObject {

	public static final int NEW_LEADERBOARD_ID = -1;

	long userId;
	long roundId;
	long score;


	public DbLeaderboardEntry() {
	}
	
	public DbLeaderboardEntry(long userId, long score, long roundId) {
		this.userId = userId;
		this.score = score;
		this.roundId = roundId;
	}

	public long getUserId() {
		return userId;
	}

	public long getScore() {
		return score;
	}

	public long getRoundId() {
		return roundId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public void setScore(long score) {
		this.score = score;
	}

	public void setRoundId(long roundId) {
		this.roundId = roundId;
	}
	
	@Override
	public String toString() {
		return userId+" "+this.score+" "+this.roundId;
	}

	public DbLeaderboardEntry fullClone() {
		return new DbLeaderboardEntry(this.userId, this.score, this.roundId);
	}
	
	public static boolean isValid(DbLeaderboardEntry e) {
		if(e.getUserId() < 0) {
			return false;
		}
		
		if(e.getRoundId() < 0) {
			return false;
		}
		
		if(e.getScore() < 0) {
			return false;
		}
		
		return true;
	}
}
