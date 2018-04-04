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
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.roguecloud.ServerInstance;
import com.roguecloud.ServerInstanceList;

/** The browser client needs a list of all the available .PNG images that the server may send. This servlet
 * returns a list of all tiles, as a JSON string, for use by the browser. */
@WebServlet("/TileList")
public class TileListServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public TileListServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		ServerInstance si = ServerInstanceList.getInstance().getServerInstance();
		response.getWriter().append(si.getTileListJson());
		
	}

}
