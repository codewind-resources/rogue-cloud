/*
 * Copyright 2018, 2019 IBM Corporation
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

package com.roguecloud.resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;

import au.com.codeka.carrot.CarrotEngine;
import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.Configuration;
import au.com.codeka.carrot.bindings.MapBindings;
import au.com.codeka.carrot.resource.FileResourceLocater;
import au.com.codeka.carrot.resource.ResourceLocater;

/** 
 * Singleton which is used to:
 * - generate the HTML page using Carrot template engine, using the templates 'index.html' and 'index-server.html'
 * - validate whether a specific numbered tile exists in the tiles directory
 * - get a tile image from the tiles directory
 **/
public class Resources {

	private static final Resources instance = new Resources();

	private Resources() {
	}

	public static Resources getInstance() {
		return instance;
	}

	// --------------------------------------------------------

	private final Object lock = new Object();

	private File templateDir;

	
	public boolean isValidTile(int tileNum) {
		byte[] barr = null;
		try {
			barr = getFile("tiles/"+tileNum+".png");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return barr != null && barr.length > 0;
		
	}
		
	public byte[] getFile(String path) throws IOException {
		try {
			InputStream is = Resources.class.getResourceAsStream(path);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			readThenWrite(is, baos);
			is.close();
			baos.close();
			return baos.toByteArray();
		} catch(IOException e) {
			System.err.println("Exception for path "+path+" "+e);
			throw e;
		} catch(RuntimeException e) {
			System.err.println("Exception for path "+path+" "+e);
			throw e;
		}

	}

	public String generatePage(Page p, boolean isServer) throws IOException {

		synchronized (lock) {
			if (templateDir == null) {
				templateDir = copyTemplates();
			}
		}

		CarrotEngine engine = new CarrotEngine();
		Configuration config = engine.getConfig();

		ResourceLocater rl = new FileResourceLocater(config, templateDir.getPath());
		config.setResourceLocater(rl);

		Map<String, Object> bindings = new TreeMap<>();
		bindings.put("page", p);

		try {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(baos);

			String htmlPage;
			if(isServer) {
				htmlPage = "index-server.html";
			} else {
				htmlPage = "index.html";
			}
			
			osw.write(engine.process(htmlPage, new MapBindings(bindings)));
			osw.close();

			return baos.toString();

		} catch (CarrotException e) {
			e.printStackTrace();
			throw new RuntimeException(e);

		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	public static String sanitizeUrlPath(String path) {

		path = path.trim();

		while (path.startsWith("/")) {
			path = path.substring(1);
		}

		if (path.contains("..") || path.contains("&") || path.contains("\\") || path.contains("./")
				|| path.contains("/.") || path.contains("\\.") || path.contains(".\\") || path.contains(":")
				|| path.contains("%") || path.startsWith("/") || path.startsWith("\\") || path.contains(" ")) {
			return null;
		}

		if (!(path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".html") || path.endsWith(".svg") || path.endsWith(".mp3"))) {
			return null;
		}

		return path;

	}

	private static File copyTemplates() throws IOException {

		File dir = File.createTempFile("template", "file");
		dir.delete();
		dir.mkdirs();
		dir.deleteOnExit();

		String[] FILES = new String[] { "index.html", "index-server.html" };

		for (String curr : FILES) {
			InputStream is = Resources.class.getResourceAsStream(curr);
			File outputFile = new File(dir, curr);
			FileOutputStream os = new FileOutputStream(outputFile);
			outputFile.deleteOnExit();
			readThenWrite(is, os);
			is.close();
			os.close();
		}

		return dir;
	}

	private static void readThenWrite(InputStream is, OutputStream os) throws IOException {
		byte[] barr = new byte[1024 * 256];
		int c;
		do {
			c = is.read(barr);
			if (c > 0) {
				os.write(barr, 0, c);
			}
		} while (c >= 0);

	}

	/** This object is used by the index.html and index-server.html Carrot templates to generate the HTML pages
	 * which are served to the user as part of the Rogue Cloud browser UI. */
	public static class Page {
		String uuid;
		String username;
		String password;
		String tilesJson;
		
		String websocketUrl;
		String serverWebUrl;

		boolean isViewOnly = false;

		public Page() {
		}

		public void setViewOnly(boolean isViewOnly) {
			this.isViewOnly = isViewOnly;
		}

		public boolean isViewOnly() {
			return isViewOnly;
		}

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getTilesJson() {
			return tilesJson;
		}

		public void setTilesJson(String tilesJson) {
			this.tilesJson = tilesJson;
		}

		public String getWebsocketUrl() {
			return websocketUrl;
		}

		public void setWebsocketUrl(String websocketUrl) {
			this.websocketUrl = websocketUrl;
		}

		public String getServerWebUrl() {
			return serverWebUrl;
		}

		public void setServerWebUrl(String serverWebUrl) {
			this.serverWebUrl = serverWebUrl;
		}

	}

}
