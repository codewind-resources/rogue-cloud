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

import java.util.List;

/** 
 * This interface should be implemented by a database frontend to provide storage/retrieval of leaderboard data. The
 * methods of this interface correspond to CRUD requests on entities in the database, but the methods not do specifically
 * dictate the technology (NoSQL/SQL/File system) used to satisfy these database requests.   
 * 
 * Currently, only MemoryDatabase implements this interface, but MemoryDatabase supports 
 * a number of backends, including Cloudant and the local file system storage.
 *
 */
public interface IDatabase {
	
	public long getAndIncrementNextRoundId();
	
	// Users
	
	public boolean isValidPasswordForUser(String username, String password);
	
	/** The password field of DBUser must be in Base64 SHA-256 form. See implementors for details. */
	public long createUser(DbUser u);
	
	public DbUser getUserByUsername(String name);
	
	public DbUser getUserById(long id);
	
	public Long[/*score, rank*/] getUserBestScoreAndRank(long id);	
	
	// Leaderboard Entries
	
	public void createOrUpdateDbLeaderboardEntry(DbLeaderboardEntry le);
	
	public List<DbLeaderboardEntry> getLeaderboardEntriesForARound(long roundId);
	
	public List<DbLeaderboardEntry> getBestOverallLeaderboardEntries();
	
	public List<DbLeaderboardEntry> getBestPreviousXRoundsOfLeaderboardEntries(long numPreviousRounds);
	
	public List<DbLeaderboardEntry> getAllLeaderboardEntriesForUser(long id);
	
	public List<DbLeaderboardEntry> getAllLeaderboardEntriesForUserAndRound(long id, long roundId);


}
