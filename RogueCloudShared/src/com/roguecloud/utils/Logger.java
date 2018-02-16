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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.roguecloud.RCRuntime;

/** 
 * For internal use only.
 * 
 * Utility class for standardized logging to console/file; this class is a thread-safe singleton.
 *
 * To log, add to class:
 * 		private static final Logger log = Logger.getInstance();
 * 
 * then:
 * 		log.[err/severe/info/interesting] (...)
 * 
 **/
public class Logger {
	
	public static final boolean CLIENT_SENT = true;
	public static final boolean CLIENT_RECEIVED = true;
	

	private Logger() {
	}	
	private static final Logger logger = new Logger();
	
	// -------
	
	private final Object lock = new Object();
	
	
	private final Level currentLogLevel = Level.INTERESTING;
	
	private final LoggerCache lc = new LoggerCache();
	
	
	public static Logger getInstance() {
		return logger;
	}
	
	private static String gameTicks() {
		long ticks = RCRuntime.GAME_TICKS.get();
		if(ticks > 0) {
			return "["+ticks+"t] ";
		}
		return "";
	}
	
	public void severe(String str, LogContext context) {
		
		synchronized(lock) {
			LoggerCache.ReturnVal rv = lc.shouldPrint(str);
			if(rv.print) {
				String throttled = rv.isThrottled ? "[throttled] " : "";
				Console.err("[sever] "+gameTicks()+getTime()+throttled+str, context);
			}

		}
	}
	

	public void severe(String str, Throwable t, LogContext context) {
		synchronized(lock) {
			LoggerCache.ReturnVal rv = lc.shouldPrint(str);

			if(rv.print) {
				String throttled = rv.isThrottled ? "[throttled] " : "";
				Console.err("[sever] "+gameTicks()+getTime()+throttled+str+"\n Exception:"+convertStackTrace(t), context);
				t.printStackTrace();
			}
		}

	}
	

	
	public void err(String str, LogContext context) {
		if(currentLogLevel.getValue() < Level.ERROR.getValue()) {
			return;
		}
		
		synchronized(lock) {			

			LoggerCache.ReturnVal rv = lc.shouldPrint(str);
		
			if(rv.print) {
				String throttled = rv.isThrottled ? "[throttled] " : "";
				Console.err("[error] "+gameTicks()+getTime()+throttled+str, context);
			}

		}
	}

	public void err(String str, Throwable t, LogContext context) {
		if(currentLogLevel.getValue() < Level.ERROR.getValue()) {
			return;
		}

		synchronized(lock) {

			LoggerCache.ReturnVal rv = lc.shouldPrint(str);

			if(rv.print) {
				String throttled = rv.isThrottled ? "[throttled] " : "";
				Console.err("[error] "+gameTicks()+getTime()+throttled+str+" Exception:"+convertStackTrace(t), context);
				t.printStackTrace();
			}
		}
	}

	public void interesting(String str, LogContext context) {
		if(currentLogLevel.getValue() < Level.INTERESTING.getValue()) {
			return;
		}
		
		synchronized(lock) {
			LoggerCache.ReturnVal rv = lc.shouldPrint(str);
			
			if(rv.print) {
				String throttled = rv.isThrottled ? "[throttled] " : "";
				Console.println("[inter] "+gameTicks()+getTime()+throttled+str, context);
			}
		}
		
	}
	
	public void infoWithStackTrace(String str, LogContext context) {
		if(currentLogLevel.getValue() < Level.INFO.getValue()) {
			return;
		}
		
		synchronized(lock) {
			LoggerCache.ReturnVal rv = lc.shouldPrint(str);
			
			if(rv.print) {
				String throttled = rv.isThrottled ? "[throttled] " : "";
				Console.println("[info] "+gameTicks()+getTime()+throttled+str+" Exception: "+convertStackTrace(new Throwable()), context);
			}
		}
	}

	
	public void info(String str, LogContext context) {
		if(currentLogLevel.getValue() < Level.INFO.getValue()) {
			return;
		}
		
		synchronized(lock) {
			LoggerCache.ReturnVal rv = lc.shouldPrint(str);
			
			if(rv.print) {
				String throttled = rv.isThrottled ? "[throttled] " : "";
				Console.println("[info] "+gameTicks()+getTime()+throttled+str, context);
			}
		}
	}
	
	public void info(String str, Exception e, LogContext context) {
		if(currentLogLevel.getValue() < Level.INFO.getValue()) {
			return;
		}
		
		synchronized(lock) {
			LoggerCache.ReturnVal rv = lc.shouldPrint(str);
			
			if(rv.print) {
				String throttled = rv.isThrottled ? "[throttled] " : "";
				Console.println("[info] "+gameTicks()+getTime()+throttled+str+" Exception: "+convertStackTrace(e), context);
			}
		}
	}

	

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-d HH:mm:ss.SSS", Locale.US);

	
    private static String getTime() {
        Calendar calendar = Calendar.getInstance();
        return "["+dateFormat.format(calendar.getTime())+"] ";
    }

	
	public static enum Level {
	    DISABLED(0), 
	    SEVERE(1), // Implies a bug in the code itself (eg a design/implementation issue, eg conditions that "should never occur"), or a serious user configuration error  
	    ERROR(2), 
	    INTERESTING(3), 
	    INFO(4);
	    private final int value;

	    private Level(int value) {
	        this.value = value;
	    }

	    public int getValue() {
	        return value;
	    }
	}


	private static String convertStackTrace(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		
		e.printStackTrace(pw);
		pw.flush();
		sw.flush(); 

		return sw.toString();
	}

	
	/** Synchronize on lock before calling any of these methods. */
	class LoggerCache {
		
		private final int MAX_LAST_MSGS_SIZE = 500;
		
		private final List<LoggerCacheEntry> lastMessages = new ArrayList<>();
		
		private final Map<String, LoggerCacheEntry> cacheMap = new HashMap<>();
		
		private void removeOld() {
			List<LoggerCacheEntry> removed = new ArrayList<>();
			
			long expireTime = System.nanoTime() - TimeUnit.NANOSECONDS.convert(30, TimeUnit.SECONDS);
			
			for(Iterator<LoggerCacheEntry> it = lastMessages.iterator(); it.hasNext();) {
				LoggerCacheEntry lce = it.next();
				if(lce.timeSinceLastOutput != null && lce.timeSinceLastOutput < expireTime) {
					removed.add(lce);
					it.remove();
				}
			}
			
			for(LoggerCacheEntry lce : removed) {
				cacheMap.remove(lce.msg);
			}
			
		}
		
		private void removeTooManyIfNeeded() {
			
			if(lastMessages.size() > MAX_LAST_MSGS_SIZE) {
				
				List<LoggerCacheEntry> removed = new ArrayList<>();

				while(lastMessages.size() > MAX_LAST_MSGS_SIZE)  {
					LoggerCacheEntry lce = lastMessages.remove(lastMessages.size()-1);
					removed.add(lce);
				}
				
								
				for(LoggerCacheEntry lce : removed) {
					cacheMap.remove(lce.msg);
				}
				
			}
		}
		
		public ReturnVal shouldPrint(String str) {
			removeOld();
			
			LoggerCacheEntry e = cacheMap.get(str);
			if(e == null) {
				e = new LoggerCacheEntry();
				e.msg = str;
				
//				e.timeSinceFirstOutput = System.nanoTime();
				e.timeSinceLastOutput = System.nanoTime();
				lastMessages.add(0, e);
				cacheMap.put(str, e);
				
				removeTooManyIfNeeded();
				
				return new ReturnVal(true, false);
			} else {
				
				e.timeSinceLastOutput = System.nanoTime();
				e.timesSeen++;
				
				Collections.sort(lastMessages);

				if(e.timesSeen > 30) {
					
					if(!e.throttling) {
						e.throttling = true;
						e.timeSinceThrottling = System.nanoTime();
					}					
				}
				
				if(e.throttling) {
					long timeElapsedSinceFirstThrottlingInMsecs = TimeUnit.MILLISECONDS.convert(System.nanoTime() - e.timeSinceThrottling, TimeUnit.NANOSECONDS);
					
					double messagesWrittenPerSecond = (double)e.timesWrittenSinceFirstThrottling / (double)(timeElapsedSinceFirstThrottlingInMsecs / 1000d); 
					
					if(messagesWrittenPerSecond <= 2) {
						e.timesWrittenSinceFirstThrottling++;
						return new ReturnVal(true, true);
					} 
					
					return new ReturnVal(false, true);
					
				} 

				return new ReturnVal(true, false);
			}
			
		}

		/** Simple internal-only return value of shouldPrint(). */
		private class ReturnVal {
			boolean print = false;
			boolean isThrottled = false;
			
			public ReturnVal(boolean print, boolean isThrottled) {
				this.print = print;
				this.isThrottled = isThrottled;
			}
		}

		/** We cache log messages based on when they were last seen, how many times seen, etc, to allow us
		 * to throttle messages to keep from overflowing the log file. */
		private class LoggerCacheEntry implements Comparable<LoggerCacheEntry> {
			
			String msg;
			
//			Long timeSinceFirstOutput = 0l;
			
			Long timeSinceLastOutput = 0l;
			
			Long timeSinceThrottling = 0l;
			
			int timesSeen = 0;
			
			int timesWrittenSinceFirstThrottling = 0;
			
			boolean throttling = false;

			@Override
			public int compareTo(LoggerCacheEntry o) {
				// Descending order
				long l = o.timeSinceLastOutput - timeSinceLastOutput;
				
				if(l > 1) { return 1;}
				else if(l == 0) { return 0; }
				else {
					return -1;
				}
				
			}
			
		}
			
	}

}
