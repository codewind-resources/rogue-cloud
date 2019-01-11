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
package com.roguecloud.json.browser;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.roguecloud.json.JsonAbstractTypedMessage;

@JsonInclude(Include.NON_NULL)
public class JsonUpdateBrowserUI extends JsonAbstractTypedMessage {

	public static final String TYPE = "JsonUpdateBrowserUI";

	public JsonUpdateBrowserUI() {
		setType(TYPE);
	}

	private List<String> newEventHtml = null;

	private Long currentPlayerScore = null;
	private Long currentPlayerRank = null;
	
	private Long currentPlayerBestTotalRank = null;
	private Long currentPlayerBestTotalScore = null;

	private List<JsonScore> currentRoundScores = null;

	private List<JsonServiceStatEntry> stats = null;

	private List<JsonBrowserCombatEvent> combatEvents = null;
	
	private List<JsonItem> inventoryItems = null;
	
	private List<JsonItem> equipment = null;

	/** Instance of either JsonActiveRoundInfo or JsonInactiveRoundInfo */
	private Object roundState = null;
	
	private long gameTicks = -1;
	
	// private List<CombatEvent>
	
	public Object getRoundState() {
		return roundState;
	}
	
	public void setRoundState(Object roundState) {
		this.roundState = roundState;
	}

	public List<String> getNewEventHtml() {
		return newEventHtml;
	}

	public void setNewEventHtml(List<String> newEventHtml) {
		this.newEventHtml = newEventHtml;
	}

	public Long getCurrentPlayerScore() {
		return currentPlayerScore;
	}

	public void setCurrentPlayerScore(Long currentPlayerScore) {
		this.currentPlayerScore = currentPlayerScore;
	}

	public Long getCurrentPlayerRank() {
		return currentPlayerRank;
	}

	public void setCurrentPlayerRank(Long currentPlayerRank) {
		this.currentPlayerRank = currentPlayerRank;
	}

	public List<JsonScore> getCurrentRoundScores() {
		return currentRoundScores;
	}

	public void setCurrentRoundScores(List<JsonScore> currentRoundScores) {
		this.currentRoundScores = currentRoundScores;
	}

	public List<JsonServiceStatEntry> getStats() {
		return stats;
	}

	public void setStats(List<JsonServiceStatEntry> stats) {
		this.stats = stats;
	}

	public List<JsonBrowserCombatEvent> getCombatEvents() {
		return combatEvents;
	}

	public void setCombatEvents(List<JsonBrowserCombatEvent> combatEvents) {
		this.combatEvents = combatEvents;
	}

	public void setGameTicks(long gameTicks) {
		this.gameTicks = gameTicks;
	}
	
	public long getGameTicks() {
		return gameTicks;
	}

	@JsonIgnore
	public boolean utilContainsAnyData() {
		
		if(roundState != null) {
			return true;
		}
		
		if(combatEvents != null && combatEvents.size() > 0) {
			return true;
		}

		if (this.stats != null && this.stats.size() > 0) {
			return true;
		}

		if (newEventHtml != null && newEventHtml.size() > 0) {
			return true;
		}
		
		if(currentPlayerBestTotalRank != null || currentPlayerBestTotalScore != null) {
			return true;
		}
		
		if (currentPlayerScore != null) {
			return true;
		}
		if (currentPlayerRank != null) {
			return true;
		}
		if (currentRoundScores != null && currentRoundScores.size() == 0) {
			return true;
		}

		return false;
	}

	public Long getCurrentPlayerBestTotalRank() {
		return currentPlayerBestTotalRank;
	}

	public void setCurrentPlayerBestTotalRank(Long currentPlayerBestTotalRank) {
		this.currentPlayerBestTotalRank = currentPlayerBestTotalRank;
	}

	public Long getCurrentPlayerBestTotalScore() {
		return currentPlayerBestTotalScore;
	}

	public void setCurrentPlayerBestTotalScore(Long currentPlayerBestTotalScore) {
		this.currentPlayerBestTotalScore = currentPlayerBestTotalScore;
	}


	public static class JsonServiceStatEntry {
		double actionsPerSecond;
		int numberOfTimesDied;
		long averageTimeBetweenActions;
		String username;
		boolean passLastHealthCheck = true;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public double getActionsPerSecond() {
			return actionsPerSecond;
		}

		public void setActionsPerSecond(double actionsPerSecond) {
			this.actionsPerSecond = actionsPerSecond;
		}

		public int getNumberOfTimesDied() {
			return numberOfTimesDied;
		}

		public void setNumberOfTimesDied(int numberOfTimesDied) {
			this.numberOfTimesDied = numberOfTimesDied;
		}

		public long getAverageTimeBetweenActions() {
			return averageTimeBetweenActions;
		}

		public void setAverageTimeBetweenActions(long averageTimeBetweenActions) {
			this.averageTimeBetweenActions = averageTimeBetweenActions;
		}

		public boolean isPassLastHealthCheck() {
			return passLastHealthCheck;
		}

		public void setPassLastHealthCheck(boolean passLastHealthCheck) {
			this.passLastHealthCheck = passLastHealthCheck;
		}
		
		

	}

	public static class JsonBrowserCombatEvent {
		long x;
		long y;
		long frame;
		long damage;
		
		public JsonBrowserCombatEvent() {
		}
		
		public JsonBrowserCombatEvent(long x, long y, long frame, long damage) {
			this.x = x;
			this.y = y;
			this.frame = frame;
			this.damage = damage;
		}

		public long getX() {
			return x;
		}

		public void setX(long x) {
			this.x = x;
		}

		public long getY() {
			return y;
		}

		public void setY(long y) {
			this.y = y;
		}

		public long getFrame() {
			return frame;
		}

		public void setFrame(long frame) {
			this.frame = frame;
		}

		public long getDamage() {
			return damage;
		}

		public void setDamage(long damage) {
			this.damage = damage;
		}

	}

	public static class JsonScore {
		String username;
		long score;
		long rank;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public long getScore() {
			return score;
		}

		public void setScore(long score) {
			this.score = score;
		}

		public long getRank() {
			return rank;
		}

		public void setRank(long rank) {
			this.rank = rank;
		}

	}
	
	public static class JsonItem {
		String name;
		int quantity;
		
		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		public JsonItem() {
		}
		
		public JsonItem(String name, int quantity) {
			this.name = name;
			this.quantity = quantity;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		
	}
	
	public List<JsonItem> getInventory() {
		return inventoryItems;
	}

	public void setInventory(List<JsonItem> inventory) {
		this.inventoryItems = inventory;
	}
	
	public List<JsonItem> getEquipment() {
		return equipment;
	}

	public void setEquipment(List<JsonItem> inventory) {
		this.equipment = inventory;
	}
	
}
