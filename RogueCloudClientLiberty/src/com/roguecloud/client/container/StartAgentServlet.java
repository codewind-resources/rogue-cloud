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

package com.roguecloud.client.container;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;

import com.roguecloud.NG;
import com.roguecloud.client.ClientMappingSingleton;
import com.roguecloud.client.ClientState;
import com.roguecloud.client.LibertyWebsocketFactory;
import com.roguecloud.client.RemoteClient;
import com.roguecloud.client.ai.SimpleAI;
import com.roguecloud.client.utils.ClientUtil;
import com.roguecloud.resources.Resources;
import com.roguecloud.resources.Resources.Page;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.RegisterUser;

@WebServlet("/StartAgent")
public class StartAgentServlet extends HttpServlet {

	private static final Logger log = Logger.getInstance();
	
	private static final long serialVersionUID = 1L;
    
	private static AtomicBoolean agentStarted = new AtomicBoolean(false);
	
	private static String lastError = null;
	
	// ----------------------------------------------------

	// Specify your username and password. These will be automatically registered when you first connect
	// to the game server.
	
	public static final String USERNAME = "your-username";
	public static final String PASSWORD = "your-password";
	
	private static RemoteClient constructMyAI() {
		return new SimpleAI();
 
	}

	// ----------------------------------------------------
	
	public static final String SERVER_HOST_AND_PATH_NON_URL = "roguecloud.space:29080/RogueCloudServer";
//	public static final String SERVER_HOST_AND_PATH_NON_URL = "localhost:29080/RogueCloudServer";

	public static final String SERVER_URL = "http://"+SERVER_HOST_AND_PATH_NON_URL;
	public static final String CLIENT_API_URL = "ws://"+SERVER_HOST_AND_PATH_NON_URL+"/api/client";
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public StartAgentServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String uuid = ClientUtil.getOrCreateClientUuid();

		atomicAgentStart(uuid);
		
		if(lastError != null) {
			response.setStatus(400);
			response.getWriter().println(lastError);
			return;
		}
		
		String username = USERNAME;
		String password = PASSWORD;
		
		if(username == null || password == null || uuid == null) {
			response.setStatus(400);
			response.getWriter().println("Missing username, password, or UUID fields.");
			return;
		}
		
		Page p = new Page();
		p.setUsername(username);
		p.setPassword(password);
		p.setUuid(uuid);
		p.setTilesJson(ClientContainerUtil.getTileListJson(SERVER_URL));  
		p.setViewOnly(false);
		p.setServerWebUrl(SERVER_URL);
		
		String pageStr = Resources.getInstance().generatePage(p, false);
		
		response.getWriter().append(pageStr);
		
	}
	
	public static void atomicAgentStart(String uuid) throws IOException {

		boolean startAgent = false;
		synchronized (agentStarted) {
			boolean isAgentStarted = agentStarted.get();
			if(!isAgentStarted) {
				agentStarted.set(true);
				startAgent = true;
				
			}
		}
		
		if(startAgent) {
			
			// Start the agent!
			try {
				agentStart(uuid);
			} catch (DeploymentException | URISyntaxException | InterruptedException e) {
				throw new RuntimeException(e);
			}

		}

	}
	
	private static void agentStart(String uuid) throws DeploymentException, IOException, URISyntaxException, InterruptedException {
		
		NG.getInstance().setWhoami("client");

		if(!isValidatedUsernameAndPassword(USERNAME, PASSWORD)) {
			return;
		}
		
		String username = USERNAME;
		String password = PASSWORD;

		long id;
		try {
			id = RegisterUser.registerAndGetUserId(username, password,  SERVER_URL );
		} catch(Exception e) {
			lastError = e.getMessage();
			ClientContainerUtil.loudlyInformUser(e.getMessage());
			throw e;
		}
		
		Integer val = null;
		
		try {
			val = (Integer) new InitialContext().lookup("rc_server_port");
		} catch (NamingException e1) {
			/* ignore */
		}
		
		if(val == null || val <= 0) {
			val = 9080;
		}
		
		System.out.println("***********************************************************************************************");
		System.out.println("*                                                                                             *");
		System.out.println("*    Agent has started. Watch at: http://localhost:"+val+"/RogueCloudClientLiberty/StartAgent   *");
		System.out.println("*                                                                                             *");
		System.out.println("***********************************************************************************************");
		System.out.println();
		
		doInitialConnect(username, password, uuid);
		
	}	
	
	private static void doInitialConnect(String username, String password, String uuid) throws DeploymentException, IOException, URISyntaxException, InterruptedException {

		Thread t = new Thread() {
			public void run() {
				
				ConnectData data = new ConnectData();
				
				while(true) {
					try {
						doInitialConnectInner(username, password, uuid, data, constructMyAI());
					} catch (Exception e) {
						log.err("Error in connection loop", e, null);
						e.printStackTrace();
					} finally {
						try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
					}
				}
				
			}
		};
		t.start();
		
	}
	
	private static void doInitialConnectInner(String username, String password, String uuid, ConnectData data, RemoteClient remoteClient) throws DeploymentException, IOException, URISyntaxException, InterruptedException {

		ClientState state = new ClientState(username, password, uuid, remoteClient, new LibertyWebsocketFactory(), data.numberOfTimesInterupted);
		ClientMappingSingleton.getInstance().putClientState(uuid, state);
		remoteClient.setClientState(state);
		
		state.initialConnect(CLIENT_API_URL);
		
		while(!state.isRoundComplete() && !state.isClientInterrupted()) {
			Thread.sleep(100);
		}

		if(state.isRoundComplete()) {
			data.roundComplete = true;
			data.numberOfTimesInterupted = 0;
			System.err.println("Round is over, waiting "+state.getNextRoundInXSeconds()+" seconds.");
			TimeUnit.SECONDS.sleep(state.getNextRoundInXSeconds()+2);
		}
		
		if(state.isClientInterrupted()) {
			System.err.println("Client was interrupted -- restarting.");
			data.roundComplete = false;
			data.numberOfTimesInterupted++;
		}
		
		ClientMappingSingleton.getInstance().removeClientState(uuid);
		
		// Dispose on a separate thread
		new Thread() {
			public void run() {
				state.dispose();
			};
		}.start();

	}
	
	private static boolean isValidatedUsernameAndPassword(String username, String password) {
		
		String error = null;
		if(username.trim().isEmpty() || password.trim().isEmpty()) {
			error = "Error: Username or password is empty. Username and password must have at least one character.";
		}
		
		if(username.equals("your-username") && password.equals("your-password")) {
			error = "Error: Change your username and password from the default in StartAgentServlet!";
		}

		if(error != null) {
			ClientContainerUtil.loudlyInformUser(error); 
			lastError = error;
		}
		
		return error == null;

	}
	
	private static class ConnectData {
		
		@SuppressWarnings("unused")
		boolean roundComplete = false;
		
		int numberOfTimesInterupted = 0;
	}

}
