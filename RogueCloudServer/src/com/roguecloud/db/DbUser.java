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

import java.util.Arrays;
import java.util.List;

import com.roguecloud.utils.UsernameUtil;

public class DbUser implements IDBObject {

	public static final int NEW_USER_ID = -1;
	
	long userId;
	String username;
	String password;
	
	public DbUser() {
	}
	
	public DbUser(long userId, String username, String password) {
		this.userId = userId;
		this.username = username;
		this.password = password;
	}

	public long getUserId() {
		return userId;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public String toString() {
		return userId+" "+username+" "+password;
	}
	
	public DbUser fullClone() {
		return new DbUser(this.userId, this.username, this.password);
	}
	
	public void copyFromParam(DbUser user) {
		this.userId  = user.getUserId();
		this.username = user.getUsername();
		this.password = user.getPassword();
	}
		
	public static String isValid(DbUser user, boolean checkUserId) {
		if(checkUserId && user.getUserId() < 0) {
			return "User id is incorrect.";
		}
		
		if(user.getUsername() == null) {
			return "Invalid username.";
		}
		
		if(user.getUsername().length() > 18) {
			return "Username is too long: max 18 characters.";
		}
		
		if(user.getPassword() == null) {
			return "Invalid password.";
		}
		
		if(user.getUsername().trim().isEmpty()) {
			return "Username is empty.";
		}
		
		if(user.getPassword().trim().isEmpty()) {
			return "Password is empty.";
		}

		// Check invalid characters.
		{
			List<String> INVALID_CHARS = Arrays.asList(new String[]{ "%", "&", "*"});
			String username = user.getUsername();
			for(int x = 0; x < username.length(); x++) {
				String ch = username.substring(x,  x+1);
				
				if(INVALID_CHARS.contains(ch)) {
					return "Username must not contain invalid characters %, & or *";
				}
			}
		}

		if(!new UsernameUtil().isValidUserName(user.getUsername())) {
			return "Username contains invalid characters. Try a different username (for example, your initials).";
		}
		
//		if(user.getPassword().equalsIgnoreCase(user.getUsername())) {
//			return "Password may not equal username.";
//		}
		
//		if(user.getPassword().equalsIgnoreCase("password")) {
//			return "Password may not equal password.";
//		}
		
		return null;
		
	}
}
