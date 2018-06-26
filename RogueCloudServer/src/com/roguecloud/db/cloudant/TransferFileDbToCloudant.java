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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.roguecloud.db.DbLeaderboardEntry;
import com.roguecloud.db.DbUser;
import com.roguecloud.db.file.FileDbBackend;

/** Three functions:
 * 
 * - Transfer the contents of a FileDbBackend to CloudantDbBackend
 * - Transfer the contents of a CloudantDbBackend to FileDbBackend
 * - Compare two local FileDbBackend directories, to confirm they are equal 
 **/
public class TransferFileDbToCloudant {

	public static void main(String[] args) {

//		transferFileDbToCloudant();
		
//		transferCloudantToFileDb();
		
//		compareTwoFileDbs();
	}
	
	private static void compareTwoFileDbs() {
		FileDbBackend fdb1 = new FileDbBackend(new File("C:\\roguecloud-import"));
		FileDbBackend fdb2 = new FileDbBackend(new File("C:\\delme\\.roguecloud"));
		
		{
			List<DbUser> users1 = fdb1.getAllUsers();
			List<DbUser> users2 = fdb2.getAllUsers();
			
			System.out.println("Users start sizes: " + users1.size()+" "+users2.size());
			
			for(Iterator<DbUser> it1 = users1.iterator(); it1.hasNext();) {
				DbUser dbu1 = it1.next();
				it1.remove();
				
				boolean found = false;
						
				for(Iterator<DbUser> it2 = users2.iterator(); it2.hasNext();) {
					DbUser dbu2 = it2.next();
					
					if(dbu1.toString().equals(dbu2.toString())) {
						it2.remove();
						found = true;
						break;
					}
				}
				
				if(!found) { System.err.println("not found: "+dbu1.toString()); }
			}
			
			System.out.println("Users new size: "+users1.size()+" "+users2.size());
		}
		
		{
			List<DbLeaderboardEntry> dles1 = fdb1.getAllLeaderboardEntries();
			List<DbLeaderboardEntry> dles2 = fdb2.getAllLeaderboardEntries();
			
			System.out.println("DBL start sizes: " + dles1.size()+" "+dles2.size());
			
			for(Iterator<DbLeaderboardEntry> it = dles1.iterator(); it.hasNext();) {
				DbLeaderboardEntry dle1 = it.next();
				
				if(dles1.size() % 1000 == 0) {
					System.out.println(dles1.size());
				}
				
				it.remove();
				
				boolean found = false;
				
				for(Iterator<DbLeaderboardEntry> it2 = dles2.iterator(); it2.hasNext(); ) {
					DbLeaderboardEntry dle2 = it2.next();
					
					if(dle2.toString().equals(dle1.toString())) {
						it2.remove();
						found = true;
						break;
					}
					
				}
				
				if(!found) { System.err.println("not found: "+dle1.toString()); }
			}

			System.out.println("DBL "+dles1.size()+" "+dles2.size());
		}
		
	}
	
	
	@SuppressWarnings("unused")
	private static void transferFileDbToCloudant() {
		FileDbBackend fdb = new FileDbBackend(new File("C:\\roguecloud-import"));
	
		System.setProperty(CloudantDbBackend.CONIG_USERNAME, "");
		System.setProperty(CloudantDbBackend.CONFIG_PASSWORD, "");
		System.setProperty(CloudantDbBackend.CONFIG_DB_NAME, "");
		System.setProperty(CloudantDbBackend.CONFIG_URL, "");

		CloudantDbBackend cdb = new CloudantDbBackend();
		
		cdb.internalSetNextRoundId(fdb.internalGetNextRoundId());
		
		cdb.writeNewOrExistingUsers(fdb.getAllUsers());
		cdb.writeNewOrExistingLeaderboardEntries(fdb.getAllLeaderboardEntries());
	}

	
	@SuppressWarnings("unused")
	private static void transferCloudantToFileDb() {
		
		File dest = new File("C:\\roguecloud-import");
		dest.mkdirs();
		
		FileDbBackend fdb = new FileDbBackend(dest);
		
		System.setProperty(CloudantDbBackend.CONIG_USERNAME, "");
		System.setProperty(CloudantDbBackend.CONFIG_PASSWORD, "");
		System.setProperty(CloudantDbBackend.CONFIG_DB_NAME, "");
		System.setProperty(CloudantDbBackend.CONFIG_URL, "");

		CloudantDbBackend cdb = new CloudantDbBackend();

		{
			List<DbUser> newUsers = new ArrayList<>();
			List<DbUser> users = cdb.getAllUsers();
			for(DbUser user : users) {
				DbUser newUser = new DbUser();
				newUser.copyFromParam(user);
				newUsers.add(newUser);
			}
			fdb.writeNewOrExistingUsers(newUsers);
		}
		

		{
			List<DbLeaderboardEntry> newDles = new ArrayList<>();
			List<DbLeaderboardEntry> dles = cdb.getAllLeaderboardEntries();
			for(DbLeaderboardEntry dle : dles) {
				DbLeaderboardEntry newDle = new DbLeaderboardEntry();
				newDle.copyFromParam(dle);
				newDles.add(newDle);
			}
			fdb.writeNewOrExistingLeaderboardEntries(newDles);
		}
		
		
		fdb.internalSetNextRoundId(cdb.internalGetNextRoundId());
		
	}

}

