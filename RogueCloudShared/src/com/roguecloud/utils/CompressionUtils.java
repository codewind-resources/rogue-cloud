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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.roguecloud.RCRuntime;

/** For internal server use only: This class provides simple deflate compression/decompression of Strings, 
 * with the compression tuned for high CPU throughput. 
 * 
 * You can disable compression setting RCRuntime.ENABLE_DEFLATE_COMPRESSION to false, but you must ensure that this value is
 * set to false on both the client and server.
 * */
public final class CompressionUtils {

	private static final Logger log = Logger.getInstance();
	
	public static final ByteBuffer compressToByteBuffer(String str) {
		return ByteBuffer.wrap(compressString(str));
	}
	
	public static final String decompressToString(byte[] bytes) {
		return decompressString(bytes);
	}
	
	private final static byte[] compressString(String str) {

		// If compression is disabled, then just return a byte array of a UTF-8 string
		if(!RCRuntime.ENABLE_DEFLATE_COMPRESSION) {
			return str.getBytes();
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.BEST_SPEED) );
		
		try {
			dos.write(str.getBytes());
			dos.close();
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Exception on compress", e, null);
			throw new RuntimeException(e);
		}
		
		return baos.toByteArray();
	}

	
	private final static String decompressString(byte[] str) {
		
		// If compression is disabled, then the byte array is just a UTF-8 string
		if(!RCRuntime.ENABLE_DEFLATE_COMPRESSION) {
			return new String(str);
		}

		
		InflaterInputStream dis = new InflaterInputStream(new ByteArrayInputStream(str));
		
		try {
			return readIntoString(dis);
		} catch (IOException e) {
			e.printStackTrace();
			log.severe("Error on decompress",  e, null);
			throw new RuntimeException(e);
		}
	}
	
	private static String readIntoString(InputStream is) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] barr = new byte[64 * 1024];
		int c;
		while (-1 != (c = is.read(barr))) {
			baos.write(barr, 0, c);
		}
		baos.close();
		
		return baos.toString();
		
	}
		

}
