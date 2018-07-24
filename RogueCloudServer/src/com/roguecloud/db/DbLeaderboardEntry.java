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

import java.util.Date;

/** A leaderboard entry for a player that played in a given round. This object is serialized to json. */
public class DbLeaderboardEntry implements IDBObject {

	public static final int NEW_LEADERBOARD_ID = -1;

	long userId;
	long roundId;
	long score;
	long dateTime;


	public DbLeaderboardEntry() {
	}
	
	public DbLeaderboardEntry(long userId, long score, long roundId, long dateTime) {
		this.userId = userId;
		this.score = score;
		this.roundId = roundId;
		this.dateTime = dateTime;
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
	
	public long getDateTime() {
		return dateTime;
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
		
	public void setDateTime(long dateTime) {
		this.dateTime = dateTime;
	}
	
	
	public void copyFromParam(DbLeaderboardEntry dle) {
		setRoundId(dle.getRoundId());
		setScore(dle.getScore());
		setUserId(dle.getUserId());
		setDateTime(dle.getDateTime());
	}
	
	@Override
	public String toString() {
		return userId+" "+this.score+" "+this.roundId+" "+(dateTime > 0 ? (new Date(dateTime)) : "--");
	}

	public DbLeaderboardEntry fullClone() {
		return new DbLeaderboardEntry(this.userId, this.score, this.roundId, this.dateTime);
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
