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

package com.roguecloud.db.cloudant;

import java.util.ArrayList;
import java.util.List;

import com.roguecloud.db.DbLeaderboardEntry;
import com.roguecloud.db.DbUser;
import com.roguecloud.utils.ServerUtil;

/** Simple standalone class for developing the Cloudant db outside of Rogue Cloud server. */
public class CloudAntStandaloneMain {

	public static void main(String[] args) {

//		createUsers();
		inner();
		
	}
	
	private static void inner() {
		
		CloudantDbBackend cdbb = new CloudantDbBackend();
		
		List<DbLeaderboardEntry> ldbs = new ArrayList<>();
		
		long dateTime = System.currentTimeMillis();
		
		ldbs.add(new DbLeaderboardEntry(1, 10, 1, dateTime));
		ldbs.add(new DbLeaderboardEntry(2, 10, 1, dateTime));
		ldbs.add(new DbLeaderboardEntry(3, 10, 1, dateTime));
		ldbs.add(new DbLeaderboardEntry(4, 10, 1, dateTime));
		ldbs.add(new DbLeaderboardEntry(5, 10, 1, dateTime));
		
		ldbs.add(new DbLeaderboardEntry(1, 10, 2, dateTime));
		ldbs.add(new DbLeaderboardEntry(2, 10, 2, dateTime));
		ldbs.add(new DbLeaderboardEntry(3, 10, 2, dateTime));
		ldbs.add(new DbLeaderboardEntry(4, 10, 2, dateTime));
		ldbs.add(new DbLeaderboardEntry(5, 10, 2, dateTime));

		cdbb.writeNewOrExistingLeaderboardEntries(ldbs);
		
		System.out.println("Ldbs:");
		cdbb.getAllLeaderboardEntries().forEach( e -> {
			
			System.out.println(e);
		});
		
	}
	
	@SuppressWarnings("unused")
	private static void createUsers() {
		CloudantDbBackend cdbb = new CloudantDbBackend();
		
		List<DbUser> users = new ArrayList<>();
		String password = ServerUtil.SHA_256_FIELD+ServerUtil.oneWayFunction("newpassword");
		users.add(new DbUser(1, "jgw", password));
		users.add(new DbUser(2, "jgw2", password));
		users.add(new DbUser(3, "jgw3", password));
		users.add(new DbUser(4, "jgw4", password));
		users.add(new DbUser(5, "jgw5", password));
		users.add(new DbUser(6, "jgw6", password));

		cdbb.writeNewOrExistingUsers(users);
		
		System.out.println("users:");
		cdbb.getAllUsers().stream().forEach( e -> {
			System.out.println(e);
		});
				
	}
}
