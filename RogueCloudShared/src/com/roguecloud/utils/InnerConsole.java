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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 
 * For internal use only.
 * 
 * A singleton thread is created, which responds to write requests to the console from the Console class.
 * 
 * Writes to the log file are received by other threads through the write(...) method, which are then 
 * passed to this console thread to write to file. 
 * 
 * The purpose of this class being on a separate thread is to prevent slow IO writes from blocking
 * threads that log to file.
 *  
 **/
public class InnerConsole {

	private static final boolean ENABLED = true;
	
	private static final InnerConsole instance = new InnerConsole();
	
	private InnerConsole() {
		
		if(ENABLED) {
			logDir = new File(new File(System.getProperty("user.home"), ".roguecloud"),  "logs");
			logDir.mkdirs();
			
			if(!logDir.exists()) {
				String MSG = "Cannot create log file.";
				throw new RuntimeException(MSG);
			}
			
			ICThreadWrapper thread = new ICThreadWrapper();
			thread.setDaemon(true);
			thread.start();
		}
	}
	
	public static InnerConsole getInstance() {
		return instance;
	}
	
	// -------------------------
	
	private final static String CR = System.getProperty("line.separator");

	private final File logDir;
	
	private String severeError = null;
	
	private List<String> out = new ArrayList<>();
	

	public void write(String message) {
		if(!ENABLED) { return; }
		
		synchronized(out) {
			if(severeError != null) {
				throw new RuntimeException(severeError);
			}
			
			out.add(message);
			out.notify();
		}
		
	}
	
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE-MMM-d-yyyy");
	
	private static final String getCptLogFileName() {		
		return "cpt-"+System.currentTimeMillis()+"-"+sdf.format(new Date())+".log";
		
	}

	private void run() {
		final List<String> localCopy = new ArrayList<String>();
		
		FileOutputStream fos = null;
		try {
			long bytesWrittenToCurrentFos = 0;
			fos = new FileOutputStream(new File(logDir, getCptLogFileName()));
			
			while(true) {
				
				synchronized(out) {
					try {
						out.wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					localCopy.addAll(out);
					out.clear();
				}
				
				for(String str : localCopy) {
					byte[] bytes =(str+CR).getBytes();
					bytesWrittenToCurrentFos += bytes.length;
					fos.write(bytes);
				}
				
				localCopy.clear();
				
				if(bytesWrittenToCurrentFos > 1024 * 1024 * 128) {
					fos.close();
					fos = new FileOutputStream(new File(logDir, getCptLogFileName())); 
					bytesWrittenToCurrentFos = 0;					
				}
				
			}

		} catch (FileNotFoundException e1) {
			synchronized (out) {
				severeError = "Unable to open FileOutputStream to "+logDir;
			}
			throw new RuntimeException(e1);
		} catch(Exception e) {
			synchronized (out) {
				severeError = "Exception occured in inner console logger: " +e.getClass().getName()+" "+e.getMessage();
			}
			throw new RuntimeException(e);
		} finally {
			try { fos.close(); } catch (Exception e) { }
		}
		
	}

	/** Utility class to call run method. */
	private class ICThreadWrapper extends Thread {
		
		public ICThreadWrapper() {
			super(ICThreadWrapper.class.getName());
		}
		
		@Override
		public void run() {
			InnerConsole.this.run();
		}
	}
	
}
