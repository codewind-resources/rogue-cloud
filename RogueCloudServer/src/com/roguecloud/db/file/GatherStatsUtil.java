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

package com.roguecloud.db.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.roguecloud.db.DbLeaderboardEntry;
import com.roguecloud.db.DbUser;

public class GatherStatsUtil {
	
	
	public static void main(String[] args) {
		
		FileDbBackend fdb = new FileDbBackend(new File("C:\\import"));
		
		HashMap<Long, DbUser> userToDbUser = new HashMap<>();
		{
			List<DbUser> user = fdb.getAllUsers();
			user.forEach( e -> {
				userToDbUser.put(e.getUserId(), e);
			});
		}
		
		HashMap<Long, List<Date>> dateOfRaceEntered = new HashMap<>();
		
		List<DbLeaderboardEntry> l = fdb.getAllLeaderboardEntries();
		
		for(DbLeaderboardEntry dle : l) {
			
			dle.getUserId();
			
			List<Date> dates = dateOfRaceEntered.get((Long)dle.getUserId());
			if(dates == null) {
				dates = new ArrayList<>();
				dateOfRaceEntered.put((Long)dle.getUserId(), dates);
			}
			
			File actualFile = new File("C:\\import\\rounds\\round-"+dle.getRoundId()+".txt");
			long time = actualFile.lastModified();
			
			Date d = new Date(time);
			dates.add(d);
		}

		
		for(int x = 0; x < 12; x++) {
			
			System.out.println();
			System.out.println("Month "+x+": ");
			
			long monthStartInMsecs;
			{
				Calendar c  = Calendar.getInstance();
				c.set(Calendar.DAY_OF_MONTH, 1);
				c.set(Calendar.MONTH, x);
				c.set(Calendar.HOUR_OF_DAY, 0);
				c.set(Calendar.MINUTE, 0);
				c.set(Calendar.SECOND, 0);
				c.set(Calendar.MILLISECOND, 0);
				monthStartInMsecs = c.getTimeInMillis();
			}
			
			long monthEndInMsecs;
			{
				Calendar c  = Calendar.getInstance();

				c.set(Calendar.MONTH, x);
				c.set(Calendar.HOUR_OF_DAY, 23);
				c.set(Calendar.MINUTE, 59);
				c.set(Calendar.SECOND, 59);
				c.set(Calendar.MILLISECOND, 0);

				int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
				c.set(Calendar.DAY_OF_MONTH, max);
				
				monthEndInMsecs = c.getTimeInMillis();
			}
			
			System.out.println(new Date(monthStartInMsecs)+" "+new Date(monthEndInMsecs));
			
			for(Map.Entry<Long, List<Date>> entry : dateOfRaceEntered.entrySet()) {
				
				DbUser user = userToDbUser.get(entry.getKey());
				
				long numberOfRacesInMonth = entry.getValue().stream().filter(e ->  e.getTime() >= monthStartInMsecs && e.getTime() <= monthEndInMsecs ).count();
				
				if(numberOfRacesInMonth != 0) {
				
					System.out.println(user.getUsername()+","+user.getUserId()+","+numberOfRacesInMonth);
				}
				
			}
			
			
		}
		
		
		
	}
	
	
}
