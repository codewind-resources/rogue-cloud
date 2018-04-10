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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/** Utility to generate an untrusted JAX-RS client */
public class HttpClientUtils {
	
	public static Client generateJaxRsHttpClient() {
		
		try {
			SSLContext sslcontext = SSLContext.getInstance("TLS");
			sslcontext.init(null, new TrustManager[]{new X509TrustManager() {
			        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
			        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
			        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }

			    }}, new java.security.SecureRandom());
			
			Client client = ClientBuilder.newBuilder().sslContext(sslcontext).hostnameVerifier(new HNV()).build();
			return client;

		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return null;
		 
	}

	/** We trust all host names, regardless of certificate. Don't do this in production :P */
	private static class HNV implements HostnameVerifier {

		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
		
	}

}
