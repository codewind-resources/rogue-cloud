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

package com.roguecloud.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.roguecloud.db.DbUser;
import com.roguecloud.map.IMap;

/** Various standalone utility methods used only by server-side code. */
public class ServerUtil {

	public static final String RC_ADMIN_USER = "rc_admin_user";
	public static final String RC_ADMIN_PASSWORD = "rc_admin_password";

	public static final String SHA_256_FIELD = "{sha256}";
	
	
	public static boolean computeAndCompareEqualPasswordHashes(DbUser userFromDb, String providedUserPassword) {
		
		// Sanity check the user
		if(DbUser.isValid(userFromDb, true) != null)  {
			return false;
		}
	
		// Convert the user password to a hash
		String passwordHash = oneWayFunction(providedUserPassword);
		
		// Extract the sha256 base64 token
		String dbPassword = userFromDb.getPassword();
		dbPassword = dbPassword.replace(SHA_256_FIELD, "").trim();
		
		// Return true if they  match.
		return dbPassword.equals(passwordHash);
		
	}
	
	/** Create a base-64 representation of the SHA-256 hash */
	public static String oneWayFunction(String password) {
		try {
			// Password is case-insensitive and whitespace-insensitive
			password = password.toLowerCase().trim();
			
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] byteArr = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			
			String hashedPassword = Base64.getEncoder().encodeToString(byteArr);
			
			return hashedPassword;
			
			
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	
	/** Returns true if the given username/password match the admin's username and password, false otherwise. */
	public static boolean isAdminAuthenticatedAndAuthorized(String usernameParam, String passwordParam) {
		if(usernameParam == null || passwordParam == null) { return false; }
		
		String adminUsername = getAdminUserName();
		if(adminUsername == null || adminUsername.trim().isEmpty()) { return false; }
		
		String adminPassword = getAdminPassword();
		if(adminPassword == null || adminPassword.trim().isEmpty()) { return false; }
		
		if(adminUsername.equalsIgnoreCase(usernameParam) && adminPassword.equals(passwordParam)) {
			return true;
		}
		
		return false;
		
	}
	
	public static String getAdminUserName() {
		
		return getConfigValue(RC_ADMIN_USER);
	}
	
	public static String getAdminPassword() {
		return getConfigValue(RC_ADMIN_PASSWORD);
	}

	
	/** Look for a pre-defined configuration value: first in server.xml, then -D properties, then system env vars. */
	public static String getConfigValue(String field) {
		String val = null;
		try {
			val = (String) new InitialContext().lookup(field);
		} catch (NamingException e) { /* ignore */ }
		
		if(val == null) {
			val = System.getProperty(field);
		}
		
		if(val == null) {
			val = System.getenv(field);
		}
		return val;
		
	}
	
	public static String getCookie(HttpServletRequest request, String key) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) { return null; }
		
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(key)) {
				return cookie.getValue();
			}
		}
		
		return null;
	}

	// At most search 33% of the space
	public static long maxAStarSearch(IMap m) {
		return (long)((m.getXSize()+m.getYSize())/3);
	}
	
	/** Run a runnable in a simple thread. */
	public static void runInAnonymousThread(Runnable r) {
		Thread t = new Thread(r);
		t.setName(r.getClass().getName());
		t.setDaemon(true);
		t.start();		
	}
	
	
	public static InputStream getServerResource(Class<?> c, String resourcePath) {
		InputStream inputStream = c.getClassLoader().getResourceAsStream(resourcePath);
		
		// If we are running outside of Liberty, we need to cheat and use the user.dir to find the resources.
		if(inputStream == null) {
			File newFile = new File(System.getProperty("user.dir"));
			newFile = new File(newFile.getParentFile(), "RogueCloudServer/WebContent"+resourcePath);
			
			if(!newFile.exists()) {
				
				// Here we assume we are running from the target/ directory of one of the other projects
				newFile = new File(System.getProperty("user.dir"));
				newFile = new File(newFile.getParentFile(), "../RogueCloudServer/WebContent"+resourcePath);
				
			}
			
			if(newFile.exists()) {
				try {
					inputStream = new FileInputStream(newFile);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e); // Convert to unchecked
				}
			}
		}

		return inputStream;
	}
	
	
	/** 
	 * To track performance data of a block of code:
	 * 
	 * PerfData start = new PerfData();
	 * {
	 * 		start.reset();
	 *		// Some code
	 * }
	 * start.output("Performance for this code block");
	 **/
	public static class PerfData {
		
		private long startTimeInNanos;
		
		public PerfData() {
			this.startTimeInNanos = System.nanoTime();
		}
		
		
		public void reset() {
			this.startTimeInNanos = System.nanoTime();
		}
		
		public void output(String type) {
			System.out.println("Elapsed time for '" + type + "': " + TimeUnit.MILLISECONDS.convert(System.nanoTime()-startTimeInNanos, TimeUnit.NANOSECONDS));
		}
		
	}
}
