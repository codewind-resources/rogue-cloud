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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.roguecloud.db.file.FileDbBackend;
import com.roguecloud.utils.Logger;

public class MemoryDatabase implements IDatabase {

	private static final Logger log = Logger.getInstance();
	
	private final Object lock = new Object();
	
	private List<DbUser> users_synch_lock = new ArrayList<>();

	private List<DbLeaderboardEntry> leaderboard_synch_lock = new ArrayList<>();
	
	private List<IDBObject> objectsToWrite_synch_lock = new ArrayList<>();

	private final FileDbBackend backend;
	
	private final MemoryDatabaseWriteThread thread;
	
	public MemoryDatabase() {
		File dir = new File(System.getProperty("user.home"), ".roguecloud");
		if(!dir.exists() && !dir.mkdirs()) {
			String msg = "Unable to create directory: "+dir.getPath();
			log.severe(msg, null);
			throw new RuntimeException(msg);
		}
		
		backend = new FileDbBackend(dir);
		
		users_synch_lock.addAll(backend.getAllUsers());
		leaderboard_synch_lock.addAll(backend.getAllLeaderboardEntries());
		
		thread = new MemoryDatabaseWriteThread();
		thread.start();
		
		
//		{
//			for(int x = 0; x < 30; x++) {
//				DbUser user = new DbUser(x, "user"+x, x+"user");
//				objectsToWrite_synch_lock.add(user);
//			}
//			
//			for(int x = 0; x < 300; x++) {
//				DbLeaderboardEntry entry = new DbLeaderboardEntry(x%30, x*10, (int)(Math.random() * 10));
//				objectsToWrite_synch_lock.add(entry);
//			}
//			
//			synchronized(objectsToWrite_synch_lock) {
//				objectsToWrite_synch_lock.notify();
//			}
//			
//		}
	}
	
	/**
	 *  User name and password are case insensitive!!!!!
	 *
	 */
	
	@Override
	public long createUser(DbUser u) {
		
		synchronized(lock) {
			String isValidUserName = DbUser.isValid(u, false);
			if(isValidUserName != null) {
				throw new RuntimeException("Invalid user name/password: "+isValidUserName);
			}
			
			if(users_synch_lock.stream().anyMatch(e -> e.getUsername().equalsIgnoreCase(u.username))) {
				log.err("Unable to find matching username in database:" +u.username, null);
				return -1;
			}
			
			// The first user is user 1
			long nextId = 1;
			for(DbUser e : users_synch_lock) {
				if(e.getUserId() >= nextId) {
					nextId = e.getUserId()+1;
				}
			}
			
			DbUser entry = new DbUser(nextId, u.getUsername().toLowerCase(), u.getPassword().toLowerCase());
			
			users_synch_lock.add(entry);
			objectsToWrite_synch_lock.add(entry);
			synchronized(objectsToWrite_synch_lock) {
				objectsToWrite_synch_lock.notify();
			}
			
			return entry.getUserId();
			
		}
	}

	@Override
	public DbUser getUserByUsername(String nameParam) {
		final String name = nameParam.trim().toLowerCase();
		
		synchronized(lock) {
			DbUser result = users_synch_lock.stream().filter(e -> e.getUsername().equalsIgnoreCase(name)).findFirst().orElse(null);
			
			if(result != null) {
				result = result.fullClone();
			}
			
			return result;
			
		}
	}

	@Override
	public DbUser getUserById(long id) {
		synchronized(lock) {
			DbUser result = users_synch_lock.stream().filter(e -> e.getUserId() == id).findFirst().orElse(null);
			if(result != null) {
				result = result.fullClone();
			}
			
			return result;
		}
	}

	@Override
	public void createOrUpdateDbLeaderboardEntry(DbLeaderboardEntry le) {
		if(!DbLeaderboardEntry.isValid(le)) {
			log.severe("Invalid leaderboard entry passed to create: "+le, null);
			return;
		}
		
		synchronized(lock) {
			
			DbLeaderboardEntry match = leaderboard_synch_lock.stream()
				.filter( e -> e.getRoundId() == le.getRoundId() && e.getUserId() == le.getUserId())
				.findFirst()
				.orElse(null);
			
			if(match == null) {
				leaderboard_synch_lock.add(le);
				objectsToWrite_synch_lock.add(le);
				synchronized(objectsToWrite_synch_lock) {
					objectsToWrite_synch_lock.notify();
				}

			} else {
				boolean result = leaderboard_synch_lock.remove(match);
				if(!result) {
					log.severe("Unable to find match in ldbd list, so couldn't replace.", null);
					return;
				}
				
				leaderboard_synch_lock.add(le);
				objectsToWrite_synch_lock.add(le);
				synchronized(objectsToWrite_synch_lock) {
					objectsToWrite_synch_lock.notify();
				}

			}
			
		}

	}

	
	@Override
	public List<DbLeaderboardEntry> getLeaderboardEntriesForARound(long roundId) {
		synchronized(lock) {
			final List<DbLeaderboardEntry> results = new ArrayList<>();
			
			leaderboard_synch_lock.stream()
				.filter( e-> e.getRoundId() == roundId)
				.forEach( e -> {
					results.add(e.fullClone());
				});
			
			return results;
		}
	}
	
	@Override
	public Long[] getUserBestScoreAndRank(long id) {
		
		List<DbLeaderboardEntry> l = getBestOverallLeaderboardEntries();

		int rank = 1;
		for(DbLeaderboardEntry dle : l) {
			if(dle.getUserId() == id) {
				
				return new Long[] { dle.getScore(), (long)rank};
			}
			
			rank++;
		}
			
		return null;
	}


	@Override
	public List<DbLeaderboardEntry> getBestOverallLeaderboardEntries() {
		synchronized(lock) {
			final List<DbLeaderboardEntry> results = new ArrayList<>();

			leaderboard_synch_lock.stream()
				.sorted( (a,b) -> {
					long val = b.getScore() - a.getScore();
					if(val > 0) { return 1;}
					else if(val == 0)  { return 0; }
					else { return -1; }})
				.forEach( e -> {
					results.add(e.fullClone());
				}
			);
			
			return results;
		}
	}

	@Override
	public List<DbLeaderboardEntry> getAllLeaderboardEntriesForUser(long id) {
		synchronized(lock) {
			final List<DbLeaderboardEntry> results = new ArrayList<>();
			leaderboard_synch_lock.stream()
				.filter(e -> e.getUserId() == id)
				.forEach(e -> {
					results.add(e.fullClone());
			});
			
			return results;
		}
	}

	@Override
	public boolean isValidPasswordForUser(String username, String password) {
		if(username == null || password == null) { return false; }
		
		username = username.trim().toLowerCase();
		DbUser user = getUserByUsername(username);
		if(user == null) {
			return false;
		}
		
		return user.getPassword().equalsIgnoreCase(password);		
		
	}
	
	public  class MemoryDatabaseWriteThread extends Thread {
		
		public MemoryDatabaseWriteThread() {
			setName(MemoryDatabaseWriteThread.class.getName());
			setDaemon(true);
		}
		
		
		public void run() {
			List<IDBObject> localObjectsToWrite = new ArrayList<>();
			List<DbUser> usersToWrite = new ArrayList<>();
			List<DbLeaderboardEntry> leaderboardEntriesToWrite = new ArrayList<>();

			while(true) {

				localObjectsToWrite.clear();
				usersToWrite.clear();
				leaderboardEntriesToWrite.clear();

				
				synchronized(objectsToWrite_synch_lock) {
					try {
						objectsToWrite_synch_lock.wait(1000);
					} catch (InterruptedException e) {
						log.severe("Unexpected thread interruption", e,  null);
						e.printStackTrace();
					}
				}
				
				synchronized(lock) {
					if(objectsToWrite_synch_lock.size() > 0) {
						localObjectsToWrite.addAll(objectsToWrite_synch_lock);
						objectsToWrite_synch_lock.clear();
					}
				}
				
				for(IDBObject o : localObjectsToWrite) {
					if(o instanceof DbLeaderboardEntry) {
						leaderboardEntriesToWrite.add((DbLeaderboardEntry) o);
					} else if(o instanceof DbUser) {
						usersToWrite.add((DbUser) o);
					} else {
						log.severe("Unknown DB object type in memory database: "+o, null);
					}					
				}
				
				// TODO: LOW - Simulate slow writes to the backend database, to verify this works as expected.
				
				// Remove duplicate user writes, then write
				{
					Map<Long, Boolean> userIdSeen = new HashMap<>();
					
					// Flip to descending chronological order (most recent entries are nearest to index 0)
					Collections.reverse(usersToWrite);
					
					for(Iterator<DbUser> it = usersToWrite.iterator(); it.hasNext(); ) {
						DbUser user = it.next();
						if(userIdSeen.containsKey(user.getUserId())) {
							it.remove();
						} else {
							userIdSeen.put(user.getUserId(), true);
						}
					}
					
					if(usersToWrite.size() > 0) {
						backend.writeNewOrExistingUsers(usersToWrite);
					}
				}
				
				// Remove duplicate leaderboard entries, then write
				{
					Map<LeaderboardPrimaryKey, Boolean> leaderboardEntrySeen = new HashMap<>();
					Collections.reverse(leaderboardEntriesToWrite);
					
					for(Iterator<DbLeaderboardEntry> it = leaderboardEntriesToWrite.iterator(); it.hasNext(); ) {
						DbLeaderboardEntry dbe = it.next();
						LeaderboardPrimaryKey key = new LeaderboardPrimaryKey(dbe); 
						if(leaderboardEntrySeen.containsKey(key)) {
							it.remove();
						} else {
							leaderboardEntrySeen.put(key, true);
						}
					}
					
					if(leaderboardEntriesToWrite.size() > 0) {
						backend.writeNewOrExistingLeaderboardEntries(leaderboardEntriesToWrite);
					}
					
				}
				
			}
			
		}
		
	}
	
	@Override
	public List<DbLeaderboardEntry> getAllLeaderboardEntriesForUserAndRound(long userId, long roundId) {
		final List<DbLeaderboardEntry> result = new ArrayList<DbLeaderboardEntry>();
		
		synchronized(lock) {
			leaderboard_synch_lock
				.stream()
				.filter( e -> e.getRoundId() == roundId && e.getUserId() == userId)
				.forEach( e -> { result.add(e.fullClone()); }
			);
		}
		
		return result;
	}

	
	private static class LeaderboardPrimaryKey {
		final private long userId;
		final private long roundId;
		
		public LeaderboardPrimaryKey(DbLeaderboardEntry dle) {
			this.userId = dle.getUserId();
			this.roundId = dle.getRoundId();
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof LeaderboardPrimaryKey)) {
				return false;
			}
			LeaderboardPrimaryKey other = (LeaderboardPrimaryKey)obj;
			
			if(other.userId != userId) {
				return false;
			}
			
			if(other.roundId != roundId) {
				return false;
			}
			
			return true;
		}
		
		@Override
		public int hashCode() {
			return (int)(userId* 32768+roundId);
		}
	}

	@Override
	public long getAndIncrementNextRoundId() {
		return backend.getAndIncrementNextRoundId();
	}


	
}
