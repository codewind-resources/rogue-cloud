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

package com.roguecloud.client;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import com.roguecloud.RCRuntime;
import com.roguecloud.NG;
import com.roguecloud.client.ClientState;
import com.roguecloud.client.ISessionWrapper;
import com.roguecloud.client.utils.ClientUtil;
import com.roguecloud.client.utils.RCUtilLatencySim;
import com.roguecloud.utils.CompressionUtils;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.ResourceLifecycleUtil;

public class LibertyClientEndpoint extends Endpoint {
	
	private final static Logger log = Logger.getInstance();

	private final ClientState clientState;
	
	private final LibertySessionWrapper sessionWrapper;
	
	private RCUtilLatencySim latencySim;
	
	public LibertyClientEndpoint(LibertySessionWrapper sessionWrapper, ClientState clientState) {
		this.sessionWrapper = sessionWrapper;
		this.clientState = clientState;
		
		if(RCRuntime.ENABLE_LATENCY_SIM) {
			latencySim = new RCUtilLatencySim();
		}
	}
	
	@Override
	public void onOpen(Session session, EndpointConfig ec) {
		log.interesting("Websocket session opened", clientState.getLogContext());
		session.setMaxBinaryMessageBufferSize(128 * 1024);
		session.addMessageHandler(new BinaryMessageHandler(this, session, sessionWrapper));
		
//		session.addMessageHandler(new StringMessageHandler(this, session));
		
		sessionWrapper.newSession(session);
		
		ResourceLifecycleUtil.getInstance().addNewSession(ClientUtil.convertSessionToManagedResource(session));

	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		System.out.println("close. "+closeReason);
		log.interesting("Websocket session closed "+closeReason, clientState.getLogContext());
		
		sessionWrapper.onDisconnect(session);

		ResourceLifecycleUtil.getInstance().removeSession(ClientUtil.convertSessionToManagedResource(session));

	}

	@Override
	public void onError(Session session, Throwable thr) {
		thr.printStackTrace();
		log.severe("An error occured in the client endpoint:", thr, clientState.getLogContext());
	}
	
	public void dispose() {
		if(latencySim != null) {
			latencySim.dispose();
		}
	}
	

	private static class BinaryMessageHandler implements MessageHandler.Whole<byte[]> {
		final LibertyClientEndpoint parent;
		final Session session;
		final ISessionWrapper sessionWrapper;

		public BinaryMessageHandler(LibertyClientEndpoint parent, Session session, ISessionWrapper sessionWrapper) {
			this.parent = parent;
			this.session = session;
			this.sessionWrapper = sessionWrapper;
		}
		
		@Override
		public void onMessage(byte[] b) {
			String str = CompressionUtils.decompressToString(b);
			
			if(NG.ENABLED) { NG.log(RCRuntime.GAME_TICKS.get(), "received in LibertyClientEndpoint: "+str); }
			if(parent.latencySim != null) {
				parent.latencySim.addMessage(sessionWrapper, str, session);
			} else {
				parent.sessionWrapper.receiveJson(str, session);				
			}
		}
		
	}
	
	@SuppressWarnings("unused")
	private static class StringMessageHandler implements MessageHandler.Whole<String> {

		final LibertyClientEndpoint parent;
		final Session session;
		
		public StringMessageHandler(LibertyClientEndpoint parent, Session session) {
			this.parent = parent;
			this.session = session;
		}
		
		@Override
		public void onMessage(String s) {
			parent.sessionWrapper.receiveJson(s, session);
		}
		
	}
}
