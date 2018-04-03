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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonBuilderFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roguecloud.Combat.CombatResult;
import com.roguecloud.UniqueIdGenerator.IdType;
import com.roguecloud.WorldGenFromFile.RoomSpawn;
import com.roguecloud.WorldGeneration.AIHint;
import com.roguecloud.WorldGeneration.GenerateWorldResult;
import com.roguecloud.WorldGeneration.WorldGenAIType;
import com.roguecloud.actions.CombatAction;
import com.roguecloud.actions.CombatActionResponse;
import com.roguecloud.actions.CombatActionResponse.CombatActionResult;
import com.roguecloud.actions.DrinkItemAction;
import com.roguecloud.actions.DrinkItemActionResponse;
import com.roguecloud.actions.EquipAction;
import com.roguecloud.actions.EquipActionResponse;
import com.roguecloud.actions.IAction;
import com.roguecloud.actions.IAction.ActionType;
import com.roguecloud.actions.IActionResponse;
import com.roguecloud.actions.MoveInventoryItemAction;
import com.roguecloud.actions.MoveInventoryItemActionResponse;
import com.roguecloud.actions.NullAction;
import com.roguecloud.actions.NullActionResponse;
import com.roguecloud.actions.StepAction;
import com.roguecloud.actions.StepActionResponse;
import com.roguecloud.actions.StepActionResponse.StepActionFailReason;
import com.roguecloud.ai.WanderingAttackAllAI;
import com.roguecloud.client.EventLog;
import com.roguecloud.client.MonsterClient;
import com.roguecloud.client.SelfState;
import com.roguecloud.client.WorldState;
import com.roguecloud.creatures.ICreature;
import com.roguecloud.creatures.IMutableCreature;
import com.roguecloud.creatures.Monster;
import com.roguecloud.creatures.MonsterTemplateList;
import com.roguecloud.creatures.PlayerCreature;
import com.roguecloud.db.DatabaseInstance;
import com.roguecloud.db.DbLeaderboardEntry;
import com.roguecloud.db.IDatabase;
import com.roguecloud.events.CombatActionEvent;
import com.roguecloud.events.DrinkItemActionEvent;
import com.roguecloud.events.EquipActionEvent;
import com.roguecloud.events.IEvent;
import com.roguecloud.events.IEvent.EventType;
import com.roguecloud.events.IMutableEvent;
import com.roguecloud.events.MoveInventoryItemActionEvent;
import com.roguecloud.events.StepActionEvent;
import com.roguecloud.items.Armour;
import com.roguecloud.items.ArmourList;
import com.roguecloud.items.ArmourSet;
import com.roguecloud.items.DrinkableItem;
import com.roguecloud.items.Effect;
import com.roguecloud.items.Effect.EffectType;
import com.roguecloud.items.GroundObject;
import com.roguecloud.items.IGroundObject;
import com.roguecloud.items.IObject;
import com.roguecloud.items.IObject.ObjectType;
import com.roguecloud.items.OwnableObject;
import com.roguecloud.items.Weapon;
import com.roguecloud.items.WeaponList;
import com.roguecloud.json.JsonAbstractTypedMessage;
import com.roguecloud.json.JsonActionMessageResponse;
import com.roguecloud.json.JsonClientInterrupt;
import com.roguecloud.json.JsonFrameUpdate;
import com.roguecloud.json.JsonRoundComplete;
import com.roguecloud.json.JsonSelfState;
import com.roguecloud.json.JsonWorldState;
import com.roguecloud.json.actions.JsonCombatAction;
import com.roguecloud.json.actions.JsonDrinkItemAction;
import com.roguecloud.json.actions.JsonEquipAction;
import com.roguecloud.json.actions.JsonMoveInventoryItemAction;
import com.roguecloud.json.actions.JsonNullAction;
import com.roguecloud.json.actions.JsonNullActionResponse;
import com.roguecloud.json.actions.JsonStepAction;
import com.roguecloud.json.browser.JsonActiveRoundInfo;
import com.roguecloud.json.browser.JsonInactiveRoundInfo;
import com.roguecloud.json.browser.JsonUpdateBrowserUI;
import com.roguecloud.json.browser.JsonUpdateBrowserUI.JsonBrowserCombatEvent;
import com.roguecloud.json.browser.JsonUpdateBrowserUI.JsonScore;
import com.roguecloud.json.browser.JsonUpdateBrowserUI.JsonServiceStatEntry;
import com.roguecloud.json.client.JsonHealthCheck;
import com.roguecloud.map.IMap;
import com.roguecloud.map.IMutableMap;
import com.roguecloud.map.RCArrayMap;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;
import com.roguecloud.map.TileTypeList;
import com.roguecloud.server.ActiveWSClient;
import com.roguecloud.server.ActiveWSClientList;
import com.roguecloud.server.ActiveWSClient.ReceivedAction;
import com.roguecloud.server.ActiveWSClient.ViewType;
import com.roguecloud.server.ActiveWSClientSession;
import com.roguecloud.server.ActiveWSClientSession.Type;
import com.roguecloud.utils.AIUtils;
import com.roguecloud.utils.LogContext;
import com.roguecloud.utils.Logger;
import com.roguecloud.utils.MonsterMachine;
import com.roguecloud.utils.MonsterRuntimeStats;
import com.roguecloud.utils.MonsterRuntimeStats.MonsterRuntimeEntry;
import com.roguecloud.utils.RoomList;

public final class GameEngine {

	public static final String BARE_HANDS_DEFAULT_WEAPON = "Bare Hands";
	
	private final GameThread gameThread;
	
	private final ServerInstance parent;
	
	private final LogContext lc;
	
	private static final Logger log = Logger.getInstance();
	
	private final RoundScope roundScope;
	
	public GameEngine(RoundScope roundScope, ServerInstance parent, GameContext gc)  {
		this.roundScope = roundScope;
		this.lc = LogContext.serverInstance(parent.getId());
		
		GenerateWorldResult gwr = WorldGeneration.generateDefaultWorld(parent, gc.generator, lc);
		
		gc.roomSpawns = Collections.unmodifiableList(gwr.getRoomSpawns());
		
		gc.map = gwr.getNewMap();
		
		gwr.getNewGroundObjects().forEach( go -> {
			gc.groundObjects.put(go.getId(), go);
		});
		
		gwr.getNewMonsters().forEach( e -> {
			gc.getOrCreateCreatureEntry(e);
		});
		
		gwr.getAIContextList().forEach( e -> {
			gc.monsterToAIMap.put(e.getCreatureId(), e);
		});

		MonsterRuntimeStats monsterStats = new MonsterRuntimeStats();
		gc.monsterStats = monsterStats;
		
		gc.monsterMachine = new MonsterMachine(roundScope, monsterStats, lc);
		
		this.gameThread = new GameThread(parent.getId(), gc);

		this.parent = parent;
		
	}
	
	public void startThread() {
		this.gameThread.start();
	}

	private void gameThreadRun(GameContext gc) throws IOException {
	
		waitForActiveClient(roundScope);
		roundScope.beginAbsoluteRoundEndTimeInNanos();
		
		boolean continueLoop = true;
		// Game loop
		while(continueLoop) {
			
			try {
				continueLoop = gameLoop(gc.map, gc);				
			} catch(Throwable t) {
				// Under no circumstances do we escape the precious game loop
				log.severe("Game Loop exception", t, lc);
				t.printStackTrace();				
			}
		}
		
		parent.startNewRound();
		
		// Dispose of the gc in a new thread.
		new Thread() { 
			@Override
			public void run() {
				gc.dispose();
			}
		}.start();
		
	}
	
	private long elapsedNanosSinceReset = 0;
	
	private boolean gameLoop(RCArrayMap map, GameContext gc) throws InterruptedException {
		
		if(gc.roundScope.getCurrentRoundEndInNanos() == null) {
			String MSG = "The game loop started with no round end in nanos, which should not happen.";
			log.severe(MSG, null);
			throw new RuntimeException(MSG);
		}
		
		boolean continueGameThread = true;
		
		GameLoopPerf perf = gc.glPerf;
		
		// When a new player is added, they are added at a certain position on the map. But we need to communicate this to all players
		// after the status phase. So we include the position here, and it is added to changedTiles after the status phase.
		List<Position> changedTilesNewPlayers = new ArrayList<>();
		
		List<Position> changedTiles = gc.changedTiles;
		
		gc.ticks++;
		RCRuntime.GAME_TICKS.set(gc.ticks);

		final int agentClientWidth = RCConstants.AGENT_CLIENT_VIEW_WIDTH;
		final int agentClientHeight = RCConstants.AGENT_CLIENT_VIEW_HEIGHT;

		final int serverFollowWidth = RCConstants.SERVER_FOLLOW_WIDTH;
		final int serverFollowHeight = RCConstants.SERVER_FOLLOW_HEIGHT;
		
		ObjectMapper om = new ObjectMapper();

		// Create the browser update JSON object, based on previous events
		try {
			perf.startSection();
			
			List<IEvent> evListPrevFrame = gc.eventsPreviousFrames.getList(gc.ticks-1);
			
			// Create a mapping of the current active players to their user name
			Map<Long /*user id*/, String /* user name */> localUserIdToUserNameMap = new HashMap<>();
			for(ActiveWSClient client : roundScope.getActiveClients().getList()) {
				
				try {
					localUserIdToUserNameMap.put(client.getUserId(), client.getUsername());
					
				} catch(Exception e) {
					log.severe("Exception occured on player mapping", e, null);
					e.printStackTrace();
				}
			}
			
			// Every 50 ticks: Create sorted score list for current round			
			HashMap<Long /* player id */, JsonScore> idtoScoreMap = null;
			List<JsonScore> scores = null;
			if(gc.ticks % 50 == 0) { // Update every 50 ticks
				idtoScoreMap = new HashMap<>();
				scores = new ArrayList<>();
				
				for(Entry<Long, Long> e : gc.scoreMap.entrySet()) {
					JsonScore s = new JsonScore();
					s.setScore(e.getValue());
					String username = localUserIdToUserNameMap.get(e.getKey());
					s.setUsername(username);
					s.setRank(0);
					if(username != null) {
						scores.add(s);
						idtoScoreMap.put(e.getKey(), s);
					} else {
						log.severe("Could not locate username for "+e.getKey(), lc);
					}					
				}
				
				// Sort descending by score
				Collections.sort(scores, new Comparator<JsonScore>() {
					@Override
					public int compare(JsonScore o1, JsonScore o2) {
						return (int)(o2.getScore() - o1.getScore());
					}
				});
				
				for(int x = 0; x < scores.size(); x++) {
					scores.get(x).setRank(x+1);
				}
			}
			
			// Every 50 ticks: Update metrics
			List<JsonServiceStatEntry> statsList = null;
			if(gc.ticks % 50 == 25 && !gc.isRoundOver) {
				
				statsList = new ArrayList<>();
				
				for(ActiveWSClient client : roundScope.getActiveClients().getList()) {
					try {
					
						EngineWebsocketState ews = (EngineWebsocketState) client.getGameEngineInfo();
						if(ews == null || ews.getPlayerCreature() == null) { continue; }
					
						JsonServiceStatEntry sse = new JsonServiceStatEntry();
						
						IMutableCreature creature = ews.getPlayerCreature();
						CreatureEntry ce = gc.getOrCreateCreatureEntry(creature);
						
						ClientStatistics clientStats = ce.clientStats;
						
						if(clientStats.getStartTimeInNanos() > 0) {
							long elapsedTimeInMsecs = TimeUnit.MILLISECONDS.convert(System.nanoTime() - clientStats.getStartTimeInNanos(), TimeUnit.NANOSECONDS);
							sse.setUsername(client.getUsername());
							
							double actionsPerSecond = ((int)((double)clientStats.getActionsReceived() / ((double)elapsedTimeInMsecs/1000d)*10))/10d;
							sse.setActionsPerSecond(  actionsPerSecond  );
							if(clientStats.getActionsReceived() != 0) {
								sse.setAverageTimeBetweenActions(elapsedTimeInMsecs/clientStats.getActionsReceived());
							}
							sse.setNumberOfTimesDied(clientStats.getTimesDied());
							sse.setPassLastHealthCheck(clientStats.isPassedPreviousHealthCheck());
							statsList.add(sse);
						}
					
					} catch(Exception e) {
						log.severe("Exception in browser UI update",  e, lc);
					}
				}
				
				statsList.sort( (a, b) -> { return (int)(b.getActionsPerSecond() - a.getActionsPerSecond());});
			}
			
			for(ActiveWSClient client : roundScope.getActiveClients().getList()) {
			
				try {
					generateUpdateBrowserUI(client, evListPrevFrame, idtoScoreMap, scores, localUserIdToUserNameMap, statsList, om, gc);
				} catch(Exception e) {
					log.severe("Exception in browser ui update code", e, lc);
				}
			}
			
			perf.endSection(GameLoopPerf.Section.BROWSER_UI_UPDATE);
		} catch(Exception e) {
			e.printStackTrace();
			log.severe("Exception in browser UI update section of game loop", e, lc);
		}
		
		// Send world state to each agent connection
		{
			perf.startSection();
			
			// Remove duplicate changed tiles
			{
				Map<Position, Boolean> coordsSeen = new HashMap<Position, Boolean>();
				for(Iterator<Position> it = gc.changedTiles.iterator(); it.hasNext();) {
					Position p = it.next();
					if(coordsSeen.containsKey(p)) {
						it.remove();
					} else {
						coordsSeen.put(p, true);
					}
				}
			}
			
			for(ActiveWSClient client : roundScope.getActiveClients().getList()) {
				try {
					
					ActiveWSClientSession clientSession = client.getMostRecentSessionOfType(Type.CLIENT);
					
					if(clientSession == null) { continue; }
					
					if(!gc.isRoundOver) {
						generateAndSendFrameUpdate(clientSession, client, agentClientWidth, agentClientHeight, gc, changedTilesNewPlayers);
						
					} else {
						int nextRoundStartInSeconds = (int) TimeUnit.SECONDS.convert(gc.roundScope.getNextRoundStartInNanos() - System.nanoTime(), TimeUnit.NANOSECONDS );
						JsonRoundComplete jrc = new JsonRoundComplete(nextRoundStartInSeconds);
						clientSession.writeToClientAsync(om.writeValueAsString(jrc));
					}
				
				} catch(Exception e) {
					// Prevent client failures from impacting the for loop
					e.printStackTrace();
					log.severe("Exception in sending world state to agent", e, lc);
					
				}
				
			} // end send world state for loop

			perf.endSection(GameLoopPerf.Section.WORLD_STATE_TO_EACH_AGENT);
		}
		
		// Find a creature to follow
		Position viewOnlyClientPosNew;
		try {
			IMutableCreature lastMonster = gc.monsterToFollow;
			
			boolean isMonsterDeadButStillVisible = false;
			
			// Is the monster we are currently following DEAD?
			if(lastMonster != null) {
				CreatureEntry ce = gc.getOrCreateCreatureEntry(lastMonster);
				Long deathTick;
				if(lastMonster.isPlayerCreature())  { 
					deathTick = ce.playerTickOfDeath;
				} else {
					deathTick = ce.monsterTickOfDeath; // gc.mapCreatureToDeathGameTick.get(lastMonster.getId());
				}
				
				if(deathTick != null) {
					isMonsterDeadButStillVisible = true;
					if(gc.ticks - deathTick > RCConstants.LINGER_ON_DEAD_CREATURE_FOR_X_TICKS) { 
						lastMonster = null;	
						isMonsterDeadButStillVisible = false;
					} 
				}
			}
			
			// If we are currently following a monster who is alive, and we have already followed them for X seconds, then switch
			if(lastMonster != null && !isMonsterDeadButStillVisible && System.nanoTime() > gc.monsterToFollow_timeToFindNewMonster) {
				lastMonster = null;
			}
						
			// See if we can find an interesting player
			if(lastMonster == null || !lastMonster.isPlayerCreature()) {
				
				IMutableCreature interestingPlayer = findMostInterestingCreature(gc, true, false);
				
				// If we couldn't find one, but there is at least one creature in our ignore list, then remove it and try again.
				if(interestingPlayer == null && gc.previouslyWatched.size() > 0) {
					gc.previouslyWatched.clear();
					interestingPlayer = findMostInterestingCreature(gc, true, false);
				}
				
				// If we found one, then start watching it
				if(interestingPlayer != null) {
					gc.monsterToFollow = interestingPlayer;
					gc.monsterToFollow_timeToFindNewMonster = System.nanoTime() + RCConstants.TIME_TO_FOLLOW_PLAYER_IN_WORLD_VIEW_NANOS;
					gc.previouslyWatched.put(interestingPlayer.getId(), true);
					lastMonster = interestingPlayer;
				}			
			} 
			
			// If no player, see if we can find an interesting monster
			if(lastMonster == null) {
				IMutableCreature interestingPlayer = findMostInterestingCreature(gc, false, true);
				if(interestingPlayer != null) {
					gc.monsterToFollow = interestingPlayer;
					gc.monsterToFollow_timeToFindNewMonster = System.nanoTime() + RCConstants.TIME_TO_FOLLOW_PLAYER_IN_WORLD_VIEW_NANOS;
//					gc.previouslyWatched.put(interestingPlayer.getId(), true);
					lastMonster = interestingPlayer;
				}
			}
			
			
			if(lastMonster != null) {
				viewOnlyClientPosNew = centerAroundCreature(lastMonster, map, serverFollowWidth, serverFollowHeight);
			} else {
				viewOnlyClientPosNew = new Position(0, 0);				
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			viewOnlyClientPosNew = new Position(0, 0);
			log.severe("Exception in creature follow logic", e, lc);
		}
		// End view only 

		// Update browser clients
		{
			perf.startSection();
			JsonBuilderFactory factory = Json.createBuilderFactory(new HashMap<String, Object>());
		
			List<ICreature> creatures = gc.creatureEntryMap.values().stream().map(e -> e.mutableCreature).collect(Collectors.toList());
			
			for(ActiveWSClient activeClient : roundScope.getActiveClients().getList()) {
				try {
					if(!activeClient.getSessions().stream().anyMatch(e -> e.getType() == Type.BROWSER && e.isSessionOpenUnsynchronized() )) { continue; }
				
					// When a new browser client connects (and thus they need a full state update), we send a full update to all the streams
					boolean doesOneOfTheStreamsNeedAFullUpdate = activeClient.getSessions()
							.stream().anyMatch( e -> e.getType() == Type.BROWSER && e.isSessionOpenUnsynchronized() && e.isBrowserFirstConnect());
					
					EngineWebsocketState ews = (EngineWebsocketState) activeClient.getGameEngineInfo();
					if((ews == null || doesOneOfTheStreamsNeedAFullUpdate) && (activeClient.getViewType() != ViewType.CLIENT_VIEW) ) {
						ews = new EngineWebsocketState();
						
						if(activeClient.getViewType() == ViewType.SERVER_VIEW_FOLLOW) {
							ews.setWidth(serverFollowWidth);
							ews.setHeight(serverFollowHeight);							
						} else if(activeClient.getViewType() == ViewType.SERVER_VIEW_WORLD) {
							ews.setWidth(map.getXSize());
							ews.setHeight(map.getYSize());
							
						} else {
							ews.setWidth(agentClientWidth);
							ews.setHeight(agentClientHeight);
							
							log.severe("Unrecognized view type: "+activeClient.getViewType(), lc);
						}
						ews.setCurrClientWorldX(-1 /*clientPosX*/);
						ews.setCurrClientWorldY(-1 /*clientPosY*/ );
		//				ews.setNextFrame(0);
						activeClient.setGameEngineInfo(ews);
					}  
										
					int innerClientPosX;
					int innerClientPosY;
					
					if(activeClient.getViewType() == ViewType.CLIENT_VIEW) {
						if(ews != null && ews.getPlayerCreature() != null) {
							
							Position clientPos = centerAroundCreature(ews.getPlayerCreature(), map, agentClientWidth, agentClientHeight);
							innerClientPosX = clientPos.getX();
							innerClientPosY = clientPos.getY();
						} else {
							log.severe("Player creature not defined for client view", lc);
							continue;
						}
					} else if(activeClient.getViewType() == ViewType.SERVER_VIEW_FOLLOW) {
						innerClientPosX = viewOnlyClientPosNew.getX();
						innerClientPosY = viewOnlyClientPosNew.getY();						
					} else if(activeClient.getViewType() == ViewType.SERVER_VIEW_WORLD) {
						innerClientPosX = 0;
						innerClientPosY = 0;
					} else {
						log.severe("Unrecognized view type: "+activeClient.getViewType(), lc);
						continue;
					}
					
					
					String str = BrowserWebSocketClientShared.generateBrowserJson(ews.getCurrClientWorldX(), ews.getCurrClientWorldY(), 
							innerClientPosX /*clientPosX*/, innerClientPosY /*clientPosY*/, ews.getWidth(), ews.getHeight(), map, changedTiles, creatures, gc.ticks, factory);
					
					activeClient.getSessions().stream().filter(e -> e.getType() == Type.BROWSER && e.isSessionOpenUnsynchronized()).forEach( e -> {
						try {
							e.writeToClientAsync(str);
							e.setBrowserFirstConnect(false);
						} catch(Exception ex) {
							// Previous a problematic session from impacting the rest.
							log.err("Error sending state to browser", ex, lc);
							ex.printStackTrace();
						}
					});
				
					ews.setCurrClientWorldX(innerClientPosX);
					ews.setCurrClientWorldY(innerClientPosY);

					ews.setNextFrame(ews.getNextFrame()+1);

					
				} catch(Exception e) {
					// Prevent a problematic client from affecting the rest.
					log.severe("Error on sending browser updates", e, lc);
					e.printStackTrace();
				}
			}
			perf.endSection(GameLoopPerf.Section.UPDATE_BROWSER_CLIENTS);
		}
		
			
		// Do monster stuff while we wait for player input; give monster a copy of the map that will only change in safe ways.		
		if(!gc.isRoundOver) {
			perf.startSection();
			
			map = map.collapseOverlayIntoNewMap();
			gc.map = map;
			RCArrayMap immutableMapForRead = map.cloneForRead();

			List<IEvent> lastTurnsEvents = gc.eventsPreviousFrames.getList(gc.ticks-1);
			
			gc.sharedMonsterEventLog.internalAddEvents(lastTurnsEvents);
			
			gc.sharedMonsterEventLog.internalClearOldEvents(gc.ticks);

			int roundTimeRemainingInSeconds = (int) TimeUnit.SECONDS.convert( roundScope.getCurrentRoundEndInNanos() - System.nanoTime(), TimeUnit.NANOSECONDS);
			for(CreatureEntry ce : gc.creatureEntryMap.values()) {
				
				try {
					ICreature m = ce.getCreature();
					if(m == null || m.isPlayerCreature()) { continue; }	
					
		 			AIContext aiContext = gc.monsterToAIMap.get(m.getId());
					
					if(aiContext == null) {
						continue;
					}
					SelfState selfState = new SelfState(m);
					WorldState worldState = new WorldState(immutableMapForRead);
					worldState.setWorldHeight(immutableMapForRead.getYSize());
					worldState.setWorldWidth(immutableMapForRead.getXSize());
					worldState.setCurrentGameTick(gc.ticks);
					// Monsters may cheat, as they see the whole map at once: the height and width here are for informational purposes only.   
					worldState.setViewHeight(agentClientHeight); 
					worldState.setViewWidth(agentClientWidth);
					worldState.setRemainingSecondsInRound(roundTimeRemainingInSeconds);
					
					gc.monsterMachine.informMonster(gc.ticks, selfState, worldState, aiContext, gc.sharedMonsterEventLog);
					
//					aiContext.getClient().receiveStateUpdate(selfState, worldState, gc.sharedMonsterEventLog);
				
				} catch(Exception e) {
					// Prevent a problematic creature from impacting the rest. 
					e.printStackTrace();
					log.severe("Error in monster state update", lc);
				}
			}
			
			perf.endSection(GameLoopPerf.Section.DO_MONSTER_STUFF);

		}
		
		// Send health checks
		if(!gc.isRoundOver) {
			
			for(ActiveWSClient client : roundScope.getActiveClients().getList()) {
	
				try {
					ActiveWSClientSession clientSession = client.getMostRecentSessionOfType(Type.CLIENT);
					
					if(clientSession == null) { continue; }
					
					// Every 100 frames, send a health check
					if(gc.getTicks() % 100 != client.getUserId()) { continue; }

					ClientStatistics cs = null;
					
					EngineWebsocketState ews = (EngineWebsocketState)client.getGameEngineInfo();
					if(ews != null && ews.getPlayerCreature() != null) {
						IMutableCreature mc = ews.getPlayerCreature();
						CreatureEntry ce = gc.getOrCreateCreatureEntry(mc);
						cs = ce.clientStats;
					}
					
					if(cs != null) {
						boolean passed = true;
						if(client.getActiveHealthCheckId() != null && client.getActiveHealthCheckSinceInNanos() != null) {
							long elapsedTimeSinceLastCheck = TimeUnit.SECONDS.convert(System.nanoTime() - client.getActiveHealthCheckSinceInNanos(), TimeUnit.NANOSECONDS);
							if(elapsedTimeSinceLastCheck >= 8) {
								// If we've not received a response in 8 seconds, then mark it as failed.								
								passed = false;
							} 
						} 
						cs.setPassedPreviousHealthCheck(passed);
					}
					
					
					long id = gc.generator.getNextUniqueId(IdType.MESSAGE);
					JsonHealthCheck jhc = new JsonHealthCheck(id);
				
					clientSession.writeToClientAsync(om.writeValueAsString(jhc));
					
					client.addHealthCheckId(id);
					
				} catch(Exception e) {
					e.printStackTrace();
					log.severe("Unexpected exception during health check", e, lc);
				}
				
			}
		}

		
		elapsedNanosSinceReset += System.nanoTime() - gc.startOfLastFrameInNanos;
		
		if(gc.ticks % 100 == 0) {
			
			System.out.println("_");
			System.out.println("_");
			System.out.println("msecs per 100 frames:"+ TimeUnit.MILLISECONDS.convert(elapsedNanosSinceReset, TimeUnit.NANOSECONDS));

			perf.outputStats();
			
			perf.reset();
			
			elapsedNanosSinceReset = 0;
		}


		// New Frame!

		long nsSleep = RCConstants.NS_PER_FRAME - ( System.nanoTime() - gc.startOfLastFrameInNanos);
		if(nsSleep > 0) {
			TimeUnit.NANOSECONDS.sleep( nsSleep   );
		}
		
		gc.startOfLastFrameInNanos = System.nanoTime();
		
		// Keep the last 100 frames of events
		gc.eventsPreviousFrames.removeFramesOlderThanX(gc.ticks-100);

		changedTiles.clear();
		
		changedTiles.addAll(changedTilesNewPlayers);
		changedTilesNewPlayers.clear();

		if(!gc.isRoundOver && System.nanoTime() > roundScope.getCurrentRoundEndInNanos()) {
			roundScope.setNextRoundStartInNanos(System.nanoTime()+RCConstants.TIME_BETWEEN_ROUNDS_IN_NANOS); 
			gc.isRoundOver = true;
			
			// On round over, wait 5 seconds then garbage collect.
			new Thread() {
				public void run() {
					try { Thread.sleep(5 * 1000); } catch (InterruptedException e) { e.printStackTrace(); }
					System.gc();
				};
			}.start();
			
			
			IDatabase db = DatabaseInstance.get();
			
			gc.scoreMap.entrySet().stream().forEach( e -> {
				long userId = e.getKey();
				long score = e.getValue();
			
				db.createOrUpdateDbLeaderboardEntry(new DbLeaderboardEntry(userId, score, gc.roundScope.getRoundId()));
			});
			
		}
				
		// After the round is over, and then after a short delay, we stop the game thread and start a new round.
		if(gc.isRoundOver && System.nanoTime() > roundScope.getNextRoundStartInNanos()) {
			continueGameThread = false;
		}
		
		// post wait
		
		// Perform and reply to player actions
		if(!gc.isRoundOver) { 
			perf.startSection();
			for(ActiveWSClient client : roundScope.getActiveClients().getList()) {
				
				try {
				
					ActiveWSClientSession clientSession = client.getMostRecentSessionOfType(Type.CLIENT);
					
					EngineWebsocketState ews = (EngineWebsocketState) client.getGameEngineInfo();
					
					if(clientSession == null || ews == null) { continue; }
					
					// Null if the player did not successfully complete any actions, or the action performed otherwise
					IAction successfulAction = null;

					// Process up to 3 invalid actions per frame (but we will only process one valid action per frame)
					actionsProcessedFor: for(int actionsProcessed = 0; actionsProcessed < 3; actionsProcessed++) {
	
						// Find the first action we have not already processed.
						ReceivedAction receivedAction = null;						
						{
							boolean continueLoop = true;
							while(continueLoop ) {
								receivedAction = client.getAndRemoveWaitingAction();
								
								if(receivedAction == null) { break; }
			
								// Skip messages we have already processed.
								if(ews.isMessageIdReceived(receivedAction.getMessageId())) {
									if(NG.ENABLED) {
										NG.log(RCRuntime.GAME_TICKS.get(), "Skipping already processed message: "+receivedAction.getMessageId());
									}
									continue;
								} else {
									ews.addMessageIdReceived(receivedAction.getMessageId());
									continueLoop = false;
									
								}
			
							}
						}
			
						if(receivedAction != null) {
							ProcessActionReturn par = processPlayerAction(receivedAction, map /*immutableMapForRead*/, clientSession, ews, gc, om, lc);
							IActionResponse response = par.response; 
							
							if(response != null && response.actionPerformed()) {
								successfulAction = par.action;
							}
							
							if(response == null || response.actionPerformed()) {
								break actionsProcessedFor;
							} else {
								/* The action was invalid, so see if there is another one waiting, at the top of the loop.*/
							}
						} else {
							break actionsProcessedFor;
						}
												
					}
					
					
					if(ews.getPlayerCreature() != null) {
						
						CreatureEntry ce = gc.getOrCreateCreatureEntry(ews.getPlayerCreature());
						
						ActionType type = successfulAction != null ? successfulAction.getActionType() : ActionType.NULL;
						
						ce.addAction(type);
												
						if(!ews.getPlayerCreature().isDead()) {
							gc.increaseScore(client.getUserId(), 1);
						}
					}
				
				} catch(Exception e) {
					// Prevent exceptions from affecting other clients.
					e.printStackTrace();
					log.severe("Error occurred during action processing", e, lc);
				}
	
			}
			perf.endSection(GameLoopPerf.Section.REPLY_TO_PLAYER_ACTIONS);
		}
		
		
		// Perform and reply to monster actions
		if(!gc.isRoundOver) {
			perf.startSection();
			for(CreatureEntry ce : gc.creatureEntryMap.values()) {
				IMutableCreature m = ce.getCreature();
				try {
					if(m.isDead()) { continue; }
					if(m.isPlayerCreature()) { continue; }
					
					AIContext aiContext = gc.monsterToAIMap.get(m.getId());
					
					if(aiContext == null) {
						continue;
					}
					
					IAction action = aiContext.getAction();
					
					if(action == null) {
						ce.addAction(ActionType.NULL);
						continue; 
					}
					
					IActionResponse response = doAction(action, map /*immutableMapForRead*/, m, changedTiles, gc);
					
					if(response != null && response.actionPerformed()) {
						ce.addAction(action.getActionType());
					}
					
					aiContext.getFuture().internalSetResponse(response);
				
				} catch(Exception e) {
					// Prevent exceptions from affecting other monsters.
					e.printStackTrace();
					log.severe("Error occured during monster action processing", lc);
				}
			}	
			
			
			if(false) {
				
//				System.out.println("size: "+gc.monsterStats.getAll().size());
//				
				List<Entry<Long, MonsterRuntimeEntry>> l = gc.monsterStats.getAll().entrySet().stream().sorted( (a, b) -> {
					long delta = b.getValue().calculateAverageNanosPerIncrement() - a.getValue().calculateAverageNanosPerIncrement();
					if(delta > 0) { return 1; }
					if(delta == 0) { return 0; }
					else { return -1; } 
	
				}).collect(Collectors.toList());
				
				for(Entry<Long, MonsterRuntimeEntry> e : l) {
					long msecs = TimeUnit.MILLISECONDS.convert(e.getValue().calculateAverageNanosPerIncrement(), TimeUnit.NANOSECONDS);
					if(msecs > 20) {
						System.out.println(" - " +e.getKey()+" -> "+msecs);
					}
				}
			}
			
			
			perf.endSection(GameLoopPerf.Section.REPLY_TO_MONSTER_ACTIONS);
		}
		
		perf.startSection();
		
		// Apply effects
		if(!gc.isRoundOver) {
			for(CreatureEntry ce : gc.creatureEntryMap.values() ) {
				IMutableCreature m = ce.getCreature();
				if(m.isDead()) { continue; }
				try {
				
					List<Effect> toRemove = new ArrayList<>();
					
					for(Effect e : m.getActiveEffects()) {
						if(e.getType() == EffectType.LIFE) {
							m.setCurrHp( Math.min(m.getHp()+e.getMagnitude(), m.getMaxHp())); // Don't exceed max hp
							changedTiles.add(m.getPosition());
						} else if(e.getType() == EffectType.DAMAGE_REDUCTION) {
							// Ignore
						} else if(e.getType() == EffectType.INVISIBILITY) {
							// Ignore
						} else if(e.getType() == EffectType.VISION_RANGE) {
							// Ignore
						} else {
							log.severe("Unrecognized effect type:"+e.getType() , lc);
						}
						
						e.setRemainingTurns(e.getRemainingTurns()-1);
						if(e.getRemainingTurns() == 0) {
							toRemove.add(e);
						}
					}
					
					// Remove expired effects
					toRemove.stream().forEach( e -> {
						m.removeEffect(e);
					});
				} catch(Exception e) {
					log.severe("Unexpected exception in effect application", e, lc);
				}
			}
		}
		
		// Clean up dead things
		if(!gc.isRoundOver) {
			
//			int creaturesDied = 0;
			AtomicInteger creaturesDied = new AtomicInteger(0);
			
			for(Iterator<Entry<Long, CreatureEntry>> it = gc.creatureEntryMap.entrySet().iterator(); it.hasNext(); ) {
				try {
					Entry<Long, CreatureEntry> e = it.next();

					boolean remove = processCreatureLifecycle(e.getValue().getCreature(), changedTiles, map, creaturesDied, gc);
					if(remove) {
						it.remove();
					}					
				} catch(Exception e) {
					e.printStackTrace();
					log.severe("Unexpected exception in lifecycle cleanup", e, lc);

				} // end try
			} // end for
	
//			if(creaturesDied.get() > 0) {
//				generateMonsters(map, gc, creaturesDied.get(), parent);
//			}			
		}
		
		// Add new monsters and items
		if(!gc.isRoundOver) {
			int totalPlayersJoined = gc.playerCreatureToUserIdMap.keySet().size();
			
			// 0 <= (totalPlayersJoined*20 - gc.itemsAdded) <= 400
			int itemsToAdd = Math.min(400, Math.max(0, totalPlayersJoined*20 - gc.itemsAdded  ));

			if(itemsToAdd > 0) {

				try {
					
					List<AIContext> aiContextList = new ArrayList<>();
					List<GroundObject> newGroundObjects = new ArrayList<>();
					
					List<Monster> newMonsters = WorldGeneration.generatePairedItemsAndMonsters(parent, gc.generator, map, newGroundObjects, (int)(itemsToAdd/2), itemsToAdd, gc.roomSpawns, aiContextList);
		
					newGroundObjects.forEach( go -> {
						gc.groundObjects.put(go.getId(), go);
						gc.changedTiles.add(go.getPosition());
					});
	
					newMonsters.forEach( e -> {
						gc.changedTiles.add(e.getPosition());
						gc.getOrCreateCreatureEntry(e);
					});
					
					aiContextList.forEach( e -> {
						gc.monsterToAIMap.put(e.getCreatureId(), e);
					});
				
				} catch(Exception e) {
					log.severe("Unexpected exception in item generation", e, null);
					e.printStackTrace();
				}

				gc.itemsAdded += itemsToAdd;
			}
			
			try {
				long wanderingMonsters = gc.monsterToAIMap.values().stream().filter( e -> e.getClient() instanceof WanderingAttackAllAI && ((WanderingAttackAllAI)e.getClient() ).getConstrainedArea() == null ).count();
								
				if(wanderingMonsters < 30) {
					
					List<AIContext> aiContextList = new ArrayList<>();
					Position p = AIUtils.findRandomPositionOnMap(0, 0, map.getXSize(), map.getYSize(), false, map);
					if(p != null) {
						Monster m = WorldGeneration.generateSingleMonsterAtPosition(map, p, parent, gc.generator, aiContextList, new AIHint(WorldGenAIType.WANDERING, false));
	
						if(m != null) {
							gc.changedTiles.add(p);
							gc.getOrCreateCreatureEntry(m);
						}
						
						aiContextList.forEach( e -> {
							gc.monsterToAIMap.put(e.getCreatureId(), e);
						});
						
					}
				}
				
				long endTimeInSeconds = TimeUnit.SECONDS.convert(gc.roundScope.getCurrentRoundEndInNanos() - System.nanoTime(), TimeUnit.NANOSECONDS);
				if(endTimeInSeconds < 35) {
					
					if(gc.ticks % 3 == 0) {
						List<AIContext> aiContextList = new ArrayList<>();
						
						int randVal = (int)(Math.random()*4);
						
						Position p = null;
						if(randVal == 0) {
							// North
							p = AIUtils.findRandomPositionOnMap(0, 0, map.getXSize(), 0, false, map);	
						} else if(randVal == 1) {
							// South
							p = AIUtils.findRandomPositionOnMap(0, map.getYSize()-1, map.getXSize(), map.getYSize()-1, false, map);
						} else if(randVal == 2) {
							// East
							p = AIUtils.findRandomPositionOnMap(map.getXSize()-1, 0, map.getXSize()-1, map.getYSize(), false, map);
						} else if(randVal == 3) {
							// West
							p = AIUtils.findRandomPositionOnMap(0, 0, 0, map.getYSize()-1, false, map);
						}
						
						
						if(p != null) {
							Monster m = WorldGeneration.generateSingleMonsterAtPosition(map, p, parent, gc.generator, aiContextList, new AIHint(WorldGenAIType.WANDERING, false ));
		
							if(m != null) {
								gc.changedTiles.add(p);
								gc.getOrCreateCreatureEntry(m);
							}
							
							aiContextList.forEach( e -> {
								gc.monsterToAIMap.put(e.getCreatureId(), e);
							});
							
						}
						
					}
					
				}
				
			} catch(Exception e) {
				log.severe("Unexpectd exception in remove items and monsters", gc.lc);
				e.printStackTrace();
			}
			
		}
		
		perf.endSection(GameLoopPerf.Section.APPLY_EFFECTS_AND_CLEANUP_DEAD);
				
		return continueGameThread;
	}
	
	private static ProcessActionReturn processPlayerAction(ReceivedAction receivedAction, RCArrayMap map, ActiveWSClientSession clientSession, EngineWebsocketState ews, GameContext gc, ObjectMapper om, LogContext lc) throws JsonProcessingException {
		
		ProcessActionReturn result = new ProcessActionReturn();
		
		IAction action = null;
		
		// First, convert the JSON action from the user to a non-JSON action
		if(receivedAction != null) {
			JsonAbstractTypedMessage jsonAction = receivedAction.getJsonAction();
			
			if(jsonAction.getType().equals(JsonCombatAction.TYPE)) {
				JsonCombatAction jca = (JsonCombatAction)jsonAction;
									
				ICreature creature = gc.creatureEntryMap.get(jca.getTargetCreatureId()).getCreature();
				if(creature != null) {
					action = new CombatAction(creature);
				} else {
					log.severe("Could not find creature with id "+jca.getTargetCreatureId()+" in database.", lc);
					action = null;
				}
				
			} else if(jsonAction.getType().equals(JsonStepAction.TYPE)) {
				JsonStepAction jsa = (JsonStepAction)jsonAction;
				action = new StepAction(jsa.getDestination().toPosition());
				
			} else if(jsonAction.getType().equals(JsonNullAction.TYPE)) {
				action = NullAction.INSTANCE;
				
			} else if(jsonAction.getType().equals(JsonMoveInventoryItemAction.TYPE)) {
				JsonMoveInventoryItemAction jpuia = (JsonMoveInventoryItemAction)jsonAction;
				action = new MoveInventoryItemAction(jpuia.getObjectId(),
						jpuia.isDropItem() ?
						MoveInventoryItemAction.Type.DROP_ITEM :
							MoveInventoryItemAction.Type.PICK_UP_ITEM
				);
				
			} else if(jsonAction.getType().equals(JsonEquipAction.TYPE)) {
				
				JsonEquipAction jea = (JsonEquipAction)jsonAction;
				
				OwnableObject ownableObject = ews.getPlayerCreature().getInventory().stream().filter(e -> e.getId() == jea.getObjectId()).findAny().orElse(null);
				
				if(ownableObject != null) {
					action = new EquipAction(ownableObject);	
				} else {
					log.severe("Object could not be found in the player's inventory: "+jea.getObjectId(), lc);
					action = null;
				}
				
			} else if(jsonAction.getType().equals(JsonDrinkItemAction.TYPE)) {
				JsonDrinkItemAction jda = (JsonDrinkItemAction)jsonAction;
				OwnableObject ownableObject = ews.getPlayerCreature().getInventory().stream().filter(e -> e.getId() == jda.getId()).findAny().orElse(null);
				if(ownableObject != null) {
					action = new DrinkItemAction(ownableObject);
				} else {
					log.severe("Object could not be found in the player's inventory: "+jda.getId(), lc);
				}
				
			} else {
				log.severe("Unknown json action type: "+jsonAction.getType(), lc);
				action = null;
			}
		}
		
		result.action = action;
		
		// Process the action, then turn it back into JSON
		if(action != null) {
		
			JsonAbstractTypedMessage jsonResponse = null;
		
			CreatureEntry ce = gc.getOrCreateCreatureEntry(ews.getPlayerCreature());
			
			ce.clientStats.setActionsReceived(ce.clientStats.getActionsReceived()+1);
			
			IActionResponse response = doAction(action, map, ews.getPlayerCreature(), gc.changedTiles, gc);
			if(response instanceof CombatActionResponse) {
				CombatActionResponse car = (CombatActionResponse)response;
				jsonResponse = car.toJson();
				
			} else if(response instanceof StepActionResponse) {
				StepActionResponse sar = (StepActionResponse)response;
				jsonResponse = sar.toJson();
				
			} else if(response instanceof NullActionResponse) {
				@SuppressWarnings("unused")
				NullActionResponse nar = (NullActionResponse)response;
				jsonResponse = new JsonNullActionResponse();
				
			} else if(response instanceof MoveInventoryItemActionResponse) {
				MoveInventoryItemActionResponse pir = (MoveInventoryItemActionResponse)response;
				jsonResponse = pir.toJson();
				
			} else if(response instanceof EquipActionResponse) {
				EquipActionResponse ear = (EquipActionResponse)response;
				jsonResponse = ear.toJson();
				
			} else if(response instanceof DrinkItemActionResponse) {
				DrinkItemActionResponse diar = (DrinkItemActionResponse)response;
				jsonResponse = diar.toJson();
			} else {
				log.severe("Missing abstract message response type:" +response, lc);
			}
			
			result.response = response;
			
			if(jsonResponse != null) {
				JsonActionMessageResponse jamr = new JsonActionMessageResponse();
				jamr.setActionResponse(jsonResponse);
				jamr.setMessageId(receivedAction.getMessageId());
				String responseJson = om.writeValueAsString(jamr);
				ews.putResponseToMessage(receivedAction.getMessageId(), responseJson);
				
				clientSession.writeToClientAsync(responseJson);
			}
			
		} 
		
		return result;

	}
	
	 

	private static void generateUpdateBrowserUI(ActiveWSClient client, List<IEvent> evListPrevFrame, Map<Long /* player id */, JsonScore> idtoScoreMap, 
			List<JsonScore> scores, Map<Long /*user id*/, String /* user name */> localUserIdToUserNameMap, List<JsonServiceStatEntry> statsList, ObjectMapper om, GameContext gc) throws JsonProcessingException {
		
		ActiveWSClientSession clientSession = client.getMostRecentSessionOfType(Type.CLIENT);
		
		if(clientSession == null) {
			clientSession = client.getMostRecentSessionOfType(Type.BROWSER);
		}
		
		if(clientSession == null) { return; }
		
		List<IMutableEvent> newEvents = new ArrayList<>();
		EngineWebsocketState ews = (EngineWebsocketState) client.getGameEngineInfo();
		
		if(ews == null) { return; }

		if(ews.getCurrClientWorldX() >= 0 && ews.getCurrClientWorldY() >= 0 && ews.getHeight() > 0 && ews.getWidth() > 0) {
			for(IEvent e : evListPrevFrame) {
				// Is the event visible to the creature?
				if(Position.containedInBox(e.getWorldLocation(), 
						ews.getCurrClientWorldX(), ews.getCurrClientWorldY(), 
						ews.getWidth(), ews.getHeight())) {
					
					newEvents.add((IMutableEvent)e);
				} 
			}
		}

		JsonUpdateBrowserUI result = new JsonUpdateBrowserUI();
		
		// Update the round info field
		RoundScope rs = gc.roundScope;
		if(gc.isRoundOver)  {
			
			if(client.getViewType() == ViewType.CLIENT_VIEW) {
				Long[] scoreAndRank = DatabaseInstance.get().getUserBestScoreAndRank(client.getUserId());
				if(scoreAndRank != null && scoreAndRank.length == 2) {
					result.setCurrentPlayerBestTotalScore(scoreAndRank[0]);
					result.setCurrentPlayerBestTotalRank(scoreAndRank[1]);
				}
			}
			
			if(rs.getNextRoundStartInNanos() != null) {
				long startTimeInSeconds = TimeUnit.SECONDS.convert(rs.getNextRoundStartInNanos() - System.nanoTime(), TimeUnit.NANOSECONDS);				
				JsonInactiveRoundInfo jiri = new JsonInactiveRoundInfo(rs.getNextRoundId(), startTimeInSeconds);
				result.setRoundState(jiri);
			}
		} else {
			long endTimeInSeconds = TimeUnit.SECONDS.convert(rs.getCurrentRoundEndInNanos() - System.nanoTime(), TimeUnit.NANOSECONDS); 
			JsonActiveRoundInfo jari = new JsonActiveRoundInfo(rs.getRoundId(), endTimeInSeconds);
			result.setRoundState(jari);
		}
		
		if(client.getViewType() == ViewType.CLIENT_VIEW) {
			result.setCurrentPlayerScore(gc.scoreMap.get(client.getUserId()));
		}
		
		if(scores != null && idtoScoreMap != null) {
			result.setCurrentRoundScores(scores);
			if(client.getViewType() == ViewType.CLIENT_VIEW) {
				JsonScore playerScore = idtoScoreMap.get(client.getUserId());
				if(playerScore != null) {
					result.setCurrentPlayerRank(playerScore.getRank());
				} else if(client.getViewType() == ViewType.CLIENT_VIEW){
					log.severe("Unable to find player score in rank list: "+client.getUserId()+" "+client.getUsername(), gc.lc);
				}
			}
		}
		
		if(statsList != null && statsList.size() > 0) {
			result.setStats(statsList);
		}

		if(newEvents.size() > 0) {
			result.setNewEventHtml(new ArrayList<String>());
		}
		
		List<JsonBrowserCombatEvent> browserCombatEvents = new ArrayList<>();
		
		newEvents.forEach( e -> {
			if(e.getActionType() == EventType.COMBAT) {
				CombatActionEvent cae = (CombatActionEvent)e;
				
				if(cae.isHit() && cae.getDamageDone() > 0) {
					browserCombatEvents.add(new JsonBrowserCombatEvent(cae.getWorldLocation().getX(), cae.getWorldLocation().getY(), gc.ticks, cae.getDamageDone()));
				}
				
				String str= "["+gc.ticks+"] ";

				str += convertCreatureToHtml(cae.getAttacker(), client.getUserId(), localUserIdToUserNameMap, gc);
				str += " attacked ";
				str += convertCreatureToHtml(cae.getDefender(), client.getUserId(), localUserIdToUserNameMap, gc);
				str += ". "+cae.userVisibleCombatResult()+", ";
				
				boolean defenderIsPlayer = false;
				
				if(ews.getPlayerCreature() != null && ews.getPlayerCreature().equals(cae.getDefender())) {
					defenderIsPlayer = true;
				}
				
				if(defenderIsPlayer) {
					str += "<b>";
				}
				
				str += cae.getDefender().getName();
				if(defenderIsPlayer) {
					str += "</b>";
				}
				
				str += " now at "+Math.max(0, cae.getDefender().getHp())+" hp.";
				
				result.getNewEventHtml().add(str);
				
			} else if(e.getActionType() == EventType.DRINK) {
				DrinkItemActionEvent dae = (DrinkItemActionEvent)e;
				
				String str = "["+gc.ticks+"] "+convertCreatureToHtml(dae.getCreature(), client.getUserId(), localUserIdToUserNameMap, gc)+" drank "+dae.getDrinkableItem().getName()+".";
				result.getNewEventHtml().add(str);
				
			} else if(e.getActionType() == EventType.STEP) {
				@SuppressWarnings("unused")
				StepActionEvent sae = (StepActionEvent)e;
				/* ignore */
				
			} else if(e.getActionType() == EventType.EQUIP) {
				EquipActionEvent eae = (EquipActionEvent)e;		
				String str = "["+gc.ticks+"] ";
				
				String ttSubstring = convertObjectToHtml(eae.getEquippedObject());
				
				str += convertCreatureToHtml(eae.getCreature(), client.getUserId(), localUserIdToUserNameMap, gc)+" equipped "+eae.getEquippedObject().getName()+ttSubstring+".";
				
				result.getNewEventHtml().add(str);
				
			} else if(e.getActionType() == EventType.MOVE_INVENTORY_ITEM) {
				MoveInventoryItemActionEvent miiae = (MoveInventoryItemActionEvent)e;
				
				String verb = miiae.isDropItem() ? "dropped" : "picked up";
				
				String str = "["+gc.ticks+"] ";
				
				String ttSubstring = convertObjectToHtml(miiae.getObject());
				
				str += convertCreatureToHtml(miiae.getCreature(), client.getUserId(), localUserIdToUserNameMap, gc)+" "+verb+" "+miiae.getObject().getName()+ttSubstring+".";
				result.getNewEventHtml().add(str);
				
			} else {
				log.severe("Unrecognized event type: "+e.getActionType().name(), gc.lc);
			}
		});
		
		
		if(browserCombatEvents.size() > 0) {
			result.setCombatEvents(browserCombatEvents);
		}
		
		if(result.utilContainsAnyData()) {
			// Only write to the client if it actually contains useful data.
			clientSession.writeToClientAsync( om.writeValueAsString(result));
		}

	}

	private static Weapon getDefaultWeapon(GameContext gc) {
		Weapon bareHands = gc.weaponList.getList().stream()
				.filter( e -> e.getName().equals(BARE_HANDS_DEFAULT_WEAPON)  )
				.findFirst()
				.orElseThrow( () -> new IllegalArgumentException("Bare Hands not found.") ); 
		
		return bareHands;
	}
		
	private static void generateAndSendFrameUpdate(ActiveWSClientSession clientSession, ActiveWSClient client, 
			int clientWidth, int clientHeight, GameContext gc,  List<Position> changedTilesNewPlayers) throws JsonProcessingException {
		
		ObjectMapper om = gc.om;
		
		RCArrayMap map = gc.map;
		
		JsonWorldState jws;
	
		boolean fullResetClientState = clientSession.getAndResetFullClientResetState();
		
		EngineWebsocketState ews = (EngineWebsocketState) client.getGameEngineInfo();
		if(ews == null) {
			ews = new EngineWebsocketState();
			ews.setWidth(clientWidth);
			ews.setHeight(clientHeight);

			client.setGameEngineInfo(ews);
		
			Weapon bareHands = getDefaultWeapon(gc) ;

			int posX = -1;
			int posY = -1;

			{
				while(true) {
					// TODO: LOW - I need to make sure the player can path to something else
					posX = (int)(Math.random() * map.getXSize());
					posY = (int)(Math.random() * map.getYSize());
					Tile t = map.getTile(posX, posY);
					if(t != null && t.isPresentlyPassable()) {
						break;
					}
				}
			}
			
			PlayerCreature pc = new PlayerCreature(client.getUsername(), gc.generator.getNextUniqueId(IdType.CREATURE), new Position(posX, posY), TileTypeList.CYBER_KNIGHT, new ArmourSet());
			pc.setWeapon(bareHands);
			pc.setMaxHp(250);
			pc.setCurrHp(pc.getMaxHp());
			
			CreatureEntry ce = gc.getOrCreateCreatureEntry(pc);

			gc.playerCreatureToUserIdMap.put(pc.getId(), client.getUserId());
			
			ce.clientStats.setStartTimeInNanos(System.nanoTime());
			
			map.getTileForWrite(pc.getPosition()).getCreaturesForModification().add(pc);
			changedTilesNewPlayers.add(pc.getPosition());
			
			ews.setPlayerCreature(pc);
		} else {
			// if ews is not null
		
			if(fullResetClientState) {
				ews.clearClientState();
			}
			
		}

		boolean sendFullFrame = false;
		// If the user has disconnected and reconnected, then send them the full world state.
		{
			ActiveWSClientSession previousClientSession = gc.mapClientToMostRecentSession.get(client); 
			if(previousClientSession != null && !(previousClientSession.equals(clientSession))) {
				System.out.println("Disconnect detected, sending full thing.");
				ews.setCurrClientWorldX(-1);
				ews.setCurrClientWorldY(-1);
				sendFullFrame = true;
			}
		}
		
		IMutableCreature playerCreature = ews.getPlayerCreature();
					
		jws = WorldStateJsonGenerator.generateJsonWorldState(clientWidth, clientHeight, ews, map, gc.changedTiles, gc, gc.lc, gc.roundScope, sendFullFrame);
		
//		clientPosX = jws.getClientViewPosX();
//		clientPosY = jws.getClientViewPosY();
		
		JsonSelfState jss = new JsonSelfState();
		jss.setPlayerId(playerCreature.getId());
		
		playerCreature.getInventory().stream().forEach( e -> {
			jss.getInventory().add(e.toJson());
		});
				
		JsonFrameUpdate jfu = new JsonFrameUpdate();
		jfu.setWorldState(jws);
		jfu.setSelfState(jss);
		jfu.setFull(sendFullFrame);
		jfu.setGameTicks(gc.ticks);
		jfu.setFrame(client.getAndIncrementNextClientFrameNumber());
		
		String msg = om.writeValueAsString(jfu);
		
		clientSession.writeToClientAsync( msg);
		
		gc.mapClientToMostRecentSession.put(client, clientSession);
		
		Long lastMessageIdReceived = client.getAndResetLastActionMessageIdReceived();
		if(lastMessageIdReceived != null && lastMessageIdReceived >= 0) {
			
			List<String> oldActionsToResend = ews.getActionMessagesWithIdsGreaterThan(lastMessageIdReceived);
			for(String str : oldActionsToResend) {
				clientSession.writeToClientAsync(str);
			}
			
		}
		
		ews.setCurrClientWorldX(jws.getClientViewPosX());
		ews.setCurrClientWorldY(jws.getClientViewPosY());

		ews.setWidth(clientWidth);
		ews.setHeight(clientHeight);

		ews.setNextFrame(ews.getNextFrame()+1);
		
		CreatureEntry ce = gc.getOrCreateCreatureEntry(playerCreature);

		if(ce != null && playerCreature.isDead()) {
			JsonClientInterrupt jci = new JsonClientInterrupt();
			jci.setInterruptNumber(ce.clientStats.getTimesDied());
			jci.setRound(gc.roundScope.getRoundId());
			
			msg = om.writeValueAsString(jci);
			clientSession.writeToClientAsync(msg);
		}
		
	}
	
//	private static void generateMonsters(IMutableMap map, GameContext gc, int num, ServerInstance parent) {
//		
//		List<AIContext> aiContextList = new ArrayList<>();
//		
//		List<Monster> newMonsters = WorldGeneration.generateMonsters(parent, map, aiContextList, gc.generator, null, num);
//		
//		newMonsters.forEach( e -> {
//			gc.changedTiles.add(e.getPosition());
//			gc.getOrCreateCreatureEntry(e);
////			gc.creatureMap.put(e.getId(), e);
//		});
//		
//		aiContextList.forEach( e -> {
//			gc.monsterToAIMap.put(e.getCreatureId(), e);
//		});
//
//	}
	
	private static Position centerAroundCreature(ICreature m, IMap map, int clientWidth, int clientHeight) {
		int x = m.getPosition().getX()-(clientWidth/2);
		int y = m.getPosition().getY()-(clientHeight/2);
		
		return normalizePositionBox(x, y, clientWidth, clientHeight, map);
	}
	
		
	private static boolean processCreatureLifecycle(IMutableCreature m, List<Position> changedTiles, IMutableMap map, AtomicInteger creaturesDied, GameContext gc) {

		boolean remove = false;
		
		CreatureEntry ce = gc.getOrCreateCreatureEntry(m);
		
		if(m.getHp() <= 0) {
			
			if(m.isPlayerCreature()) {						
				if(!ce.isCurrentDeathProcessed) {
					ce.isCurrentDeathProcessed = true;

					ce.playerTickOfDeath = gc.ticks;
					
//					ActiveWSClient awsClient = gc.creatureIdToClientMap.get(m.getId());
//					if(awsClient == null) {
//						log.severe("Unable to locate clinet for creature "+m.getId(), gc.lc);
//						return false;
//					}
					
					ce.creatureReviveTick = gc.ticks + RCConstants.REVIVE_DEAD_PLAYER_AFTER_X_TICKS; 
					
					ce.clientStats.setTimesDied(ce.clientStats.getTimesDied()+1);
					
					
					List<IObject> droppedObjects = new ArrayList<>();
					Weapon defaultWeapon = getDefaultWeapon(gc);
					
					if(Math.random() < 0.5) {
						Weapon oldWeapon = m.getWeapon();
						if(!oldWeapon.equals(defaultWeapon)) {
							m.setWeapon(getDefaultWeapon(gc));
							
							droppedObjects.add(oldWeapon);										
						}
					}
					
					AtomicBoolean inventoryRemovalFailed = new AtomicBoolean(false);
					
					m.getArmour().getAll().stream().filter( a -> (Math.random() < 0.5)).forEach( a -> {
						droppedObjects.add(a);
						if(!m.getArmour().remove(a)) {
							inventoryRemovalFailed.set(true);
						}
					});
					
					m.getInventory().stream().filter(i -> (Math.random() < 0.5)).forEach( i -> {
						droppedObjects.add(i.getContainedObject());
						if(!m.removeFromInventory(i)) {
							inventoryRemovalFailed.set(true);
						}
						
					});

					// Removing items from the inventory and putting them on the ground should NEVER fail.
					// However, here we verify that it did not fail to prevent item duping, which can be exploited.
					if(!inventoryRemovalFailed.get()) {
						for(IObject dropped : droppedObjects) {
							GroundObject go = new GroundObject(gc.generator.getNextUniqueId(IdType.OBJECT), dropped, m.getPosition());
							
							Tile t = map.getTileForWrite(m.getPosition());
							t.getGroundObjectForModification().add(go);
							changedTiles.add(m.getPosition());
							gc.groundObjects.put(go.getId(), go);
						}						
					} else {
						log.severe("Inventory removal failed for one or more items.", gc.lc);
					}
					
					m.setMaxHp( Math.max(1, (int)(m.getMaxHp()*RCConstants.PLAYER_HP_REDUCTION_ON_DEATH)) );
					
					m.getActiveEffects().forEach( ae -> { m.removeEffect(ae); } );
				} else {
					// Death has already been processed
					if(ce.creatureReviveTick < gc.ticks) {
						ce.isCurrentDeathProcessed = false;
						ce.creatureReviveTick = -1;
						ce.playerTickOfDeath = null; 
						m.setCurrHp(m.getMaxHp());
						changedTiles.add(m.getPosition());
					}
				}
				
			} else {
				
				// Non-player creatures
				
				Long gameTickOfDeath = ce.monsterTickOfDeath;// gc.mapCreatureToDeathGameTick.get(m.getId());
				if(gameTickOfDeath == null) {
					gameTickOfDeath = gc.ticks;
					ce.monsterTickOfDeath = gameTickOfDeath;
					creaturesDied.incrementAndGet();
				}
				
				// If a creature has been dead for more than 300 turns, then remove it.
				if(gc.ticks - gameTickOfDeath > 100) {
					map.getTileForWrite(m.getPosition()).getCreaturesForModification().remove(m);
					changedTiles.add(m.getPosition());
//					it.remove();
					remove = true;

					if(!m.isPlayerCreature()) {
						gc.monsterToAIMap.remove(m.getId());
					}
					
					gc.monsterStats.removeEntry(m);
					ce.monsterTickOfDeath = null;
				}						
			}
		
		} 

		return remove;
	}
	
	private static String convertObjectToHtml(IObject o) {
		if(o == null) { return ""; }
		
		String ttSubstring = "";
		TileType tt = o.getTileType();
		if(tt != null && tt.getNumber() > 0) {
			ttSubstring = " <img src='resources/tiles/"+tt.getNumber()+".png' width='15' height='15'/>";
		}
		
		return ttSubstring;
	}
	
	private static String convertCreatureToHtml(ICreature creature, long activeUserId, Map<Long, String> localUserIdToUserNameMap, GameContext gc) {

		if(creature == null) {
			log.severe("Null creature passed to convertCreatureToHtml", gc.lc);
			return null;
		}
		
		String username = null;
		Long userId = gc.playerCreatureToUserIdMap.get(creature.getId());
		if(userId != null) {
			username = localUserIdToUserNameMap.get(userId);
		}
		
		boolean isActiveClient = userId != null && userId.longValue() == activeUserId;

		String result = "";
		if(isActiveClient) {
			result += "<b>";
		}
		
		if(username != null) {
			result += username;	
		} else {
			result += creature.getName();
		}
		
		if(isActiveClient) {
			result += "</b>";	
		}

		result +=  " "+creature.getPosition();
		return result;

	}
	
//	private static void increaseScore( Long userId, int scoreIncrease, GameContext gc) {
//		if(userId == null) { return; }
//
//		long newScore = gc.scoreMap.getOrDefault(userId, 0l) + scoreIncrease;
//
//		if(scoreIncrease > 5) {
//			log.info("Player "+userId+" had score increased by "+scoreIncrease+" to "+newScore, gc.lc);
//		}
//		
//		gc.scoreMap.put(userId, newScore);
//		
//	}
	
	private static IActionResponse doAction(IAction action, RCArrayMap map, IMutableCreature m, List<Position> changedTiles, GameContext gc) {
		IActionResponse response = NullActionResponse.INSTANCE;

		LogContext lc = gc.lc;
		
		// user id, or null if this action is not being performed by a user.
		Long userId = gc.playerCreatureToUserIdMap.get(m.getId()); 
		
		if(action.getActionType() == ActionType.NULL) {
			// ignore			
		} else if(action.getActionType() == ActionType.COMBAT) {
			
			CombatActionResponse.CombatActionResult result;
			
			CombatAction combatAction = (CombatAction)action;
			
			if(m.isDead()) {
				return new CombatActionResponse(CombatActionResult.COULD_NOT_ATTACK, 0, combatAction.getTarget());
			}
			
			ICreature targetC = combatAction.getTarget();
			// Ensure that:
			// - attacker can reach defender
			// - defender is not already dead
			// - attacker is not trying to attack themselves
			if(targetC != null && AIUtils.canReach(m.getPosition(), targetC.getPosition(), map) && !targetC.isDead() && targetC.getId() != m.getId()) {
				int damageDealt = 0;
				
				CombatResult combat = Combat.doCombat(m, (IMutableCreature) targetC, lc);
				if(combat.isHit()) {
					result = CombatActionResult.HIT;
					damageDealt = combat.getDamageDealt();
					
					// On kill, increase score by 1000 * level.
					if(targetC.getHp() <= 0) {
						gc.increaseScore(userId, 1000 * targetC.getLevel());
					}
				} else {
					result = CombatActionResult.MISS;
				}
				response = new CombatActionResponse(result, damageDealt, targetC);
				changedTiles.add(targetC.getPosition());
				changedTiles.add(m.getPosition());
				
				gc.eventsPreviousFrames.add(new CombatActionEvent(m, targetC, combat.isHit(), combat.getDamageDealt(), gc.ticks, gc.generator.getNextUniqueId(IdType.EVENT)));
				
			} else {
				result = CombatActionResult.COULD_NOT_ATTACK;				
				response = new CombatActionResponse(result, 0, null);
			}
			
			
		} else if(action.getActionType() == ActionType.STEP) {
			StepAction stepAction = (StepAction)action;
			
			if(m.isDead()) {
				return new StepActionResponse(StepActionFailReason.OTHER);
			}
			
			Tile destTile = map.getTile(stepAction.getDestPosition());
			if(destTile == null) {
				log.err("Invalid destination step action position:"+stepAction.getDestPosition(), lc);
				return new StepActionResponse(StepActionFailReason.OTHER);
			} else if(destTile.isPresentlyPassable() && AIUtils.canReach(m.getPosition(), stepAction.getDestPosition(), map)) {
				Tile srcTile = map.getTileForWrite(m.getPosition());
				boolean removed = srcTile.getCreaturesForModification().remove(m);
				
				if(removed) {
					gc.eventsPreviousFrames.add(new StepActionEvent(m, m.getPosition(), stepAction.getDestPosition(), gc.ticks, gc.generator.getNextUniqueId(IdType.EVENT)));
					changedTiles.add(m.getPosition());
					
					Position destPos = stepAction.getDestPosition();
					m.setPosition(destPos);
					destTile = map.getTileForWrite(destPos);
					destTile.getCreaturesForModification().add(m);
					changedTiles.add(destPos);
					response = new StepActionResponse(destPos);
				} else {
					response = new StepActionResponse(StepActionFailReason.OTHER);
				}
			} else {
				response = new StepActionResponse(StepActionFailReason.BLOCKED);	
			}
			
		} else if(action.getActionType() == ActionType.MOVE_INVENTORY_ITEM) {
			
			MoveInventoryItemAction puia = (MoveInventoryItemAction)action;
			
			if(puia.isDropItem()) {
				// Remove the item from the inventory
				
				OwnableObject oo = m.getInventory().stream().filter(e -> e.getId() == puia.getObjectId()).findAny().orElse(null);
				if(oo == null) {
					log.severe("Object is not in the creature's inventory: "+puia.getObjectId(), lc);
					return new MoveInventoryItemActionResponse(puia.getObjectId(), false, puia.isDropItem());
				}
				
				if(!m.removeFromInventory(oo)) {
					log.severe("Could not remove the item from the creature's inventory", lc);
					return new MoveInventoryItemActionResponse(puia.getObjectId(), false, puia.isDropItem());
				}
				
				GroundObject go = new GroundObject(oo.getId(), oo.getContainedObject(), m.getPosition());
				Tile t = map.getTileForWrite(m.getPosition());
				
				t.getGroundObjectForModification().add(go);
				
				changedTiles.add(m.getPosition());
				
				log.interesting("Player creature "+m.getId()+" dropped item "+go, lc);
				
				gc.groundObjects.put(go.getId(), go);
				
				gc.eventsPreviousFrames.add( new MoveInventoryItemActionEvent(m, oo.getContainedObject(), true, gc.ticks, gc.generator.getNextUniqueId(IdType.EVENT)));
				
				response = new MoveInventoryItemActionResponse(puia.getObjectId(), true, puia.isDropItem());
				
			} else {
				// Add the item to the inventory
				
				IGroundObject go = gc.groundObjects.get(puia.getObjectId());
				
				if(go == null) {
//					List<Tile> neighbouringTiles = new ArrayList<Tile>();
//					neighbouringTiles.add(map.getTile(m.getPosition()));
//					AIUtils.getValidNeighbouringPositions(m.getPosition(), map).stream().forEach(e -> {
//						neighbouringTiles.add(map.getTile(e));
//					});
//					
//					List<IGroundObject> neighbouringGos = new ArrayList<>();
//					neighbouringGos.addAll(neighbouringTiles.stream().flatMap(e -> e.getGroundObjects().stream()  ).collect(Collectors.toList()));
					
					log.severe("We could not find the object id in our database, creature:"+m+" object id: "+puia.getObjectId(), lc);
					return new MoveInventoryItemActionResponse(puia.getObjectId(), false, puia.isDropItem());
				}
				
				if(!AIUtils.canReach(m.getPosition(), go.getPosition(), map)) {
					log.severe("The creature can't reach the object", lc);
					return new MoveInventoryItemActionResponse(puia.getObjectId(), false, puia.isDropItem());
				}
				
				changedTiles.add(m.getPosition());
				changedTiles.add(go.getPosition());
				
				// Remove the ground object from the ground objects db
				if(gc.groundObjects.remove((Long)puia.getObjectId()) == null) {
					log.severe("The object was not found in the db", lc);
					return new MoveInventoryItemActionResponse(puia.getObjectId(), false, puia.isDropItem());
				}
				
				Tile t = map.getTileForWrite(go.getPosition());
				if(!t.getGroundObjectForModification().remove(go)) {
					log.severe("The object was not found at the expected position;", lc);
					return new MoveInventoryItemActionResponse(puia.getObjectId(), false, puia.isDropItem());
				}
				
				log.interesting("Creature "+m.getId()+" added "+go.getId()+" to their inventory.", lc);
				
				OwnableObject oo = new OwnableObject(go.get(), go.getId());
				m.addToInventory(oo);
				
				gc.eventsPreviousFrames.add( new MoveInventoryItemActionEvent(m, oo.getContainedObject(), false, gc.ticks, gc.generator.getNextUniqueId(IdType.EVENT)));
				
				response = new MoveInventoryItemActionResponse(puia.getObjectId(), true, puia.isDropItem());
			}
			
						
		} else if(action.getActionType() == ActionType.EQUIP) {
			EquipAction ea = (EquipAction)action;
			
			IObject containedObject = ea.getOwnableObject().getContainedObject();
			if(containedObject.getObjectType() == ObjectType.ITEM) {
				
				log.err("Attempt to equip non-armour or non-weapon object,", lc);
				return new EquipActionResponse(false, ea.getOwnableObject().getId());
				
			}
			
			if(!m.removeFromInventory(ea.getOwnableObject())) {
				log.err("Attempt to equip item "+ea.getOwnableObject().getId()+"  that was not in creature "+m.getId()+" inventory.", lc);
				return new EquipActionResponse(false, ea.getOwnableObject().getId());
			}
			
			changedTiles.add(m.getPosition());
			
			gc.eventsPreviousFrames.add(new EquipActionEvent(ea.getOwnableObject().getContainedObject(), m, gc.ticks, gc.generator.getNextUniqueId(IdType.EVENT)));
			
			if(containedObject.getObjectType() == ObjectType.ARMOUR) {
				
				Armour newArmour = (Armour)containedObject;
				ArmourSet set = m.getArmour();				
				Armour previousItemInSlot = set.get(newArmour.getType());
				
				// Put the new armour to the appropriate slot
				set.put(newArmour);
				
				if(previousItemInSlot != null) {
					// Move the previously equipped item back into the inventory
					m.addToInventory(new OwnableObject(previousItemInSlot, gc.generator.getNextUniqueId(IdType.OBJECT)));
				}
				
				CreatureEntry ce = gc.creatureEntryMap.get(m.getId());
				if(m.isPlayerCreature() && ce != null && userId != null) {
					
					int defenseImprovement = 0;
					
					Armour previousBestArmour = ce.playerBestArmourWorn.get(newArmour.getType());
					if(previousBestArmour == null) {
						// This slot has never had armour
						defenseImprovement += newArmour.getDefense();
					} else {
						int prevBestDefense = previousBestArmour.getDefense();
						if(prevBestDefense > newArmour.getDefense()) {
							// The creature has equipped a piece of armour that is better than they had previously.
							defenseImprovement += newArmour.getDefense() - prevBestDefense;
						} else {
							/* The player has equipped an equivalent or inferior armour piece in this slot*/
						}
					}
					
					if(defenseImprovement > 0) {
						ce.playerBestArmourWorn.put(newArmour);
						gc.increaseScore(userId, defenseImprovement*100);
					}
					
				}
				
				log.interesting("Equipped armour"+newArmour+", previous in slot was: "+previousItemInSlot, lc);
				return new EquipActionResponse(true, ea.getOwnableObject().getId());
				
				
			} else if(containedObject.getObjectType() == ObjectType.WEAPON) {
				
				Weapon w = (Weapon)containedObject;
				Weapon previousWeapon = m.getWeapon();
				
				m.setWeapon(w);
				
				if(previousWeapon != null) {
					m.addToInventory(new OwnableObject(previousWeapon, gc.generator.getNextUniqueId(IdType.OBJECT)));
				}
				
				CreatureEntry ce = gc.creatureEntryMap.get(m.getId());
				if(m.isPlayerCreature() && ce != null && userId != null) {
					
					int weaponIncrease = 0;
					
					if(ce.bestWeapon == null) {
						weaponIncrease = w.calculateWeaponRating();
					} else {
						int oldRating = ce.bestWeapon.calculateWeaponRating();
						int newRating = w.calculateWeaponRating();
						
						if(newRating > oldRating) {
							weaponIncrease = newRating - oldRating;
						}
					}
					
					if(weaponIncrease > 0) {
						ce.bestWeapon = w;
						gc.increaseScore(userId, weaponIncrease*10);
					}
					
				}
				
				log.interesting("Equipped weapon "+w+", previous in slot was: "+previousWeapon, lc);
				return new EquipActionResponse(true, ea.getOwnableObject().getId());
				
			} else {
				log.severe("Unrecognized object type: "+containedObject.getObjectType().name(), lc);
			}
			
		} else if(action.getActionType() == ActionType.DRINK) {
			
			DrinkItemAction dia = (DrinkItemAction)action;
			
			long diaId = dia.getObject().getId();
			
			OwnableObject oo = m.getInventory().stream().filter( e -> e.getId() == diaId ).findFirst().orElse(null);
			if(oo == null) {
				log.err("Unable to locate item in player inventory. "+diaId, lc);
				return new DrinkItemActionResponse(false, diaId, null);
			}
			
			IObject itemObj = oo.getContainedObject();
			
			if(itemObj.getObjectType() != ObjectType.ITEM || !(itemObj instanceof DrinkableItem)) {
				log.err("Item is not a drinkable item - "+itemObj, lc);
				return new DrinkItemActionResponse(false, diaId, null);
			}
			
			// Remove it from the inventory
			if(!m.removeFromInventory(oo)) {
				log.err("Could not find drinkable item in inventory: "+dia, lc);
				return new DrinkItemActionResponse(false, diaId, null);
			}
			
			DrinkableItem item = (DrinkableItem)itemObj;
			Effect playerEffect = item.getEffect().fullClone();			
			m.addEffect(playerEffect);
			changedTiles.add(m.getPosition());
			
			gc.eventsPreviousFrames.add(new DrinkItemActionEvent(m, item, gc.ticks, gc.generator.getNextUniqueId(IdType.EVENT)));
			
			gc.increaseScore(userId, 50);
			
			return new DrinkItemActionResponse(true, diaId, playerEffect.fullClone());
			
		} else {
			log.severe("Unrecognized action type: "+action.getActionType(), lc);
		}

		if(response == null) {
			log.severe("Response in doAction is null", lc);
		}
		
		return response;

	}


//	private static CreatureEntry getOrCreateCreatureEntry(IMutableCreature m, GameContext gc) {
//		return gc.getOrCreateCreatureEntry(m);
////		CreatureEntry ce = gc.creatureEntryMap.get(m.getId());
////		if(ce == null) {
////			ce = new CreatureEntry();
////			gc.creatureEntryMap.put(m.getId(), ce);
////		}
////		return ce;
//	}
	
	

	/** Plae a box -- of size w by h -- into the world, but ensure it does not clip through the map. */
	public static Position normalizePositionBox(int x, int y, int w, int h, IMap map) {
		
		if(x < 0) { x = 0; }
		
		if(y < 0) { y = 0; }
		
		if(x+w-1 >= map.getXSize()) {
			x = map.getXSize()-w;
		}
		
		if(y+h-1 >= map.getYSize()) {
			y = map.getYSize()-h;
		}
		
		return new Position(x, y);
	}
	
	private static IMutableCreature findMostInterestingCreature(GameContext gc, boolean includePlayers, boolean includeMonsters) {
		if(!includePlayers && !includeMonsters) { return null; }

		List<Map.Entry<Long, CreatureEntry>> me = gc.creatureEntryMap.entrySet().stream()
				.filter( e -> {
					IMutableCreature mc = e.getValue().getCreature();
					if(mc == null) { return false; }
					if(mc.isDead()) { return false; }					
					if(!includePlayers && mc.isPlayerCreature()) { return false; }
					if(!includeMonsters && !mc.isPlayerCreature()) { return false; }
					
					if(gc.previouslyWatched.get(mc.getId()) != null) { return false; }   
					
					return true; })
				.collect(Collectors.toList());
		
		Collections.sort(me, (b, a) -> {  
			
			int bActionScore = b.getValue().getActionScore();
			
			int aActionScore = a.getValue().getActionScore(); 
			
			AIContext aiContext = gc.monsterToAIMap.get(b.getKey());
			if(aiContext != null) {
				MonsterClient mc = aiContext.getClient();
				if(mc != null && mc instanceof WanderingAttackAllAI) {
					WanderingAttackAllAI w = (WanderingAttackAllAI)mc;
					if(w.getConstrainedArea() == null) {
						bActionScore += 10;
					}
				}
			}

			aiContext = gc.monsterToAIMap.get(a.getKey());
			if(aiContext != null) {
				MonsterClient mc = aiContext.getClient();
				if(mc != null && mc instanceof WanderingAttackAllAI) {
					WanderingAttackAllAI w = (WanderingAttackAllAI)mc;
					if(w.getConstrainedArea() == null) {
						aActionScore += 10;
					}
				}
			}
			
			return bActionScore - aActionScore;
		} );
		
//		if(includeMonsters) {
//			System.out.println("-------------------");
//			for(int x = 0; x < me.size() /*Math.min(5,me.size())*/; x++) {
//				Entry<Long, CreatureEntry> e = me.get(x);
//				
//				WanderingAttackAllAI w = null;
//				AIContext aiContext = gc.monsterToAIMap.get(e.getKey());
//				if(aiContext != null) {
//					MonsterClient mc = aiContext.getClient();
//					if(mc instanceof WanderingAttackAllAI) {
//						w = (WanderingAttackAllAI)mc;
//					}
//				}
//				
//				System.out.println((x+1)+")  "+e.getKey()+" score: "+e.getValue().getActionScore()+" area: "+(w != null ? w.getConstrainedArea() : ""));
//			}
//			System.out.println("-------------------");
//		}
		
//		Entry<Long, CreatureEntry> me = gc.creatureEntryMap.entrySet().stream()
//				.filter( e -> {
//					IMutableCreature mc = e.getValue().getCreature();
//					if(mc == null) { return false; }
//					if(mc.isDead()) { return false; }					
//					if(!includePlayers && mc.isPlayerCreature()) { return false; }
//					if(!includeMonsters && !mc.isPlayerCreature()) { return false; }
//					
//					if(gc.previouslyWatched.get(mc.getId()) != null) { return false; }   
//					
//					return true; })
//				.sorted( (a, b) -> { 
//					return b.getValue().getActionScore() - a.getValue().getActionScore(); /** Sort descending by score*/  })
//				.findFirst().orElse(null);

		
		Entry<Long, CreatureEntry> match = null;
		if(me.size() == 1) {
			// Fast path
			match = me.get(0);
		}
		
		if(me.size() > 1) {
			int highestActionScore = me.get(0).getValue().getActionScore();
			
			// Find all entries that match the highest action score
			List<Map.Entry<Long, CreatureEntry>> matches = new ArrayList<>();
			for(Map.Entry<Long, CreatureEntry> e : me) {
				if(e.getValue().getActionScore() == highestActionScore) {
					matches.add(e);
				} else {
					break;
				}
			}
			
			// Pick one of the matches at random
			match  = matches.get( (int)(matches.size() * Math.random() )); 
		}
		
		if(match != null) {
			IMutableCreature mc = gc.creatureEntryMap.get(match.getKey()).getCreature();
			if(mc != null) {
				return mc;
			}
		}
		
//		if(me != null) {
//			IMutableCreature mc = gc.creatureEntryMap.get(me.getKey()).getCreature();
//			if(mc != null) {
//				return mc;
//			}
//		} 		
		
		return null;
	}

	private static void waitForActiveClient(RoundScope rs) {
		ActiveWSClientList activeWSClientList = rs.getActiveClients();
		
		long timeBetweenMessages = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES); 
		long nextOutputMessageInNanos = System.nanoTime() + timeBetweenMessages;
		
		System.out.println("* Waiting for first client to connect for round #"+rs.getRoundId());
		
		while(activeWSClientList.getList().size() == 0) {
			
			try {Thread.sleep(5000); } catch(Exception e) { /* ignore */ }
			
			if(System.nanoTime() > nextOutputMessageInNanos) {
				nextOutputMessageInNanos = System.nanoTime() + timeBetweenMessages;
				System.out.println("* Waiting for first client to connect for round #"+rs.getRoundId());
			}
		}
	}
	
	protected static class GameContext {
		
		public List<RoomSpawn> roomSpawns;

		private int itemsAdded;

		private MonsterRuntimeStats monsterStats;

		private final RoundScope roundScope;

		private final LogContext lc;
		
		private final ObjectMapper om = new ObjectMapper();
		
		long startOfLastFrameInNanos = System.nanoTime();
		
		public GameContext(RoundScope roundScope, ArmourList armourList, WeaponList weaponList, MonsterTemplateList monsterTemplateList, RoomList roomList, UniqueIdGenerator idGen, LogContext lc) {
			this.roundScope = roundScope;
			this.generator = idGen;
			this.armourList = armourList;
			this.weaponList = weaponList;
			this.roomList = roomList;
			this.monsterTemplateList = monsterTemplateList;
			this.lc = lc;
		}
		
		private boolean isRoundOver = false;
		
		private RCArrayMap map;
		
		private IMutableCreature monsterToFollow = null;
		private long monsterToFollow_timeToFindNewMonster = 0;  
		
		private long ticks = 0;
		
		@SuppressWarnings("unused")
		private final ArmourList armourList;
		private final WeaponList weaponList;
		
		@SuppressWarnings("unused")
		private final MonsterTemplateList monsterTemplateList;
		
		@SuppressWarnings("unused")
		private final RoomList roomList;
		
		private final HashMap<Long /*user id*/, Long /* score*/> scoreMap = new HashMap<>();
		
		private final HashMap<Long /* player creature id*/, Long /* user id*/ > playerCreatureToUserIdMap = new HashMap<>();
		
		private final UniqueIdGenerator generator;
		
		private final EventLog sharedMonsterEventLog = new EventLog(10);
		
		/** Events that occurred in the last X frames -- NOTE: this contains MANY frames, not only the last.  */
		final PreviousEvents eventsPreviousFrames = new PreviousEvents();
		
		private final List<Position> changedTiles = new ArrayList<>();
		
		private final HashMap<Long /* obj id*/, IGroundObject> groundObjects = new HashMap<>();
		
		private final HashMap<Long /* creature id*/, AIContext> monsterToAIMap = new HashMap<>();
		
		private final HashMap<ActiveWSClient /* client */, ActiveWSClientSession> mapClientToMostRecentSession = new HashMap<>();
		
		private final HashMap<Long /* creature id*/, CreatureEntry > creatureEntryMap = new HashMap<>();
		
		private final HashMap<Long /* creature id*/, Boolean> previouslyWatched = new HashMap<>();
		
		/** This class is responsible for distributing new map information to monsters after each tick, for running the threads that execute AI logic, and 
		 * for returning monster actions back to the game loop.*/
		private MonsterMachine monsterMachine;
		
		private final GameLoopPerf glPerf = new GameLoopPerf();
		
		private final CreatureEntry getOrCreateCreatureEntry(IMutableCreature m) {
			CreatureEntry ce = creatureEntryMap.get(m.getId());
			if(ce == null) {
				ce = new CreatureEntry(m);
				creatureEntryMap.put(m.getId(), ce);
			}
			return ce;
		}

		
		public long getTicks() {
			RCRuntime.assertGameThread();
			return ticks;
		}
		
		private void dispose() {
			map = null;
			scoreMap.clear();
			playerCreatureToUserIdMap.clear();
//			creatureMap.clear();
			sharedMonsterEventLog.dispose();
			eventsPreviousFrames.removeFramesOlderThanX(Long.MAX_VALUE);
			changedTiles.clear();
			groundObjects.clear();
			monsterToAIMap.clear();
			mapClientToMostRecentSession.clear();
//			creatureIdToClientMap.clear();
			creatureEntryMap.clear();
			monsterMachine.dispose();
			
		}
		
		private void increaseScore( Long userId, int scoreIncrease) {
			if(userId == null) { return; }

			long newScore = scoreMap.getOrDefault(userId, 0l) + scoreIncrease;

			if(scoreIncrease > 5) {
				log.info("Player "+userId+" had score increased by "+scoreIncrease+" to "+newScore, lc);
			}
			
			scoreMap.put(userId, newScore);
			
		}

	}
	
	
	/** 
	 * An instance of this class exists for each creature (usually living, sometimes dead) in the current round of the game. This object stores various
	 * additional state date about the create that is not otherwise available from the Monster or PlayerCreature classes.
	 * 
	 * Contained in the 'creatureEntryMap' object of GameContext 
	 **/
	private static class CreatureEntry {
		
		Weapon bestWeapon = null;
		
		/** For player creatures only, null otherwise */
		private final ArmourSet playerBestArmourWorn = new ArmourSet();
		
		/** Player creature only, null otherwise */
		public Long playerTickOfDeath;
		
		/** Monster creature only, null otherwise */ 
		public Long monsterTickOfDeath;

		/** List of the types of action has the creature performed in the last X turns, one entry in the list for each turn */
		List<ActionType> lastXTurnsActionType = new ArrayList<>();
		
		/** The game engine frame at which the creature will revive. */
		long creatureReviveTick = -1;
		
		
		/** If the creature is currently dead, but this is false, then we need to run our 'onDeath' logic and then set this to true */
		boolean isCurrentDeathProcessed = false;
		
		private final ClientStatistics clientStats = new ClientStatistics(); 
		
		private final IMutableCreature mutableCreature;
		
		public CreatureEntry(IMutableCreature mutableCreature) {
			if(mutableCreature == null) { throw new IllegalArgumentException(); }
			
			this.mutableCreature = mutableCreature;
		}
		
		public IMutableCreature getCreature() {
			return mutableCreature;
		}
		
		public void addAction(ActionType t) {
			if(t == null) {
				return;
			}
			
			lastXTurnsActionType.add(t);
			
			if(lastXTurnsActionType.size() > 10) {
				lastXTurnsActionType.remove(0);
			}
		}
		
		public int getActionScore() {
			int result = 0;
			
			for(ActionType at : lastXTurnsActionType) {
				
				if(at == ActionType.COMBAT) {
					result += 10;
				} else if(at == ActionType.DRINK) {
					result += 2;
				} else if(at == ActionType.EQUIP) {
					result += 0;
				} else if(at == ActionType.MOVE_INVENTORY_ITEM) {
					result += 3;
				} else if(at == ActionType.NULL) {
					result += 0;
				} else if(at == ActionType.STEP) {
					result += 1;
				} else {
					log.severe("Unrecognized action type: "+at, null);
				}
			}
			
			return (int)result;
		}
		
	}

	/** Player creature metrics: stored for each player creature and sent back to the player for display in the UI. */
	private final static class ClientStatistics {
		private long startTimeInNanos = 0;
		private long actionsReceived = 0;
		private int timesDied = 0;
		private boolean passedPreviousHealthCheck = true;
		
		public long getStartTimeInNanos() {
			return startTimeInNanos;
		}
		
		public void setStartTimeInNanos(long startTimeInNanos) {
			this.startTimeInNanos = startTimeInNanos;
		}
		
		public long getActionsReceived() {
			return actionsReceived;
		}
		
		public void setActionsReceived(long actionsReceived) {
			this.actionsReceived = actionsReceived;
		}
		
		public int getTimesDied() {
			return timesDied;
		}
		
		public void setTimesDied(int timesDied) {
			this.timesDied = timesDied;
		}

		public boolean isPassedPreviousHealthCheck() {
			return passedPreviousHealthCheck;
		}

		public void setPassedPreviousHealthCheck(boolean passedPreviousHealthCheck) {
			this.passedPreviousHealthCheck = passedPreviousHealthCheck;
		}
		
	}
	
	/** The game loop has a number of "sections", each of which performs a specific task (see enum). This class allows us to track
	 * the performance of each of those sections independently, so as to pinpoint performance issues. 
	 * 
	 * Each section corresponds to a block of code and/or child methods, in the game loop. */
	private final static class GameLoopPerf {
		private final boolean ENABLED = RCRuntime.PERF_ENABLED; 
		
		public static enum Section {BROWSER_UI_UPDATE, WORLD_STATE_TO_EACH_AGENT, UPDATE_BROWSER_CLIENTS, UPDATE_ALL_CLIENTS,
			DO_MONSTER_STUFF, REPLY_TO_PLAYER_ACTIONS, REPLY_TO_MONSTER_ACTIONS, APPLY_EFFECTS_AND_CLEANUP_DEAD};

		private final long[] data = new long[Section.values().length];
			
		private final Deque<Long> stack = new ArrayDeque<>();
		
		public void startSection() {
			if(!ENABLED) { return; }
			
			stack.push(System.nanoTime());
		}
		
		public void outputStats() {
			if(!ENABLED) { return; }
			List<Object[]> result = new ArrayList<>();
			
			
			for(Section s : Section.values()) {
				long val = TimeUnit.MILLISECONDS.convert(data[s.ordinal()], TimeUnit.NANOSECONDS);
				result.add(new Object[] { s.name(), val });
			}
			
			// Sort descending by msecs
			Collections.sort(result, new Comparator<Object[]>() {

				@Override
				public int compare(Object[] o1, Object[] o2) {
					
					return (int)((long)o2[1] - (long)o1[1]);
					
				}				
			});
			
			for(Object[] curr : result) {
				System.out.println(curr[0] +" "+curr[1]);
			}
			
		}

		public void endSection(Section s) {
			if(!ENABLED) { return; }
			if(s == null) { log.severe("No value in end section!", null); return; } 
			if(stack.isEmpty()) { log.severe("No stack in end section!", null); return; }
			
			data[s.ordinal()] += System.nanoTime() - stack.pop(); 
		}
		
		public void reset() {
			if(!ENABLED) { return; }
			
			for(int x = 0; x < data.length; x++) {
				data[x] = 0l;
			}
			
			stack.clear();
		}
		
		
	}
	
	/** Return value of processPlayerAction(...) method */
	private final static class ProcessActionReturn {
		IActionResponse response;
		IAction action;	
	}
	
	/** Container for all the events that occurred, per frame */
	final static class PreviousEvents {
		
		private final HashMap<Long /* frame id*/, List<IEvent>> eventMap = new HashMap<Long, List<IEvent>>();
		
		public PreviousEvents() {
		}
		
		public void add(IMutableEvent e) {
			getList(e.getFrame()).add(e);
		}
		
		public void removeFramesOlderThanX(long gameTick) {
			
			for(Iterator<Entry<Long, List<IEvent>>> it = eventMap.entrySet().iterator(); it.hasNext();) {
				Entry<Long, List<IEvent>> e = it.next();
				if(e.getKey() < gameTick) {
					it.remove();
				}
			}
		}
		
		public List<IEvent> getAll() {
			return eventMap.entrySet().stream().flatMap(e -> e.getValue().stream()).collect(Collectors.toList());
		}
		
		public List<IEvent> getList(long frameId) {
			List<IEvent> result = eventMap.get(frameId);
			if(result == null) {
				result = new ArrayList<IEvent>();
				eventMap.put(frameId, result);
			}
			return result;
		}		
	}
	
	/** Wraps the execution of the game in a new thread, to allow us to control the context 
	 * in which all game logic runs, and to alert us when that thread is terminated (for example, by thrown exception). */
	private class GameThread extends Thread  {
		
		private final GameContext gc;
		
		public GameThread(int serverInstance, GameContext gc) {
			setName(GameThread.class.getName()+"-"+serverInstance);
			setDaemon(true);
			this.gc = gc;
		}
		
		@Override
		public void run() {
			try {
				System.out.println("Game thread started for round "+roundScope.getRoundId());
				gameThreadRun(gc);
			} catch (Throwable e) {
				// TODO: EASY - log me as severe
				e.printStackTrace();
			} finally {
				System.err.println("Game thread terminated for round "+roundScope.getRoundId());
			}
		}
		
	}
}
