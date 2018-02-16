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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.roguecloud.map.IMap;

public class ServerUtil {

	public static final String RC_ADMIN_USER = "rc_admin_user";
	public static final String RC_ADMIN_PASSWORD = "rc_admin_password";

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
		
		String val = null;
		try {
			val = (String) new InitialContext().lookup(RC_ADMIN_USER);
		} catch (NamingException e) { /* ignore */ }
		
		if(val == null) {
			val = System.getProperty(RC_ADMIN_USER);
		}
		if(val == null) {
			val = System.getenv(RC_ADMIN_USER);
		}
		return val;
	}
	
	public static String getAdminPassword() {
		String val = null;
		try {
			val = (String) new InitialContext().lookup(RC_ADMIN_PASSWORD);
		} catch (NamingException e) { /* ignore */ }
		
		if(val == null) {
			val = System.getProperty(RC_ADMIN_PASSWORD);
		}
		
		if(val == null) {
			val = System.getenv(RC_ADMIN_PASSWORD);
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
}
