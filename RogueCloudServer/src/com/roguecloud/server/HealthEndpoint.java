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
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.roguecloud.RCConstants;
import com.roguecloud.ServerInstanceList;
import com.roguecloud.utils.Logger;

/** In order to work around an issue with the Liberty runtime leaking WebSockets, the Rogue Cloud server will
 * ask Kubernetes to restart itself after a certain elapsed period of time (but always on a weekend day). 
 * 
 * After the elapsed time (and on the correct day), the health endpoint will return 500, rather than 200.
 **/
@WebServlet("/RogueCloudServerHealth")
public class HealthEndpoint extends HttpServlet {
	
	private final Logger log  = Logger.getInstance();
	
	private final AtomicInteger logCount_synch = new AtomicInteger(0);
	
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HealthEndpoint() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		int responseCode = 200;
		
		Calendar c = Calendar.getInstance();
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		
		if(dayOfWeek == Calendar.SATURDAY ||  dayOfWeek != Calendar.SUNDAY) {
			int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
			if(hourOfDay == 4) {
				
				long expireTime = ServerInstanceList.getInstance().getStartTimeInNanos() + RCConstants.RESTART_SERVER_AFTER_X_NANOSECONDS;
				
				if(System.nanoTime() > expireTime) {
					
					responseCode = 500;
					
					synchronized (logCount_synch) {
						
						int numTimesLogged = logCount_synch.getAndIncrement();
						if(numTimesLogged < 10) {
							log.severe("Returning 500 from health endpoint to trigger scheduled restart.", null);
						}
					}
					
				}
			} 
		}
		
		
		response.setStatus(responseCode);
			
		
	}

}
