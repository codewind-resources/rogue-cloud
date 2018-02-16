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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/** A dead simple benchmark of native Java compression algorithms. */
public class CompressTest {

	public static void main(String[] args) throws IOException {
		
		
		Random r = new Random();
		
		String str;
		
		long startTimeInNanos = System.nanoTime();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.BEST_SPEED) );
		
		long stringSize = 0;
		
		int x;
		for(x = 0; x < 1024 *1024*16; x++) {
		
			str = ""+r.nextInt();
			byte[] bytes = str.getBytes();
			stringSize += bytes.length;
			dos.write(bytes);
//			if(x % 1024 * 1024 == 0) {
//				System.out.println("hi: "+x);
//			}
			
		}
		dos.close();
		baos.close();
		
		long compressedSize = baos.size();
		System.out.println("compressedSize: "+compressedSize+" non-compressed: "+stringSize+" ratio: " + ((double)compressedSize/stringSize)   );
		
		long elapsedTime = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTimeInNanos, TimeUnit.NANOSECONDS);
		
		System.out.println(elapsedTime);
		
//		System.out.println(x / (elapsedTime/1000));
		
		
	}
}
