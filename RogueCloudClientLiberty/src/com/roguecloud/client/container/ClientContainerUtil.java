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

package com.roguecloud.client.container;

import java.util.Arrays;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.roguecloud.utils.HttpClientUtils;
import com.roguecloud.utils.Logger;

/** Utility methods */
public class ClientContainerUtil {

	private static Logger log = Logger.getInstance();
	
	
	/** Get the list of tiles (in json format) from the user, see TileList servlet for details. */
	public static String getTileListJson(String resourceUrl) {
		
		Client client = HttpClientUtils.generateJaxRsHttpClient();
		
		while(resourceUrl.endsWith("/")) {
			resourceUrl = resourceUrl.substring(0, resourceUrl.length()-1);
		}
		
		WebTarget target = client.target(resourceUrl + "/TileList");
		
		Response response = target.request().get();

		if(response.getStatus() != 200) {
			log.severe("Unexpected response from tile list endpoint", null);
			return null;
		}
		
		String tileListJson = response.readEntity(String.class);
		
		return tileListJson;
		
	}
	
	
	public static void loudlyInformUser(String str) {
		String[] list = new String[] {
				"",
				"    |",
				"    |",
				"    |",
				"    |  ",
				"\\   |   /",
				" \\  |  /",
				"  \\ | /",
				"   \\|/",
				"    .",
				str,
				"    .",
				"   /|\\",
				"  / | \\",
				" /  |  \\",
				"/   |   \\",
				"    |",
				"    |",
				"    |",
				"    |",
				"    |",
			
		};
		
		Arrays.asList(list).forEach( e -> System.err.println(e));
		
	}
	
	
}
