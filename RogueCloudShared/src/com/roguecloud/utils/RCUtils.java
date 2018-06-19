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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Various miscellaneous utility methods.
 * 
 * This class is an internal class, for server use only.
 */
public class RCUtils {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getInstance();
	
	public static void safeClose(Writer w) {
		try {
			if(w == null) { return; }
			w.close();
			
		} catch(Exception e) {
			/* ignore*/ 
		}
		
	}
	
	public static void safeClose(OutputStream os) {
		try {
			if(os != null) {
				os.close();
			}
		} catch(Exception e) {
			/* ignore*/
		}
	
	}
	
	public static void safeClose(InputStream is) {
		
		try {
			if(is != null) {
				is.close();
			}
		} catch(Exception e) {
			/* ignore*/
		}
		
	}

	public static List<String> readIntoStringListAndClose(InputStream is) throws IOException {
		
		List<String>  result = new ArrayList<>();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String str;
		while (null != (str = br.readLine())) {
			result.add(str);				
		}
		
		safeClose(is);
		
		return result;
		
	}
	
	public static String readIntoString(InputStream is) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] barr = new byte[64 * 1024];
		int c;
		while (-1 != (c = is.read(barr))) {
			baos.write(barr, 0, c);
		}
		baos.close();
		
		return baos.toString();
		
	}

	public static void sleep(long msecs) {
		try { Thread.sleep(msecs); } catch (InterruptedException e) { throw new RuntimeException(e); }
	}
	
	public static String convertThrowableToString(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}
}
