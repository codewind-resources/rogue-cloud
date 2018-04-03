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

package com.roguecloud.db.file;

import java.util.List;

import com.roguecloud.db.DbLeaderboardEntry;
import com.roguecloud.db.DbUser;

/** 
 * MemoryDatabase is an in-memory frontend API for persistence, but MemoryDatabase requires a backend from which to retrieve
 * and store data. Classes that implement this IDBBackend interface may be used as a database backend for the in-memory database.
 * 
 * The design of MemoryDatabase and IDBBackend are such that the full database contents is retrieved on first use, and from there
 * individual or bulk writes may be performed.
 * 
 * Current backends are FileDbBackend (local storage) and CloudantDbBakcend (storage to Cloudant on IBM Cloud).
 **/
public interface IDBBackend {

	List<DbUser> getAllUsers();

	List<DbLeaderboardEntry> getAllLeaderboardEntries();

	void writeNewOrExistingUsers(List<DbUser> users);

	void writeNewOrExistingLeaderboardEntries(List<DbLeaderboardEntry> dbe);

	long getAndIncrementNextRoundId();

}
