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

package com.roguecloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.roguecloud.NG;
import com.roguecloud.utils.Logger;

/** 
 * At present, there is only ever a single ServerInstance, but this ServerInstanceList class exists to provide 
 * architectural support for a single instance of this application running multiple simultaneous server instances. 
 * 
 * In addition to containing a list of active server instances, this class also periodically dumps thread stack traces for
 * debugging purposes.
 **/
public final class ServerInstanceList {

	private static final ServerInstanceList instance = new ServerInstanceList();   
	
	private ServerInstanceList() {
		try {
			NG.getInstance().setWhoami("server");
			
			onlyInstance = new ServerInstance();
			
			new ThreadStackDumpThread().start();
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static ServerInstanceList getInstance() {
		return instance;
	}
	
	// --------------------------------------
	
	private final ServerInstance onlyInstance;
	
	public final ServerInstance getServerInstance() {
		return onlyInstance;
		
	}

	
	/** Periodically dumps thread stack traces for debugging purposes. */
	private static class ThreadStackDumpThread extends Thread{
		private final Logger log = Logger.getInstance();
		
		public ThreadStackDumpThread() {
			setDaemon(true);
			setName(ThreadStackDumpThread.class.getName());;
		}
		
		
		@Override
		public void run() {
			
			while(true) {
				try { TimeUnit.MINUTES.sleep(30); } catch (InterruptedException e) { throw new RuntimeException(e); }

				log.interesting(getAllThreadStacktraces(), null);
				
			}
		}
		
		public static String getAllThreadStacktraces() {
			final String CRLF = System.getProperty("line.separator");
			
			Map<Thread, StackTraceElement[]> m = Thread.getAllStackTraces();
			
			List<Map.Entry<Thread, StackTraceElement[]>> threadList = new ArrayList<Map.Entry<Thread, StackTraceElement[]>>();
			threadList.addAll(m.entrySet());
			
			Collections.sort(threadList, new Comparator<Map.Entry<Thread, StackTraceElement[]>>() {

				@Override
				public int compare(Entry<Thread, StackTraceElement[]> o1, Entry<Thread, StackTraceElement[]> o2) {
					return (int)(o1.getKey().getId() - o2.getKey().getId());
				}
				
			});
			
			StringBuilder sb = new StringBuilder();
			
			for(Map.Entry<Thread, StackTraceElement[]> e : threadList) {
				Thread t = e.getKey();
				StackTraceElement[] stes = e.getValue();
				
				sb.append("- Thread "+t.getId()+" ["+t.getName()+"]: "+CRLF);
				if(stes.length > 0) {
					for(StackTraceElement ste : stes) {
						sb.append("    "+ste+CRLF);
					}
				} else {
					sb.append("    None."+CRLF);
				}
				sb.append(""+CRLF);
			}
			
			return sb.toString();
		}

	}
}
