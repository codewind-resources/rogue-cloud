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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Utility methods that can be used to print to the console; the actual console write operation 
 * is passed to InnerConsole, which will write on a separate thread. 
 * 
 * For internal use only. */
public class Console {

	private static final Object sysoutLock = new Object(); 
	
	private static final InnerConsole console = InnerConsole.getInstance();
	
    private static String getTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        return "["+dateFormat.format(calendar.getTime())+"] ";
    }

    public static void printlnWithTime(String str, LogContext context) {
    	
    	if(context != null) {
			str = context.getContext()+str;
		}

		str = getTime() + " " + str;

		// Add the time to the file 
		writeToFile(str, "[out]");

		synchronized(sysoutLock) {
			System.out.println(str);
		}
    	
    }
	
	public static void println(String str, LogContext context) {
		
//		if(addTime) {
//			str = getTime() + " "+	str;
//		}
		
		if(context != null) {
			str = context.getContext()+str;
		}
		writeToFile(str, "[out]");
		
		synchronized(sysoutLock) {
			System.out.println(str);
		}
	}
	
	public static void err(String str, LogContext context) {
		if(context != null) {
			str = context.getContext()+str;
		}
		writeToFile(str, "[err]");
		
		synchronized(sysoutLock) {
			System.err.println(" > "+str);
		}
	}
	
	public static void usermsg(String str, LogContext context) {
		printlnWithTime("* "+str, context);
	}
	
	public static void usererr(String str, LogContext context) {
		err("* "+str, context);
	}
	
	private static void writeToFile(String text, String severity) {
		console.write(severity+" "+text);
	}

}
