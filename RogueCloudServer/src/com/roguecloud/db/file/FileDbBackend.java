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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roguecloud.db.DbLeaderboardEntry;
import com.roguecloud.db.DbUser;
import com.roguecloud.utils.RCUtils;
import com.roguecloud.utils.Logger;

/** This database backend writes database entries to the local file system, as simple text files. */
public class FileDbBackend implements IDBBackend {
	
	private static final Logger log = Logger.getInstance();
	
	private final File dbRoot;
	
	/** Synchronized on me when reading and writing */
	private final Object lock = new Object();
	
	/** synchronize on lock when accessing, object itself is not thread safe. */
	private final SafeKeyValueStore keyValueStore_synch_lock;
	
	private static final String KEY_ROUND_ID = "KEY_ROUND_ID";
	
	public FileDbBackend(File dbRoot) {
		this.dbRoot = dbRoot;
		this.keyValueStore_synch_lock = new SafeKeyValueStore(new File(dbRoot, "key-store"));
	}
	
	public long internalGetNextRoundId() {
		synchronized(lock) {
			long nextId = keyValueStore_synch_lock.readId(KEY_ROUND_ID).orElse(1l);
			
			return nextId;
		}
		
	}
	
	public void internalSetNextRoundId(long nextRoundId) {
		synchronized(lock) {
			keyValueStore_synch_lock.writeId(KEY_ROUND_ID, nextRoundId);
		}
	}
	
	public long getAndIncrementNextRoundId() {
		
		synchronized(lock) {
			long nextId = keyValueStore_synch_lock.readId(KEY_ROUND_ID).orElse(1l);
			
			keyValueStore_synch_lock.writeId(KEY_ROUND_ID, nextId+1);
			
			return nextId;
			
		}
		
	}

	@Override
	public List<DbUser> getAllUsers() {
		ObjectMapper om = new ObjectMapper();
		
		List<DbUser> result = new ArrayList<DbUser>();
		
		synchronized(lock) {
			File users = new File(dbRoot, "users");
			users.mkdirs();
			
			for(File userFile : users.listFiles()) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(userFile);
					
					DbUser d = om.readValue(RCUtils.readIntoString(fis), DbUser.class);
					result.add(d);
										
				} catch(Exception t ) {
					log.err("Error occured on reading user from file: "+userFile.getPath(), t, null);
					t.printStackTrace();
				} finally {
					RCUtils.safeClose(fis);
				}
				
			}
		}
		
		return result;
	}
	
	@Override
	public List<DbLeaderboardEntry> getAllLeaderboardEntries() {
		ObjectMapper om = new ObjectMapper();
		
		List<DbLeaderboardEntry> result = new ArrayList<DbLeaderboardEntry>();
		
		synchronized(lock) {
			File rounds = new File(dbRoot, "rounds");
			rounds.mkdirs();
			
			for(File roundFile : rounds.listFiles()) {
				
				JsonDbLeaderboardEntries jdb = readLeaderboardFile(roundFile, om);
				if(jdb != null) {
					result.addAll(jdb.getEntries());
				}
			}
			
		}
		
		return result;
	}

	private static JsonDbLeaderboardEntries readLeaderboardFile(File roundFile, ObjectMapper om) {
		FileInputStream fis = null;
		try {
			
			fis = new FileInputStream(roundFile);
			return om.readValue(RCUtils.readIntoString(fis), JsonDbLeaderboardEntries.class);
			
		} catch(Exception t ) {
			log.severe("Exception on reading from file "+roundFile, t, null);
			t.printStackTrace();
		} finally {
			RCUtils.safeClose(fis);
		}
		
		return null;
	
	}
	
	@Override
	public void writeNewOrExistingUsers(List<DbUser> users) {
		ObjectMapper om = new ObjectMapper();
		
		synchronized(lock) {
			
			for(DbUser dbu : users) {
				
				String userValidationStr = DbUser.isValid(dbu, true);
				if( userValidationStr != null) {
					log.severe("Invalid db user provided to writeNewOrExistingUsers: "+dbu+" - "+userValidationStr, null);
					continue;
				}
				
				File path = new File(new File(dbRoot, "users"), "user-"+dbu.getUserId()+".txt");
				path.getParentFile().mkdirs();
		
				FileWriter fw = null;
				try {
					fw = new FileWriter(path);
					fw.write(om.writeValueAsString(dbu));
				} catch(Exception e) {
					log.severe("Exception on writing to file: "+path+" "+dbu, null);
					e.printStackTrace();
				} finally {
					RCUtils.safeClose(fw);
				}
			}
		}
		
		
		// write to (db-path)/users/user-().txt
	}
	
	@Override
	public void writeNewOrExistingLeaderboardEntries(List<DbLeaderboardEntry> dbe) {
		ObjectMapper om = new ObjectMapper();
		
		final Map<Long /* round id */, Map<Long /* user id*/, DbLeaderboardEntry>> toAdd = new HashMap<>();
		
		synchronized(lock) {
			// For each of the rounds that we will be writing to with this request, read the existing database entries
			// and add them to our map.
			dbe.stream().map(e -> e.getRoundId()).distinct().forEach( e -> {
				File f = new File(new File(dbRoot, "rounds"), "round-"+e+".txt");
				if(!f.exists()) { return; }
				
				JsonDbLeaderboardEntries jdle = readLeaderboardFile(f, om);
				if(jdle == null) { return; }
				
				jdle.getEntries().forEach( ld -> {
					
					Map<Long /*user id*/, DbLeaderboardEntry> userMap = toAdd.get(ld.getRoundId());
					if(userMap == null) {
						userMap = new HashMap<Long, DbLeaderboardEntry>();
						toAdd.put(ld.getRoundId(), userMap);
					}
					
					userMap.put(ld.getUserId(), ld);
				});
				
			});
		}
		
		// Now add the round entries we will be updating to our map.
		for(DbLeaderboardEntry dle : dbe) {
			
			if(!DbLeaderboardEntry.isValid(dle)) {
				log.severe("Invalid leaderboard entry in update map: "+dle, null);
				continue;
			}
			
			Map<Long /* user id*/, DbLeaderboardEntry> userMap = toAdd.get(dle.getRoundId());
			if(userMap == null) {
				userMap = new HashMap<>();
				toAdd.put(dle.getRoundId(), userMap);
			}
			
			userMap.put(dle.getUserId(), dle);
			
		}
				
		
		// Now write the map to file
		// write to (db-path)/rounds/round-().txt
		synchronized(lock) {

			toAdd.entrySet().forEach( round -> {
				long roundId = round.getKey();
				Map<Long, DbLeaderboardEntry> m = round.getValue();
				
				File f = new File(new File(dbRoot, "rounds"), "round-"+roundId+".txt");
				f.getParentFile().mkdirs();
				
				JsonDbLeaderboardEntries entries = new JsonDbLeaderboardEntries();				
				entries.getEntries().addAll(m.values());
				
				FileWriter fw = null;
				try {
					fw = new FileWriter(f);
					fw.write(om.writeValueAsString(entries));
				} catch(Exception e) {
					log.severe("Unable to write round to file: "+f.getPath(), e, null);
					e.printStackTrace();
					return;
				} finally {
					RCUtils.safeClose(fw);
				}
				
			});
			
		}
		
	}	

	/** Simple list of DbLeaderboardEntry, (de)serializes to JSON. */
	public static class JsonDbLeaderboardEntries {
		List<DbLeaderboardEntry> entries = new ArrayList<>();

		public List<DbLeaderboardEntry> getEntries() {
			return entries;
		}

		public void setEntries(List<DbLeaderboardEntry> entries) {
			this.entries = entries;
		}		
	}
	
	public static void mainMigrateUtil(String[] args) {
		
		FileDbBackend fdb = new FileDbBackend(new File("C:\\roguecloud"));
		
		List<DbLeaderboardEntry> l = fdb.getAllLeaderboardEntries();
		
		l.forEach( e -> {
			
//			if(e.getRoundId() % 1000 == 0) {
//				System.out.println(e.getRoundId());
//			}
			
			File f = new File("C:\\roguecloud\\rounds\\round-"+e.getRoundId()+".txt");
						
//			if(!f.exists()) {
//				System.out.println(f.getPath());
//			}
			
//			System.out.println(f.getPath()+"   "+new Date(f.lastModified()));
			
//			e.dateTime = f.lastModified();
		});

//		fdb.writeNewOrExistingLeaderboardEntries(l);
				
	}
	

}
