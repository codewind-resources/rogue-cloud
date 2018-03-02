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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.views.AllDocsResponse;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roguecloud.db.DbLeaderboardEntry;
import com.roguecloud.db.DbUser;
import com.roguecloud.db.file.IDBBackend;
import com.roguecloud.utils.ServerUtil;

public class CloudantDbBackend implements IDBBackend {

	private static final boolean LOGGING = true;
	
	private final Object lock = new Object();
	
	private long nextUserId_synch_lock;
	
	private static final String VALUE_NEXTUSERID = "value:nextUserId";
	private static final String VALUE_NEXTROUNDID = "value:nextRoundId";

	private static final String USER = "user:";
	private static final String LDB_ENTRY = "leaderboard-entry:";
	
	public static final String CONFIG_URL = "cloudant_url";
	public static final String CONIG_USERNAME = "cloudant_username";
	public static final String CONFIG_PASSWORD = "cloudant_password";
	public static final String CONFIG_DB_NAME = "cloudant_db_name";
	

	public CloudantDbBackend() {
		
		if(!isCloudAntDbConfigured()) {
			throw new IllegalArgumentException("Cloudant DB not configured.");
		}
		
		Database db = db();
		
		nextUserId_synch_lock = getNextUserId(db);
	}
	
	@Override
	public List<DbUser> getAllUsers() {
		
		Database db = db();
		
		List<DbUser> result = new ArrayList<DbUser>();
		
		long nextUserId;
		synchronized (lock) {
			nextUserId = nextUserId_synch_lock;
		}
		
		out("* Requesting users from Cloudant, up to "+nextUserId);
		
		for(long x = 1; x < nextUserId; x++) {
			try {
				CloudantDbUser cdbUser = db.find(CloudantDbUser.class, USER+x);
				result.add((DbUser)cdbUser);
			} catch(NoDocumentException e) { System.err.println("*Ignoring NDE on user:"+x); }
		}
		
		return result;
		
	}

	@Override
	public List<DbLeaderboardEntry> getAllLeaderboardEntries() {
		Database db = db();
		
		List<DbLeaderboardEntry> result = new ArrayList<>();

		try {
			out("* Retrieving all document IDs");
			AllDocsResponse adr = db.getAllDocsRequestBuilder().includeDocs(false).build().getResponse();
			
			int count = 0;
			
			out("* Retrieving at most "+adr.getDocIds().size()+" leaderboard docs.");
			List<String> docIds = adr.getDocIds();
			for(String docId : docIds) {
				if(!docId.contains(LDB_ENTRY)) { continue; }
				
				CloudantDbLeaderboardEntry dle = db.find(CloudantDbLeaderboardEntry.class, docId);
				
				result.add((DbLeaderboardEntry)dle);
				count++;
			}
			
			out("* Retrieved "+count+" leaderboard docs.");
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return result;
	}

	@Override
	public void writeNewOrExistingUsers(List<DbUser> users) {
		Database db = db();
		for(DbUser dbu : users) {
			
			if(dbu.getUserId() <= 0) {
				// TODO: Handle this better.
				throw new IllegalArgumentException("Invalid user id: "+dbu.getUsername());
			}
			
			boolean create;
			
			try {
					CloudantDbUser cdbu = db.find(CloudantDbUser.class, USER+dbu.getUserId());
					if(cdbu != null) {
						out("Updating user in db: "+dbu);
					// We found an existing entry in the database so update it
					cdbu.copyFromParam(dbu);
					db.update(cdbu);
					create = false;
				} else {
					create = true;
				}
			} catch(NoDocumentException e) { create = true; } 
			
			if(create) {
				
				synchronized(lock) {
					if(dbu.getUserId() > nextUserId_synch_lock) {
						nextUserId_synch_lock = dbu.getUserId()+1;
					}
					writeValueAsLong(VALUE_NEXTUSERID, nextUserId_synch_lock, db);
				}
				
				CloudantDbUser cdbu = new CloudantDbUser();
				cdbu.copyFromParam(dbu);
				cdbu.set_id("user:"+dbu.getUserId());
				db.save(cdbu);
				out("Created new user in db: "+dbu);
			}
			
		}
	}

	@Override
	public void writeNewOrExistingLeaderboardEntries(List<DbLeaderboardEntry> dbe) {
		
		Database db = db();
		for(DbLeaderboardEntry dle : dbe) {

			if(!DbLeaderboardEntry.isValid(dle)) {
				// TODO: Handle this better. Ignore?
				throw new IllegalArgumentException("Invalid leaderboard entry: "+dle.toString());
			}
			
			boolean create;
			
			String id = LDB_ENTRY+dle.getRoundId()+"-"+dle.getUserId();
			
			try {
				CloudantDbLeaderboardEntry cdble = db.find(CloudantDbLeaderboardEntry.class, id);
				if(cdble != null) {
					// We found an existing entry in the database so update it
					cdble.copyFromParam(dle);
					db.update(cdble);
					create = false;
					out("Updating leaderboard in db: "+dle);
				} else {
					create = true;
				}
			} catch(NoDocumentException e) { create = true; } 

			if(create) {
				CloudantDbLeaderboardEntry cdble = new CloudantDbLeaderboardEntry();				
				cdble.copyFromParam(dle);
				cdble.set_id(id);
				db.save(cdble);
				out("Creating leaderboard in db: "+dle);
			}

		}
	}
		
	private static long getNextUserId(Database db) {
		return readValueAsLong(VALUE_NEXTUSERID, db, 0);
	}
	
	private static void writeValueAsLong(String field, long value, Database db) {
		
		boolean create = false;
		try { 
			InputStream is = db.find(field);
			if(is != null)  {
				JsonObject jo = (JsonObject) new JsonParser().parse(new InputStreamReader(is));
				String rev = jo.get("_rev").getAsString();
				
				JsonObject json = new JsonObject();
				json.addProperty("_id", field);
				json.addProperty("value", value);
				json.addProperty("_rev", rev);
				Response response = db.update(json);
				
			}
		} catch(NoDocumentException nde) {
			create = true;
		}

		if(create) {
			JsonObject json = new JsonObject();
			json.addProperty("_id", field);
			json.addProperty("value", value);
			Response response = db.save(json);
		}
		
	}
	
	private static long readValueAsLong(String field, Database db, long defaultIfNotFound) {
		
		try {
			
			InputStream is = db.find(field);
			if(is == null) {
				return defaultIfNotFound;
			}
			
			JsonObject jo = (JsonObject) new JsonParser().parse(new InputStreamReader(is));
			return jo.get("value").getAsLong();
			
		} catch(NoDocumentException | NumberFormatException nde ) {
			if(!(nde instanceof NoDocumentException)) { nde.printStackTrace(); }
			return defaultIfNotFound;
		}
	}
	
	private static void out(String str) {
		if(!LOGGING) { return; }
		System.out.println(str);
	}
	
	public static boolean isCloudAntDbConfigured() {
		
		String[] fields = new String[] {
				CONFIG_URL, CONIG_USERNAME, CONFIG_PASSWORD, CONFIG_DB_NAME 
		};
		
		int matchedFields = 0;
		
		boolean result = true;
		
		for(String field : fields) {

			String text = ServerUtil.getConfigValue(field); 
			
			if(text == null) { result = false; }
			else if(text.trim().isEmpty()) { result = false; } 
			else {
				matchedFields++;	
			}
		
		}
		
		if(!result && matchedFields > 0) {
			// TODO: EASY - Convert to severe error.
			System.err.println("WARNING: More than one cloudant db field matched, but not all could be found. There may be missing properties.");
		}
			
		return result;
		
	}
	
	private static Database db() {
		CloudantClient client;
		try {
			client = ClientBuilder.url(new URL(ServerUtil.getConfigValue(CONFIG_URL)))
					.username(ServerUtil.getConfigValue(CONIG_USERNAME))
					.password(ServerUtil.getConfigValue(CONFIG_PASSWORD)).build();
			
			// Get a Database instance to interact with, but don't create it if it doesn't already exist
			Database db = client.database(ServerUtil.getConfigValue(CONFIG_DB_NAME), true);

			return db;

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public long getAndIncrementNextRoundId() {
		
		Database db = db();
		
		long returnValue = readValueAsLong(VALUE_NEXTROUNDID, db, 1);
		
		writeValueAsLong(VALUE_NEXTROUNDID, returnValue+1, db);
		
		return returnValue;
	}
}
