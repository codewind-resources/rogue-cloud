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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 
 * A specialized logger for 'nitty gritty' details that are too verbose for a traditional logger. Writes log statements
 * to the file on a separate thread. 
 * 
 * You should first call setWhoami(..) before calling the log(...) method.
 *  
 * This class is an internal class, for server use only.
 */
public class NG {

	private static final NG instance = new NG();

	private NG() {
	}

	public static NG getInstance() {
		return instance;
	}

	public static void log(long gameTicks, String str) {
		instance.logInner(gameTicks, str);
	}

	// -----------------------------------------------------

	public static final boolean ENABLED = false;
	
	private final Object lock = new Object();

	public String whoami_synch_lock;

	private final List<String> toWrite_synch = new ArrayList<>();

	private NittyGrittyLogThread thread;

	public void setWhoami(String whoami) {
		if(!ENABLED) { return; } 
		
		synchronized (lock) {
			if (this.whoami_synch_lock != null) {
				return;
			}

			this.whoami_synch_lock = whoami;

			thread = new NittyGrittyLogThread(whoami);
			thread.start();
		}
	}

	private void logInner(long gameTicks, String str) {
		if(!ENABLED) { return; }
		
		if(thread == null) { return; }

		String gtText = "" + gameTicks;
		while (gtText.length() < 5) {
			gtText = "0" + gtText;
		}

		String statement = "[" + System.currentTimeMillis() + "] [" + gtText + "] [" + whoami_synch_lock + "] " + str;
		synchronized (toWrite_synch) {
			toWrite_synch.add(statement);
			toWrite_synch.notify();
		}

	}

	/** Write to file on a separate thread, so that we don't block the callng thread on writing to the disk.  */
	private class NittyGrittyLogThread extends Thread {

		private FileWriter fw;

		public NittyGrittyLogThread(String whoami) {
			super(NittyGrittyLogThread.class.getName());

			setDaemon(true);
			try {
				File dir = new File(System.getProperty("user.home"), ".roguecloud");
				
				dir = new File(dir, "ng-logs");
				
				if(!dir.exists()) { dir.mkdirs(); }
				
				fw = new FileWriter(new File(dir, whoami + ".log"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			List<String> localToWrite = new ArrayList<>();
			while (true) {

				try {

					localToWrite.clear();

					synchronized (toWrite_synch) {
						localToWrite.addAll(toWrite_synch);
						toWrite_synch.clear();
					}

					for (String str : localToWrite) {
						try {
							fw.write(str + "\r\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					try {
						fw.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}

					synchronized (toWrite_synch) {
						toWrite_synch.wait(50);
					}
				} catch (Throwable t) {
					/* ignore */
				}

			}
		}

	}

}
