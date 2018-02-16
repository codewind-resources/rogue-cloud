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

package com.roguecloud.json;

import java.util.ArrayList;
import java.util.List;

public class JsonWorldState {
	int clientViewPosX;
	int clientViewPosY;

	int clientViewWidth;
	int clientViewHeight;
	
	int worldWidth;
	int worldHeight;
	
	int roundSecsLeft;

	private List<JsonDrinkableItem> drinkables = new ArrayList<>();
	private List<JsonWeapon> weapons = new ArrayList<>();
	private List<JsonArmour> armours = new ArrayList<>();
	
	private List<JsonViewFrame> frames = new ArrayList<>();

	private List<JsonVisibleCreature> visibleCreatures = new ArrayList<>();
	
	private List<JsonVisibleObject> visibleObjects = new ArrayList<>();
	
	private List<Object> tileProperties = new ArrayList<>();
	
	private List<Object> events = new ArrayList<>();
	
	public JsonWorldState() {
	}

	public void setDrinkables(List<JsonDrinkableItem> drinkables) {
		this.drinkables = drinkables;
	}

	public List<JsonDrinkableItem> getDrinkables() {
		return drinkables;
	}
	
	public int getClientViewPosX() {
		return clientViewPosX;
	}

	public void setClientViewPosX(int clientViewPosX) {
		this.clientViewPosX = clientViewPosX;
	}

	public int getClientViewPosY() {
		return clientViewPosY;
	}

	public void setClientViewPosY(int clientViewPosY) {
		this.clientViewPosY = clientViewPosY;
	}

	public int getClientViewWidth() {
		return clientViewWidth;
	}

	public void setClientViewWidth(int clientViewWidth) {
		this.clientViewWidth = clientViewWidth;
	}

	public int getClientViewHeight() {
		return clientViewHeight;
	}

	public void setClientViewHeight(int clientViewHeight) {
		this.clientViewHeight = clientViewHeight;
	}
	
	public List<JsonViewFrame> getFrames() {
		return frames;
	}
	
	public void setFrames(List<JsonViewFrame> frames) {
		this.frames = frames;
	}
	

	public List<JsonVisibleCreature> getVisibleCreatures() {
		return visibleCreatures;
	}

	public void setVisibleCreatures(List<JsonVisibleCreature> visibleCreatures) {
		this.visibleCreatures = visibleCreatures;
	}

	public List<JsonVisibleObject> getVisibleObjects() {
		return visibleObjects;
	}

	public void setVisibleObjects(List<JsonVisibleObject> visibleObjects) {
		this.visibleObjects = visibleObjects;
	}

	public List<JsonWeapon> getWeapons() {
		return weapons;
	}

	public void setWeapons(List<JsonWeapon> weapons) {
		this.weapons = weapons;
	}

	public List<JsonArmour> getArmours() {
		return armours;
	}

	public void setArmours(List<JsonArmour> armours) {
		this.armours = armours;
	}


	public List<Object> getTileProperties() {
		return tileProperties;
	}
	
	public void setTileProperties(List<Object> tileProperties) {
		this.tileProperties = tileProperties;
	}
	
	public void setWorldHeight(int worldHeight) {
		this.worldHeight = worldHeight;
	}
	
	public void setWorldWidth(int worldWidth) {
		this.worldWidth = worldWidth;
	}
	
	public int getWorldHeight() {
		return worldHeight;
	}
	
	public int getWorldWidth() {
		return worldWidth;
	}
	

	public List<Object> getEvents() {
		return events;
	}

	public void setEvents(List<Object> events) {
		this.events = events;
	}

	public int getRoundSecsLeft() {
		return roundSecsLeft;
	}

	public void setRoundSecsLeft(int roundSecsLeft) {
		this.roundSecsLeft = roundSecsLeft;
	}

	public static class JsonViewFrame {
		int x;
		int y;
		int w;
		int h;

		private List<Object> data = new ArrayList<>();
		
		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}

		public int getY() {
			return y;
		}

		public void setY(int y) {
			this.y = y;
		}

		public int getW() {
			return w;
		}

		public void setW(int w) {
			this.w = w;
		}

		public int getH() {
			return h;
		}

		public void setH(int h) {
			this.h = h;
		}

		public List<Object> getData() {
			return data;
		}
		
		public void setData(List<Object> data) {
			this.data = data;
		}
	}

}
