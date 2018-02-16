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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.roguecloud.utils.Logger;

/**
 * Write key/value pairs to a store, which is represented in a single directory. The value is stored
 * in a file, with the name of the file represented by the key. 
 * (Not thread safe) */
public class SafeKeyValueStore {
	
	private static final Logger log = Logger.getInstance();
	
	private static final String CRLF = System.lineSeparator();
	
	private final File directory;
	
	public SafeKeyValueStore(File directory) {
		this.directory = directory;
	}
	
	
	public Optional<Long> readId(String key) {
		
		List<String> resultList = getFileContents(key).orElse(null);
		if(resultList != null) {
			return Optional.of(Long.parseLong(resultList.get(0)));
		}
		
		return Optional.empty();
	}
	
	
	public Optional<String> readStringNoCrlf(String key) {
		List<String> resultList = getFileContents(key).orElse(null);
		if(resultList != null) {
			return Optional.of(resultList.get(0));
		}
		
		return Optional.empty();
		
	}
	
	private Optional<List<String>> getFileContents(String key) {
		List<File> filesFound = new ArrayList<>();
				
		// Find files that were properly written to
		File[] files = directory.listFiles();
		if(files != null) {
			for(File f : files) {
				String name = f.getName();
				if(name.startsWith(key+"-")) {
					
					// A proper file container two lines: the value, and then END
					// the lack of END indicates the file did not complete writing.
					List<String> result = readFullFileContents(f);
					if(result.size() == 2 && result.get(1).equalsIgnoreCase("end")) {
						filesFound.add(f);
					}
				}
				
			}
			
		}
		
		// Sort files by descending order; newer files will be first in the list
		Collections.sort(filesFound, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				return o2.getName().compareTo(o1.getName());
			}
			
		});
		
		if(filesFound.size() > 0) {
			// Return the contents of the first (newest) file
			File resultFile = filesFound.get(0);
			
			List<String> resultList = readFullFileContents(resultFile);
			
			return Optional.of(resultList);
			
		} else {
			return Optional.empty();
		}
		
	}
	
	public void writeId(String id, long idToWrite) {
		write(id, Long.toString(idToWrite));
	}
	
	public void writeStringNoCrlf(String id, String val) {
		write(id, val);
	}
	
	
	private void write(String id, String value) {
		if(value.contains("\r") || value.contains("\n")) {
			throw new IllegalArgumentException("Value contains CRLF");
		}
		
		File outFile;
		
		// Write the id
		try {
			outFile = new File(directory, id+"-"+System.currentTimeMillis());
			outFile.getParentFile().mkdirs();
			FileOutputStream fos = new FileOutputStream(outFile);
			fos.write((value+CRLF+"END"+CRLF).getBytes());
			fos.flush();
			fos.getFD().sync();
			fos.close();
		} catch(IOException e) {
			log.severe("Unable to write "+id+" val: "+value, null);
			throw new RuntimeException(e);
		}
		
		
		List<File> filesFound = new ArrayList<>();
		
		// Delete ones older than 5 minutes
		File[] files = directory.listFiles();
		if(files != null) {
			for(File f : files) {
				String name = f.getName();
				if(name.startsWith(id+"-")) {
					filesFound.add(f);
				}
				
			}
			
		}
		
		long expireTimeInMsecs = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
		if(filesFound.size() > 1) {
			for(File f : filesFound) {
				String name = f.getName();
				long time = Long.parseLong(name.substring(name.lastIndexOf("-")+1));
				if(time <  expireTimeInMsecs) {
					boolean result = f.delete();
					if(!result || f.exists()) {
						log.severe("Unable to delete file: " +name, null);
						throw new RuntimeException("Unable to delete file "+name);
					}
				}
			}
			
		}
		
		
	}
	
	private static List<String> readFullFileContents(File f) {
		List<String> result = new ArrayList<>();
		BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				
				String str;
				while(null != (str = br.readLine())) {
					result.add(str);
				}

			} catch (IOException e) {
				log.severe("IOException in readFileContents: " +f.getPath(), null);
				throw new RuntimeException(e);
			} finally {
				if(br != null) {
					try { br.close(); } catch (IOException e) { /* ignore */ }
				}
			}
		
		return result;
	}


}
