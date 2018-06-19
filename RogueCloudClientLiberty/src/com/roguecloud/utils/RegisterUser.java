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

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.roguecloud.json.client.JsonUserRequest;
import com.roguecloud.json.client.JsonUserRequestResponse;

/** HTTP client utility method to register a new user in the server's user database if they are not already registered. */
public class RegisterUser {

	public static ClientApiVersionReturn isClientApiVersionSupported(String apiVersion, String resourceUrl, long expireTimeInMsecs) {
		
		long expireTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(expireTimeInMsecs, TimeUnit.MILLISECONDS);
		
		boolean lastResult = false;
		
		boolean connectionSucceeded = false;
		
		Exception lastException = null;
		
		while(System.nanoTime() < expireTime && !connectionSucceeded) {
			
			int httpResponseCode = -1;
			
			try {
				httpResponseCode = issueClientApiVersionSupportedRequest(apiVersion, resourceUrl);
				lastException = null;
			} catch(Exception ce) {
				/* unable to connect, try again after waiting. */
				RCUtils.sleep(1000);
				lastException = ce;
			}
			
			if(httpResponseCode != 404 && httpResponseCode != 500 && httpResponseCode != -1) {
				connectionSucceeded = true;
			}
			
			lastResult = (httpResponseCode == 200);
			
		}
		
		if(lastResult) {
			// Success
			return new ClientApiVersionReturn(true, null);
		} 
		
		// API Version not supported, or unable to connect.
		return new ClientApiVersionReturn(lastResult, lastException);
	}
	
	private static int issueClientApiVersionSupportedRequest(String apiVersion, String resourceUrl) {
		Client client = HttpClientUtils.generateJaxRsHttpClient();
		
		while(resourceUrl.endsWith("/")) {
			resourceUrl = resourceUrl.substring(0, resourceUrl.length()-1);
		}
		
		WebTarget target = client.target(resourceUrl + "/services/apiVersion").path(apiVersion).path("supported");
		
		Invocation.Builder builder = target.request("application/json");		
		
		Response response = builder.get();

		return response.getStatus();		

	}

	
	/** Register a user if they don't already exist, otherwise check the password and return the user id.*/
	public static long registerAndGetUserId(String username, String password, String resourceUrl) {
		
		Client client = HttpClientUtils.generateJaxRsHttpClient();
		
		while(resourceUrl.endsWith("/")) {
			resourceUrl = resourceUrl.substring(0, resourceUrl.length()-1);
		}
		
		WebTarget target = client.target(resourceUrl + "/database/user/name").path(username);
		
		Invocation.Builder builder = target.request("application/json");
		
		JsonUserRequest userRequest = new JsonUserRequest();
		userRequest.setUsername(username);
		userRequest.setPassword(password);
		
		Response response = builder.put(Entity.entity(userRequest, MediaType.APPLICATION_JSON_TYPE));
		
		if(response.getStatus() == 401) {
			String errFromServer = response.readEntity(String.class);
			throw new RuntimeException("Password is incorrect: "+errFromServer);
		}
		
		if(response.getStatus() == 500 || response.getStatus() == 403) {
			String errFromServer = response.readEntity(String.class);
			throw new RuntimeException("An error occured on username check: "+errFromServer);
		}
		
		JsonUserRequestResponse jsonResponse = response.readEntity(JsonUserRequestResponse.class);
	
		return jsonResponse.getUserId();
		
	}
	
	/** Return value for 'isClientApiVersionSupported' */
	public static class ClientApiVersionReturn {
		private final boolean supported;
		private final Exception exception;
		
		public ClientApiVersionReturn(boolean supported, Exception exception) {
			this.supported = supported;
			this.exception = exception;
		}

		/** True if client api version is supported, false if not supported or an error occurred */
		public boolean isSupported() {
			return supported;
		}

		public Exception getException() {
			return exception;
		}
		
		
	}
}
