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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.roguecloud.json.client.JsonUserRequest;
import com.roguecloud.json.client.JsonUserRequestResponse;

public class RegisterUser {

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
}
