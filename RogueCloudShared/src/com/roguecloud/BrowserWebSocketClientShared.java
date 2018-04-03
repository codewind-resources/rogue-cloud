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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;

import com.roguecloud.creatures.ICreature;
import com.roguecloud.map.IMap;
import com.roguecloud.map.Tile;
import com.roguecloud.map.TileType;
import com.roguecloud.utils.Logger;

/**
 * This class generates a JSON representation of all or part of the game world map, for consumption by HTML/JS-based browser UI. 
 * The algorithm used by this class will, in certain cases, only send small sections of the world, rather than the full world, in order to save bandwidth/processing.
 *
 * This class is an internal class, for server use only.
 */
public class BrowserWebSocketClientShared {
	
	private static final Logger log = Logger.getInstance();

	/** This class/method is used by both the client and the server, and will be run on both the game thread and the client message thread. */
	
	public static String generateBrowserJson(/*EngineWebsocketState ews, */ int currClientWorldX, int currClientWorldY, int newWorldPosX, int newWorldPosY,
			int newWidth, int newHeight, IMap map, List<Position> changedTiles, List<ICreature> creatures, long ticks, JsonBuilderFactory factory) {

		JsonArrayBuilder frameData = factory.createArrayBuilder();
		
		JsonObjectBuilder outer = factory.createObjectBuilder()
				.add("frame", ticks)
				.add("currWorldPosX", newWorldPosX) // the world X coord of the top left hand tile, eg browser tile (0, 0)
				.add("currWorldPosY", newWorldPosY) // the world Y coord of the top left hand tile
				.add("currViewHeight", newHeight) // the height of the visible bounding box of information we are sending the client  
				.add("currViewWidth", newWidth) // the width of the visible bounding box of information we are sending the client 
				; 
		
		boolean fullThingSent = false;
		
		Map<Long, ICreature> creaturesSeen = new HashMap<Long, ICreature>();
		
		// Add all creatures in the view 

		if(creatures != null) {
			creatures.forEach( e -> {
				
				if(Position.containedInBox(e.getPosition(), newWorldPosX, newWorldPosY, newWidth, newHeight)) {
					creaturesSeen.put(e.getId(), e);
				}
			
			});
		} else {
			for(int y = newWorldPosY; y < newWorldPosY+newHeight; y++) {
				for(int x = newWorldPosX; x < newWorldPosX+newWidth; x++) {
					Tile t = map.getTile(x, y);
					
					if(t == null) { log.severe("Missing tile at "+x+" "+y, null); continue; }
					
					t.getCreatures().forEach( e -> {
						creaturesSeen.put(e.getId(), e);
					});
				}
			}			
		}
		
		if(newWorldPosX != currClientWorldX || newWorldPosY != currClientWorldY/* || newWidth != ssd.getCurrWidth() || newHeight != ssd.getCurrHeight()*/) {
		
			final int deltaX = newWorldPosX-currClientWorldX; // > 0 if the view has shifted right
			final int deltaY = newWorldPosY-currClientWorldY; // > 0 if the view has shifted down
			
			if(currClientWorldX == -1 || currClientWorldY == -1 || (Math.abs(deltaX) > 0 &&  Math.abs(deltaY) > 0) ) {
							
				// Send the whole thing
				JsonObjectBuilder frame = factory.createObjectBuilder()
						.add("x", 0 /* 0 because we are sending the full screen */)
						.add("y", 0 )
						.add("w", newWidth)
						.add("h", newHeight);
	
				{
					JsonArrayBuilder data = factory.createArrayBuilder();
		
					for(int y = newWorldPosY; y < newWorldPosY+newHeight; y++) {
						for(int x = newWorldPosX; x < newWorldPosX+newWidth; x++) {
							
							data = getAndConvertTile(x, y, map, data, factory, ticks);
//							Tile t = map.getTile(x, y);					
//							data = data.add(convertSingle(t));
							
						}
					}
					frame = frame.add("data", data);
				}
				
				frameData = frameData.add(frame);
				
				fullThingSent = true;
			} else {
								
				// Send only the new bits
				
				if(deltaX > 0) {

					JsonObjectBuilder frame = factory.createObjectBuilder()
							.add("x", newWidth-deltaX /*newWorldPosX*/)
							.add("y", 0 /*newWorldPosY*/)
							.add("w", deltaX)
							.add("h", newHeight);
		
					{
						JsonArrayBuilder data = factory.createArrayBuilder();
			
						for(int y = newWorldPosY; y < newWorldPosY+newHeight; y++) {
							for(int x = newWorldPosX+newWidth-deltaX; x < newWorldPosX+newWidth; x++) {

								data = getAndConvertTile(x, y, map, data, factory, ticks);
//								Tile t = map.getTile(x, y);		
//								data = data.add(convertSingle(t));
								
							}
						}
						frame = frame.add("data", data);
					}
					
					frameData = frameData.add(frame);
					
				} else if(deltaX < 0) {

					JsonObjectBuilder frame = factory.createObjectBuilder()
							.add("x", 0 /*newWorldPosX*/)
							.add("y", 0 /*newWorldPosY*/)
							.add("w", Math.abs(deltaX))
							.add("h", newHeight);
		
					{
						JsonArrayBuilder data = factory.createArrayBuilder();
			
						for(int y = newWorldPosY; y < newWorldPosY+newHeight; y++) {
							for(int x = newWorldPosX; x < newWorldPosX+Math.abs(deltaX); x++) {
								
								data = getAndConvertTile(x, y, map, data, factory, ticks);
//								Tile t = map.getTile(x, y);		
//								data = data.add(convertSingle(t));
								
							}
						}
						frame = frame.add("data", data);
					}
					
					frameData = frameData.add(frame);
					
				} else if(deltaY > 0) {
					
					JsonObjectBuilder frame = factory.createObjectBuilder()
							.add("x", 0 )
							.add("y", newHeight-deltaY)
							.add("w", newWidth)
							.add("h", deltaY);
		
					{
						JsonArrayBuilder data = factory.createArrayBuilder();
			
						for(int y = newWorldPosY+newHeight-deltaY; y < newWorldPosY+newHeight; y++) {
							for(int x = newWorldPosX; x < newWorldPosX+newWidth; x++) {
								
								data = getAndConvertTile(x, y, map, data, factory, ticks);
//								Tile t = map.getTile(x, y);		
//								data = data.add(convertSingle(t));
								
							}
						}
						frame = frame.add("data", data);
					}
					
					frameData = frameData.add(frame);
					
				} else if(deltaY < 0 ) {
					
					JsonObjectBuilder frame = factory.createObjectBuilder()
							.add("x", 0 )
							.add("y", 0)
							.add("w", newWidth)
							.add("h", Math.abs(deltaY));
					{
						JsonArrayBuilder data = factory.createArrayBuilder();
			
						for(int y = newWorldPosY; y < newWorldPosY+Math.abs(deltaY); y++) {
							for(int x = newWorldPosX; x < newWorldPosX+newWidth; x++) {

								data = getAndConvertTile(x, y, map, data, factory, ticks);
								
//								Tile t = map.getTile(x, y);		
//								data = data.add(convertSingle(t));
								
							}
						}
						frame = frame.add("data", data);
					}
					
					frameData = frameData.add(frame);
					
				} else {
					log.severe("Unexpected browser client branch", null);
				}
						
			}
			
		}
		
		if(fullThingSent) {
			outer = outer.add("fullSent", true);
		} else {
			outer = outer.add("fullSent", false);
		}
		
		if(!fullThingSent) {
			for(Position p : changedTiles) {
			
				if(!Position.containedInBox(p, newWorldPosX, newWorldPosY, newWidth, newHeight)) { continue; }
				
				JsonArrayBuilder data = factory.createArrayBuilder();
				
				JsonObjectBuilder frame = factory.createObjectBuilder().add("x", p.getX()-newWorldPosX)
					.add("y", p.getY()-newWorldPosY)
					.add("w", 1)
					.add("h", 1);
				
//				Tile t = map.getTile(p);
//				data = data.add(convertSingle(t));
				data = getAndConvertTile(p.getX(), p.getY(), map, data, factory, ticks);
				
				frame = frame.add("data", data);
				
				frameData = frameData.add(frame);
			}
		}
		

//		ews.setCurrClientWorldX(newWorldPosX);
//		ews.setCurrClientWorldY(newWorldPosY);
//		ews.setHeight(newHeight);
//		ews.setWidth(newWidth);
		
		outer = outer.add("frameData", frameData);

		final JsonArrayBuilder creatureArray = factory.createArrayBuilder();
		creaturesSeen.values().forEach( e -> {
			JsonObjectBuilder creature = factory.createObjectBuilder()
				.add("id", e.getId())
				.add("position", factory.createArrayBuilder().add(e.getPosition().getX()).add(e.getPosition().getY()))
				.add("hp", e.getHp())
				.add("maxHp",  e.getMaxHp());
			
			if(e.isPlayerCreature()) {
				creature = creature.add("username", e.getName());
			}
			
			creatureArray.add(creature);
		});
		
		outer = outer.add("creatures", creatureArray);


		String result = outer.build().toString();


		return result;
		
	}
	
	private static JsonArrayBuilder getAndConvertTile(int x, int y, IMap map, JsonArrayBuilder data, JsonBuilderFactory factory, long ticks) {
		
		Tile t = map.getTile(x, y);
		
//		t.getCreatures().forEach( e -> {
//			creaturesSeen.put(e.getId(), e);
//		});
		
		data = data.add(convertSingle(t, factory, ticks));
	
		return data;
	}
	
	
	private static JsonArrayBuilder convertSingle(Tile t, JsonBuilderFactory factory, long ticks) {
		JsonArrayBuilder outer = factory.createArrayBuilder();
		
		if(t == null) {
			throw new IllegalArgumentException();
		}
		
		// When there are multiple creatures on the same tile, alternate between them every 500 msecs.
		TileType[] ttArr = t.getTileTypeLayersForBrowserPresentation((int)(ticks/5));
		// TODO: LOWER - I have hardcoded an assumption of a tick rate of 100 msecs per frame for the above line.
		
//		// [
		
		for(int c = 0; c < ttArr.length; c++) {			
//			JsonArrayBuilder tileLayer = Json.createArrayBuilder();
			// [
			
			// For each tile layer
			TileType tt = ttArr[c];
		
			if(tt.getRotation() != 0) {
				// [ number, rotation], 
				outer = outer.add(
						factory.createArrayBuilder().add(tt.getNumber()).add(tt.getRotation())
				);
			} else{
				// [ number],
				outer = outer.add(
						factory.createArrayBuilder().add(tt.getNumber())
				);
			}
			
			// ]
//			outer = outer.add(tileLayer);
		}
		
		// ]
		return outer;
		
	}	
}
