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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.views.AllDocsResponse;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.client.org.lightcouch.TooManyRequestsException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roguecloud.db.DbLeaderboardEntry;
import com.roguecloud.db.DbUser;
import com.roguecloud.db.file.IDBBackend;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.MutableObject;
import com.roguecloud.utils.ServerUtil;

/** 
 * This database backend writes database entries to Cloudant. In order to use this backend, Cloudant credentials must
 * be specified as Environment variables, system properties, or server.xml JNDI values.
 *
 * The required credential properties are listed in the CONFIG_* fields below.
 **/
public class CloudantDbBackend implements IDBBackend {

	private static final Logger log = Logger.getInstance();
	
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
			final long finalX = x;
			
			retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
				try {
					CloudantDbUser cdbUser = db.find(CloudantDbUser.class, USER+finalX);
					result.add((DbUser)cdbUser);
				} catch(NoDocumentException e) { System.err.println("*Ignoring NDE on user:"+finalX); }
			});
		}
		
		return result;
		
	}

	@Override
	public List<DbLeaderboardEntry> getAllLeaderboardEntries() {
		Database db = db();
		
		List<DbLeaderboardEntry> retrievedDocs = new ArrayList<>();

		out("* Retrieving all document IDs");
		
		final List<String> docIds = new ArrayList<>();

		retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
			AllDocsResponse adr;
			try {
				adr = db.getAllDocsRequestBuilder().includeDocs(false).build().getResponse();
				docIds.addAll(adr.getDocIds());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		

		int lastOutputDocsRetrieved = 0;
		
		while(docIds.size() > 0) {

			// Remove 100 at a time
			List<String> toRetrieve = new ArrayList<>();
			while(toRetrieve.size() < 100 && docIds.size() > 0) {
				
				String currDocId = docIds.remove(0);
				
				if(!currDocId.contains(LDB_ENTRY)) { continue; }

				toRetrieve.add(currDocId);
			}
			
			retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
			
				try {
					retrievedDocs.addAll(db.getAllDocsRequestBuilder().keys(toRetrieve.toArray(new String[toRetrieve.size()])).includeDocs(true).build()
					           .getResponse().getDocsAs(CloudantDbLeaderboardEntry.class));
					
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			
			});
			
			if(retrievedDocs.size() - lastOutputDocsRetrieved > 500) {
				lastOutputDocsRetrieved = retrievedDocs.size();
				System.out.println("Retrieved "+retrievedDocs.size()+" docs.");
			}
		}
		
		out("* Retrieved "+retrievedDocs.size()+" leaderboard docs.");
			
		
		return retrievedDocs;
	}

	@Override
	public void writeNewOrExistingUsers(List<DbUser> users) {
		Database db = db();
		for(DbUser dbu : users) {
			
			if(dbu.getUserId() <= 0) {
				log.severe("Invalid user value: "+dbu, null);
				continue;
			}
			
			AtomicBoolean create = new AtomicBoolean(false);
			
			retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
				try {
						CloudantDbUser cdbu = db.find(CloudantDbUser.class, USER+dbu.getUserId());
						if(cdbu != null) {
							out("Updating user in db: "+dbu);
						// We found an existing entry in the database so update it
						cdbu.copyFromParam(dbu);
						db.update(cdbu);
						create.set(false);
					} else {
						create.set(true);
					}
				} catch(NoDocumentException e) { create.set(true); }
			
			});
			
			if(create.get()) {
				
				synchronized(lock) {
					if(dbu.getUserId() > nextUserId_synch_lock) {
						nextUserId_synch_lock = dbu.getUserId()+1;
					}
					writeValueAsLong(VALUE_NEXTUSERID, nextUserId_synch_lock, db);
				}
				
				retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
					CloudantDbUser cdbu = new CloudantDbUser();
					cdbu.copyFromParam(dbu);
					cdbu.set_id("user:"+dbu.getUserId());
					db.save(cdbu);
					out("Created new user in db: "+dbu);
				});
			}
			
		}
	}
	
	private static final long MAX_TIME_IN_MSECS= 120 * 1000;
	private static final long INITIAL_TIME_TO_WAIT = 100;
	private static final float UPDATE_FACTOR = 2f;

	@Override
	public void writeNewOrExistingLeaderboardEntries(List<DbLeaderboardEntry> dbe) {
		
		Database db = db();
		for(DbLeaderboardEntry dle : dbe) {

			if(!DbLeaderboardEntry.isValid(dle)) {
				log.severe("Invalid leaderboard entry: "+dle, null);
				return;
			}
			
			final AtomicBoolean create = new AtomicBoolean(false);
			
			String id = LDB_ENTRY+dle.getRoundId()+"-"+dle.getUserId();
			
			retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
				try {

					CloudantDbLeaderboardEntry cdble = db.find(CloudantDbLeaderboardEntry.class, id);
					if(cdble != null) {
						// We found an existing entry in the database so update it
						cdble.copyFromParam(dle);
						db.update(cdble);
						create.set(false);
						out("Updating leaderboard in db: "+dle);
					} else {
						create.set(true);
					}
				} catch(NoDocumentException e) { create.set(true); } 
				
			});

			if(create.get()) {
				retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
					CloudantDbLeaderboardEntry cdble = new CloudantDbLeaderboardEntry();				
					cdble.copyFromParam(dle);
					cdble.set_id(id);
					db.save(cdble);
					out("Creating leaderboard in db: "+dle);
				});
			}

		}
	}
		
	private static long getNextUserId(Database db) {
		return readValueAsLongNew(VALUE_NEXTUSERID, db, 0);
	}
	
	private static void writeValueAsLong(String field, long value, Database db) {
		
		final AtomicBoolean create = new AtomicBoolean(false);
		retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
			try { 
				
				InputStream is = db.find(field);
				if(is != null)  {
	
					JsonObject jo = (JsonObject) new JsonParser().parse(new InputStreamReader(is));
					String rev = jo.get("_rev").getAsString();
					
					JsonObject json = new JsonObject();
					json.addProperty("_id", field);
					json.addProperty("value", value);
					json.addProperty("_rev", rev);
					
					@SuppressWarnings("unused")
					Response response = db.update(json);
				}
			} catch(NoDocumentException nde) {
				create.set(true);
			}
			
		});
				

		if(create.get()) {
			
			retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
				JsonObject json = new JsonObject();
				json.addProperty("_id", field);
				json.addProperty("value", value);
				
				@SuppressWarnings("unused")
				Response response = db.save(json);				
			});
			
		}
		
	}
	
	private static long readValueAsLongNew(String field, Database db, long defaultIfNotFound) {
		
		final MutableObject<InputStream> isM = new MutableObject<>();
		
		retryOnFail(MAX_TIME_IN_MSECS, INITIAL_TIME_TO_WAIT, UPDATE_FACTOR, () -> {
			try {
				isM.set(db.find(field));
			} catch(NoDocumentException nde ) {
				if(!(nde instanceof NoDocumentException)) { nde.printStackTrace(); }
			}
		});
		
		InputStream is = isM.get();
		if(is == null) {
			return defaultIfNotFound;
		}
		
		try {

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
			String MSG = "WARNING: More than one cloudant db field matched, but not all could be found. There may be missing properties.";
			log.severe(MSG, null);
			// An exception will be thrown by the caller.
		}
			
		return result;
		
	}
	
	private static Database db() {
		CloudantClient client;
		try {
			String url = ServerUtil.getConfigValue(CONFIG_URL);
			String username = ServerUtil.getConfigValue(CONIG_USERNAME);
			String password = ServerUtil.getConfigValue(CONFIG_PASSWORD);
			String dbName = ServerUtil.getConfigValue(CONFIG_DB_NAME);

			SSLSocketFactory internalSSLSocketFactory = null;
			
			try {
				SSLContext context = SSLContext.getInstance("TLSv1.2");
				context.init(null, null, null);
				internalSSLSocketFactory = context.getSocketFactory();
			} catch (KeyManagementException e) {
				throw new RuntimeException(e);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			
			client = ClientBuilder.url(new URL(url))
					.username(username)
					.password(password)
					.customSSLSocketFactory(internalSSLSocketFactory)
					.build();
			
			// Get a Database instance to interact with, but don't create it if it doesn't already exist
			Database db = client.database(dbName, true);

			return db;

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

	}

	public void internalSetNextRoundId(long nextRoundId) {
		
		Database db = db();
		writeValueAsLong(VALUE_NEXTROUNDID, nextRoundId, db);
		
	}
	
	public long internalGetNextRoundId() {
		Database db = db();
		return readValueAsLongNew(VALUE_NEXTROUNDID, db, 1);
	}
	
	@Override
	public long getAndIncrementNextRoundId() {
		
		Database db = db();
		
		long returnValue = readValueAsLongNew(VALUE_NEXTROUNDID, db, 1);
		
		writeValueAsLong(VALUE_NEXTROUNDID, returnValue+1, db);
		
		return returnValue;
	}
	
	
	/**
	 * Keep retrying a runnable until it succeeds
	 * @param maxTimeInMsecs The total time to keep retrying, in milliseconds
	 * @param initialTimeToWaitOnFailInMsecs The initial time to wait between failures
	 * @param updateFactorOnFail After each failure, multiple the time to wait by this value
	 * @param r The actual bit of code to run
	 */
	private static void retryOnFail(long maxTimeInMsecs, long initialTimeToWaitOnFailInMsecs, float updateFactorOnFail, Runnable r) { 
		if(updateFactorOnFail < 1) { throw new IllegalArgumentException("update factor must be >= 1"); }
		
		long expireTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(maxTimeInMsecs, TimeUnit.MILLISECONDS);
		
		long timeToSleepInMsecs = initialTimeToWaitOnFailInMsecs;
		
		Throwable lastThrowable = null;
		
		while(System.nanoTime() < expireTimeInNanos) {
			
			try {
				r.run();
				lastThrowable = null;
				break;
			} catch(Throwable t) {
				if(!(t instanceof TooManyRequestsException)) {
					System.err.println(t.getClass().getName()+" "+t.getMessage());					
				}
				lastThrowable = t;
				try { Thread.sleep(timeToSleepInMsecs); } catch (InterruptedException e) { throw new RuntimeException(e); }
				timeToSleepInMsecs *= updateFactorOnFail;
			}
			
		}
		
		if(lastThrowable != null) {
			if(lastThrowable instanceof RuntimeException) {
				throw (RuntimeException)lastThrowable;
			} else {
				throw new RuntimeException(lastThrowable);
			}
		}
		
	}
}
