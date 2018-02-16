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

package com.roguecloud.server;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.roguecloud.ServerInstance;
import com.roguecloud.ServerInstanceList;
import com.roguecloud.resources.Resources;
import com.roguecloud.resources.Resources.Page;
import com.roguecloud.utils.ServerUtil;

@WebServlet("/WorldView")
public class WorldViewServlet extends HttpServlet { 
	private static final long serialVersionUID = 1L;

	public WorldViewServlet() {
		super();
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String username = ServerUtil.getCookie(request, "username");
		String password = ServerUtil.getCookie(request, "password");
		
		if(username == null && password == null) {
			username = request.getParameter("username");
			password = request.getParameter("password");
			if(username != null && username.trim().isEmpty())  { username = null; }
			if(password != null && password.trim().isEmpty())  { password = null; }
		}
		
		if(username == null || password == null/* || uuid == null*/) {
			response.setStatus(400);
			response.getWriter().println("Missing username, password, or UUID fields.");
			return;
		}
		
		if(!ServerUtil.isAdminAuthenticatedAndAuthorized(username, password)) {
			response.getWriter().append("Invalid username / password");
			response.setStatus(HttpURLConnection.HTTP_FORBIDDEN);
			return;
		}

		ServerInstance si = ServerInstanceList.getInstance().getServerInstance();
		
		Page p = new Page();
//		p.setWebsocketUrl("ws://localhost:9080/RogueCloudServer/api/browser");  
		p.setUsername(username);
		p.setPassword(password);
		p.setUuid(null);
		p.setTilesJson(si.getTileListJson());
		p.setViewOnly(true);
//		p.setServerWebUrl("http://localhost:9080/RogueCloudServer");
		
		String pageStr = Resources.getInstance().generatePage(p, true);
		
		response.getWriter().append(pageStr);

		
	}
	
}
