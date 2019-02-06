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

'use strict';

function canvasJs(spriteSize, myCanvas, consoleDomElement, leaderboardDomElement, metricsDomElement, inventoryDomElement, equipmentDomElement, viewType, optionalUuid) {

// If adjusting this, then you will need to adjust the 'minimumElapsed' algorithm too
var LERP_CONSTANT = 5;

var viewTypeParam = viewType;
	
console.log("View type is "+viewType+" optionalUuid is "+optionalUuid);

// secondaryCanvas/secondaryCtx are a copy of the last frame that was drawn to the screen BEFORE
// the damage values, HP indicators, and usernames were drawn. This copy can then be used to refresh the 
// screen without a full redraw.  Thse variables are only used when the view type is SERVER_VIEW_WORLD.
var secondaryCanvas = document.createElement("canvas"),
	secondaryCtx = secondaryCanvas.getContext("2d");

var keyFrameBufferCanvas = document.createElement("canvas"),
	keyFrameBufferCtx = keyFrameBufferCanvas.getContext("2d");
	
//  Whether secondaryCtx contains frame data (this is always true after the first frame is drawn)
var secondaryCtxDrawn = false;
{
	secondaryCtx.canvas.width = myCanvas.width;
	secondaryCtx.canvas.height = myCanvas.height;

	keyFrameBufferCtx.canvas.width = 80*32;
	keyFrameBufferCtx.canvas.height = 40*32;
}

var currCanvasWidth = myCanvas.width;
var currCanvasHeight = myCanvas.height;

addCanvasListeners();

var globalState;

////Start game theme sound
//var gameSound = new sound("resources/game-theme.mp3");
//gameSound.play();


// Create and define globalState object.
{
	{
		globalState =  {};
		
		//The current world pixel position of the top left hand corner of the sliding view
		//(after the current interpolation has completed)
		globalState.lerpViewCurrPosX_pixels = -1;
		globalState.lerpViewCurrPosY_pixels = -1;
		
		globalState.playerCurrHp = -1;
		globalState.playerMaxHp = -1;
		
		// globalState.dirtyRedraw = null;
		
		// True if he have drawn at least one frame to the screen, false otherwise. (Used for misc init) 
		globalState.firstFrameDrawn = false;		
		
		globalState.viewType = viewType;
		globalState.damageGradient = generateDamageGradient();
		globalState.globalTileMap = new Map(/* tile number -> tile name */);
		globalState.imagesLoaded = false;
		
		for(var x = 0; x < GLOBAL_TILES_JSON.length; x++) {
			globalState.globalTileMap.set(GLOBAL_TILES_JSON[x][0], GLOBAL_TILES_JSON[x][1]);
		}

		globalState.nextFrameId = -1;

		globalState.lerpQueue = [];
		
		globalState.spriteSize = spriteSize;

		// The world (x,y) tile coords for the top-left-hand-side of the canvas
		globalState.startX = null;
		globalState.startY = null;

		
		// The world (x, y) tile coords for the top-left-hand-side of the 80x40 agent data view 
		// - from json property 'currWorldPosX/Y' of browser update json	
		globalState.currWorldX = -1;
		globalState.currWorldY = -1;

		globalState.waitingCombatEvents = [];

		globalState.frameQueue = [];
		
		globalState.entityList = [];
		
		globalState.prevDataMap = new Map( /* x*y coord -> num+rot */);

		globalState.dataArray = null;
		
		globalState.imageMap = new Map( /* num -> img */);

		// Primary canvas context	
		globalState.ctx = myCanvas.getContext("2d");

		globalState.keyFrameBufferCtx = keyFrameBufferCtx;
		
		// Absolute time of last frame draw in milliseconds
		globalState.lastFrameDrawTime = 0;

		// Absolute time of last lerp frame draw in milliseconds
		globalState.lerpFrameDrawTime = 0;

		// Tile are scaled to size before they are drawn to the map, but scaling is
		// a CPU intensive process, so we cache each scale down in this map.
		// Used by drawRotatedImage(...)
		globalState.scaleCache = new Map( /* tile number -> { "imgdata" : Image{ (...) } } */);

		// offscreenCanvas and offscreenContext are used by drawRotatedImage(...) to draw and cache scaled-down sprites
		globalState.offscreenCanvas = document.createElement('canvas');
		globalState.offscreenContext = globalState.offscreenCanvas.getContext('2d');
		{
			globalState.offscreenCanvas.width = spriteSize; 
			globalState.offscreenCanvas.height = spriteSize; 
		}

		globalState.leaderboardData = {
			"currRound" : 0,
			"timeleft" : "",
			"yourscore" : "",
			"leaderboard" : "",
			"leaderboard_list" : "",
			"overall" : "",
			"stats" : "",
			"leaderboard_curr_winner" : ""
		};
		
		globalState.console = new Array();
		
		globalState.mouseMoveEnabled = true;

	}

	createWebSocketAndSetInGlobalState(optionalUuid);
	
}

function createWebSocketAndSetInGlobalState(optionalUuid) {
	console.log("pre?");
	try {
		
		let loc = window.location, new_uri;
		if (loc.protocol === "https:") {
		    new_uri = "wss:";
		} else {
		    new_uri = "ws:";
		}
		new_uri += "//" + loc.host;
		new_uri += loc.pathname + "/../api/browser";
				
		globalState.gameSocket = new WebSocket(new_uri);

		globalState.wsInterval = setInterval( function() {
			
			if(globalState.ctx == null) { return; }
			
			if(globalState.gameSocket.readyState != 1) {
				clearInterval(globalState.wsInterval);
				console.log("ready state: "+globalState.gameSocket.readyState);				
				reestablishConnection();

			}

		}, 5000);
	} catch(err) {
		console.log(err);
		// reestablishConnection();
		return;
	}
	console.log("post?");
	
	globalState.gameSocket.onopen = function() {

		if(globalState.gameSocket == null) {
			return;
		}
		
		if(globalState.ctx == null) { globalState.gameSocket.close(); }
		
		var browserConnect = {
			"type" : "JsonBrowserConnect",
			"uuid" : optionalUuid == null ? generateUuid() : optionalUuid, /* GLOBAL_UUID,*/
			"username" : GLOBAL_USERNAME,
			"password" : GLOBAL_PASSWORD,
			"viewType" : viewTypeParam
			/* "viewOnly" : true*/
		}
		
		globalState.gameSocket.onclose = function() {
			console.log("on close called.");

			// Return if we've already disposed.
			if(globalState.ctx == null) { return; }
		
			// reestablishConnection();
		}
		
		var browserConnectJson = JSON.stringify(browserConnect);
		globalState.gameSocket.send(browserConnectJson);
		console.log("open: "+browserConnectJson);
	}

	globalState.gameSocket.onerr = function(event) {
		console.log("An error occurred: "+event)
		
		// Return if we've already disposed.
		// reestablishConnection();		
	}
	
	globalState.gameSocket.onmessage = function(event) {
		
		var json = jQuery.parseJSON(event.data);
		
		if(json.type == "JsonUpdateBrowserUI") {
		
			processUpdateJsonBrowserUI(json);

			return;
		} 
		
		if(globalState.nextFrameId == -1) {
			let frameJson = json;
			globalState.nextFrameId = frameJson.frame;
		}
		
		if(!globalState.imagesLoaded) {
			console.log("loadAndCall called.");
			loadAndCall(event, receiveFrameData);
		} else {
			receiveFrameData(event);
		}
		
	}	
}

function reestablishConnection() {
	console.log("reestablishConnection - in.");
	if(globalState.ctx == null) { return; }
	
	var viewTypeLocal = viewTypeParam;
	console.log("reestablish: "+viewTypeLocal);
	
	disposeGlobalState();
	console.log("reestablishConnection - setting timeout.");
	setTimeout(function() {
		canvasJs(spriteSize, myCanvas, consoleDomElement, leaderboardDomElement, metricsDomElement, inventoryDomElement, equipmentDomElement, viewTypeLocal, optionalUuid);
		// canvasJs(spriteSize, myCanvas, consoleDomElement, leaderboardDomElement, metricsDomElement, viewTypeLocal, optionalUuid);
	}, 200);
	if(globalState.gameSocket != null) {
		globalState.gameSocket.close();
	}
}

function processUpdateJsonBrowserUI(json) {

	if(json.newEventHtml != null) {
		for(let x = 0; x < json.newEventHtml.length; x++) {
			globalState.console.unshift(json.newEventHtml[x]);		
		}
	}
	if(consoleDomElement != null) {
		updateConsoleUI(globalState.console, consoleDomElement);
	}
	
	if(leaderboardDomElement != null) {
		updateLeaderboardUI(leaderboardDomElement, json, globalState.leaderboardData);
	}
	
	if(inventoryDomElement != null) {
		updateInventoryUI(inventoryDomElement, json);
	}
	
	if(equipmentDomElement != null) {
		updateEquipmentUI(equipmentDomElement, json);
	}
	
	if(json.combatEvents != null && json.combatEvents.length > 0) {
			
		// Add all the combat events to a list
		var entityList = [];
		for(let c = 0; c < json.combatEvents.length; c++) {
			let entry = json.combatEvents[c];
			let spread = SPREAD[Math.floor(SPREAD.length * Math.random())]

			let magnitudeSpriteSize = spriteSize > 10 ? spriteSize : 5;
			
			let magnitude = Math.floor(7 * (magnitudeSpriteSize/22));
			
			let newEntry = {
				worldx : entry.x,
				worldy : entry.y,
				xoffset : 0,
				yoffset: 0,
				frame : 0,
				directionX : magnitude * spread[0],
				directionY : magnitude * spread[1],
				damage : entry.damage
			};
			
			entityList.push(newEntry);
		}
		
		// Combat events should only be processed when frame #(json.gameTicks) is displayed, so add the events
		// to a global list, and only process them when the frame is drawn.
		if(json.gameTicks >= 0) {
			globalState.waitingCombatEvents.push( { "ticks" :  json.gameTicks, "entityList" : entityList } );
		}

	}	
}




globalState.interval = setInterval( function() {
	
	let INTERVAL_DEBUG = false;
	
	if(globalState.lerpQueue.length > 0 && globalState.viewType != "SERVER_VIEW_WORLD") {

		// All lerp frames together have at most 100 msec budget (due to 10 game-server-ticks per second)

		// minimum time between lerp frames in msecs
		let minimumElapsed = 20;

		let fqLength = globalState.frameQueue.length;

		if(fqLength < 10) {
			minimumElapsed -= Math.floor(fqLength/1.2); 
		} else if(fqLength < 20) {
			minimumElapsed -= 13
		} else if(fqLength < 30) {
			minimumElapsed -= 16
		} else if(fqLength < 40) {
			minimumElapsed -= 18
		} else {
			minimumElapsed -= 20;
		}
		
		if(minimumElapsed < 0) { minimumElapsed = 0; }

		// If not enough time has elapsed since we last drew a frame, then return.
		if(window.performance.now() - globalState.lerpFrameDrawTime < minimumElapsed ) {
			return;
		}

		if(INTERVAL_DEBUG) { console.log("lerp queue: "+globalState.lerpQueue.length+" fq: "+globalState.frameQueue.length+" "+minimumElapsed); }

		let entry = globalState.lerpQueue[0];
		
		globalState.lerpQueue.splice(0, 1);

		globalState.lerpFrameDrawTime = window.performance.now();

		if(INTERVAL_DEBUG) {
			let xCoord_worldPixels = globalState.lerpViewCurrPosX_pixels + entry.x;
			let yCoord_worldPixels = globalState.lerpViewCurrPosY_pixels + entry.y;
			console.log(`world pix: (${xCoord_worldPixels}, ${yCoord_worldPixels})`)
		}

		let ex = entry.x, ey = entry.y;
		if(entry.x + myCanvas.width > keyFrameBufferCanvas.width) {

			if(INTERVAL_DEBUG) { console.log("adapted high"); }
			
			ex = keyFrameBufferCanvas.width - myCanvas.width;  
		}
		if(entry.y + myCanvas.height > keyFrameBufferCanvas.height) {
			if(INTERVAL_DEBUG) { console.log("adapted high"); }
			ey = keyFrameBufferCanvas.height - myCanvas.height;  
		} 
		
		if(ex < 0) { ex = 0; if(INTERVAL_DEBUG) { console.log("adapted low"); } }
		if(ey < 0) { ey = 0; if(INTERVAL_DEBUG) { console.log("adapted low"); } }
		
		let ctx = globalState.ctx;
		ctx.drawImage(keyFrameBufferCanvas, ex, ey, myCanvas.width, myCanvas.height, 0, 0, myCanvas.width, myCanvas.height);

		// Draw frame rate
		ctx.fillStyle="white";	
		ctx.font = '15px sans-serif';
		ctx.fillText(globalState.nextFrameId - 1, 40, 40);

		
		// Draw player health at bottom of canvas
		if(globalState.playerCurrHp != -1 && globalState.playerMaxHp != -1){
			let percent = Math.max(0, globalState.playerCurrHp) / globalState.playerMaxHp;
			let percentIndex = Math.max( 0, Math.min(99, 100*percent));
			
			// Draw colour part of damage bar
			let offset = (myCanvas.width/5);
			ctx.fillStyle=globalState.damageGradient[Math.floor(percentIndex)]; 
			ctx.fillRect(offset, myCanvas.height-50, (myCanvas.width-(2*offset))*percent, 24);
			
			// Draw grey part of damage bar
			ctx.fillStyle="#666666";
			ctx.fillRect((myCanvas.width-(2*offset))*percent+offset, myCanvas.height-50, (myCanvas.width-(2*offset))*(1-percent), 24);
			ctx.font = "20px Arial";
			ctx.fillStyle = "white";
			ctx.textAlign = "center";
			let displayHealth = Math.max(0, globalState.playerCurrHp)+" / "+globalState.playerMaxHp;
			ctx.fillText(displayHealth.trim(),myCanvas.width/2,myCanvas.height-30);
			ctx.restore();
			
		}
		
		return;
	}

	var frameQueue = globalState.frameQueue;

	if(INTERVAL_DEBUG) { console.log("fq length: "+frameQueue.length); }

	if(globalState.viewType == "SERVER_VIEW_WORLD") {
		 // Depending on how far behind we are in drawing the latest frames, we wait between 30 and 100 msecs.
		 let minimumElapsed = 30+7*Math.max(0, (10-frameQueue.length));	
		 if(window.performance.now() - globalState.lastFrameDrawTime < minimumElapsed ) {
			 return;
		 }
	}
	
	while(true) {
	
		var frameFound = false;
		
		var lowestFrame = Number.MAX_VALUE;
		for(var x = 0; x < frameQueue.length; x++) {
			
			var currFrameNum = frameQueue[x].frame;
			
			if(currFrameNum < lowestFrame && currFrameNum >= globalState.nextFrameId) {
				lowestFrame = currFrameNum;
			}
		}
		
		if(lowestFrame == Number.MAX_VALUE) {
			return;
		}
		
		for(var x = 0; x < frameQueue.length; x++) {
			
			if(frameQueue[x].frame < lowestFrame) {

				frameQueue.splice(x, 1);
				x--;
				
			} else if(frameQueue[x].frame == lowestFrame) {
				globalState.nextFrameId = lowestFrame+1;

				// For any combat events from JsonUpdateBrowserUI, add them to our global entities list.
				moveEntitiesForFrame(frameQueue[x].frame);
				
				processAndDrawNewFrame(frameQueue[x],  /*globalState.nextFrameId % 10 != 0 &&*/ frameQueue.length > 10  );

				if(INTERVAL_DEBUG) {  console.log("nonlerp queue: "+globalState.lerpQueue.length+" fq: "+globalState.frameQueue.length); }
				
				frameQueue.splice(x, 1);
				frameFound = true;
				globalState.lastFrameDrawTime = window.performance.now();

				return;
			}	
		}
		
				
		if(!frameFound) {
			return;
		}
	}
}, 2);


/** Look in waitingCombatEvents for any events that apply to the current frame ('currFrameTicks' param), 
 * then add those matches to the global entity list (and remove them from waitingCombatEvents). */
function moveEntitiesForFrame(currFrameTicks) {
			
	var elements = globalState.waitingCombatEvents;
	
	let matchFound = false;
	
	for(var i = elements.length -1; i >= 0 ; i--){
						
		if(elements[i].ticks == currFrameTicks ){
							
			elements[i].entityList.forEach( function(x) {
				globalState.entityList.push(x);
				matchFound = true;
			});
		} 
						
		if(elements[i].ticks <= currFrameTicks ){
	        elements.splice(i, 1);
	    }
	}
	
	if(matchFound) {
		// play combat sound
		// var combatSound = new sound("resources/spawn.mp3");
		// combatSound.play();
	}
					
}


function receiveFrameData(event) {
	
	var frameJson = jQuery.parseJSON(event.data);			
	globalState.frameQueue.push(frameJson);
	
	if(globalState.frameQueue.length > 100) {

		// TODO: What is this?

		console.log("Closing!");
		globalState.gameSocket.close();
	}

}

function convertWorldPixelsToPixelsInImage(posX_worldCoord, posY_worldCoord, currWorldX, currWorldY, currViewWidth, currViewHeight, targetWidth, targetHeight, spriteSize) {

	// Find the center point of the agent view
	let centerPointOfFullViewX = Math.floor(currViewWidth/2)+currWorldX;
	let centerPointOfFullViewY = Math.floor(currViewHeight/2)+currWorldY;

	// Use the above center point to calculate the top left corner of the (smaller) browser view
	let topLeftOfBrowserViewX_worldCoords = centerPointOfFullViewX - Math.floor(targetWidth/2);	
	let topLeftOfBrowserViewY_worldCoords = centerPointOfFullViewY - Math.floor(targetHeight/2);

	let topLeftOfBrowserViewX_canvasCoords = topLeftOfBrowserViewX_worldCoords - currWorldX;
	let topLeftOfBrowserViewY_canvasCoords = topLeftOfBrowserViewY_worldCoords - currWorldY;

	let posX_canvasCoords = posX_worldCoord + 32 * (0 - Math.floor(currViewWidth/2) + Math.floor(targetWidth/2) - currWorldX  +  topLeftOfBrowserViewX_canvasCoords);
	let posY_canvasCoords = posY_worldCoord + 32 * (0 - Math.floor(currViewHeight/2) + Math.floor(targetHeight/2) - currWorldY  +  topLeftOfBrowserViewY_canvasCoords);
	
	return [posX_canvasCoords, posY_canvasCoords];
}



function convertWorldCoordsToPixelsInImage(posX_worldCoord, posY_worldCoord, currWorldX, currWorldY, currViewWidth, currViewHeight, targetWidth, targetHeight, spriteSize) {

	// Find the center point of the agent view
	let centerPointOfFullViewX = Math.floor(currViewWidth/2)+currWorldX;
	let centerPointOfFullViewY = Math.floor(currViewHeight/2)+currWorldY;

	// Use the above center point to calculate the top left corner of the (smaller) browser view
	let topLeftOfBrowserViewX_worldCoords = centerPointOfFullViewX - Math.floor(targetWidth/2);	
	let topLeftOfBrowserViewY_worldCoords = centerPointOfFullViewY - Math.floor(targetHeight/2);

	let topLeftOfBrowserViewX_canvasCoords = topLeftOfBrowserViewX_worldCoords - currWorldX;
	let topLeftOfBrowserViewY_canvasCoords = topLeftOfBrowserViewY_worldCoords - currWorldY;

	let posX_canvasCoords = posX_worldCoord - Math.floor(currViewWidth/2) + Math.floor(targetWidth/2) - currWorldX  +  topLeftOfBrowserViewX_canvasCoords;
	let posY_canvasCoords = posY_worldCoord - Math.floor(currViewHeight/2) + Math.floor(targetHeight/2) - currWorldY  +  topLeftOfBrowserViewY_canvasCoords;
	
	return [posX_canvasCoords * spriteSize, posY_canvasCoords * spriteSize ];
}


/** This is called when there is a new frame drawn */
function createEntry(currWorldX, currWorldY, prevWorldX, prevWorldY, targetWidth, targetHeight, currViewWidth, currViewHeight, spriteSize) {

	// The browser top left coorner in world coords
	// Absent the lerp algorithm, this is what the browser would be showing
	let naturalXPos_worldCoords, naturalYPos_worldCoords;
	{
		// Find the center point of the agent view
		let centerPointOfFullViewX = Math.floor(currViewWidth/2)+currWorldX;
		let centerPointOfFullViewY = Math.floor(currViewHeight/2)+currWorldY;
	
		// Use the above center point to calculate the top left corner of the (smaller) browser view
		let topLeftOfBrowserViewX_worldCoords = centerPointOfFullViewX - Math.floor(targetWidth/2);
		let topLeftOfBrowserViewY_worldCoords = centerPointOfFullViewY - Math.floor(targetHeight/2);

		naturalXPos_worldCoords = topLeftOfBrowserViewX_worldCoords;
		naturalYPos_worldCoords = topLeftOfBrowserViewY_worldCoords;

	}

	if(globalState.lerpViewCurrPosX_pixels == -1 || globalState.lerpViewCurrPosY_pixels == -1) {

		globalState.lerpViewCurrPosX_pixels = naturalXPos_worldCoords * spriteSize;
		globalState.lerpViewCurrPosY_pixels = naturalYPos_worldCoords * spriteSize; 

	}
	
	let lerpViewCurrPosX_pixels = globalState.lerpViewCurrPosX_pixels;
	let lerpViewCurrPosY_pixels = globalState.lerpViewCurrPosY_pixels;
	


	// The worst case scenario is the character constantly moving to the bottom of the screen, in which case we have this many frames:
	// 1280 vs 800 (1280 - 800 = 400)
	// This is the difference in pixels between the browser size and the agent size

	// in the worst case scenario, the user is moving 320 pixels a second down.

	let distanceWeNeedToCloseX_worldPixels = (32 * naturalXPos_worldCoords) - lerpViewCurrPosX_pixels;
	let distanceWeNeedToCloseY_worldPixels = (32 * naturalYPos_worldCoords) - lerpViewCurrPosY_pixels;

	//	console.log(`distance in world pixels: (${distanceWeNeedToCloseX_worldPixels}, ${distanceWeNeedToCloseY_worldPixels}) `);
	
	let closeInHowManyFrames;
	
	{
		var absDistanceWeNeedToCloseY_worldPixels = Math.abs(distanceWeNeedToCloseY_worldPixels);
		
		absDistanceWeNeedToCloseY_worldPixels = Math.min(absDistanceWeNeedToCloseY_worldPixels, 320); // x <= 320
		absDistanceWeNeedToCloseY_worldPixels = Math.max(absDistanceWeNeedToCloseY_worldPixels, 0); // x >= 0
		
		closeInHowManyFrames = 12 - Math.max(0, Math.min(10, absDistanceWeNeedToCloseY_worldPixels/45));
	}
		
	// console.log("distance: "+ distanceWeNeedToCloseY_worldPixels+" " +closeInHowManyFrames);
	
	let srcX_worldPixels = lerpViewCurrPosX_pixels;
	let srcY_worldPixels = lerpViewCurrPosY_pixels;

	let destX_worldPixels = lerpViewCurrPosX_pixels + Math.ceil(distanceWeNeedToCloseX_worldPixels / closeInHowManyFrames);
	let destY_worldPixels = lerpViewCurrPosY_pixels + Math.ceil(distanceWeNeedToCloseY_worldPixels / closeInHowManyFrames);
	

	var src_pixels = convertWorldPixelsToPixelsInImage(srcX_worldPixels, srcY_worldPixels, currWorldX, currWorldY, currViewWidth, currViewHeight, targetWidth, targetHeight, spriteSize)
	var dest_pixels = convertWorldPixelsToPixelsInImage(destX_worldPixels, destY_worldPixels, currWorldX, currWorldY, currViewWidth, currViewHeight, targetWidth, targetHeight, spriteSize)

	let result = interpolateCoords( src_pixels[0], src_pixels[1], dest_pixels[0], dest_pixels[1], LERP_CONSTANT);

	globalState.lerpViewCurrPosX_pixels = destX_worldPixels;
	globalState.lerpViewCurrPosY_pixels = destY_worldPixels;

	return result;
}

/* Get from (src, srcY) to (destX, destY) in 'count' equal increments.  */
function interpolateCoords(srcX, srcY, destX, destY, count) {

	let result = [];

	let currX_float = srcX, currY_float = srcY;

	let deltaX_float = (destX-srcX)/count;
	let deltaY_float = (destY-srcY)/count;

	for(let x = 0; x < count; x++) {

		let entry = {
			"x" : Math.floor(currX_float),
			"y" : Math.floor(currY_float)
		};

		result.push(entry);

		currX_float += deltaX_float;
		currY_float += deltaY_float;
	}

	return result;
}


function disposeGlobalState() {
	globalState.mouseMoveEnabled = false;
	globalState.ctx = null;
	globalState.keyFrameBufferCtx = null;
	globalState.globalTileMap = {};
	globalState.entityList = {};
	globalState.frameQueue = {};
	globalState.prevDataMap = {};
	globalState.imageMap = {};
	globalState.console = {};
	globalState.dataArray = {};
//	globalState.dirtyRedraw = {};
	clearInterval(globalState.interval);
	globalState.scaleCache = null;

	globalState.offscreenCanvas = null;
	globalState.offscreenContext = null;
	globalState.lerpQueue = null;

}

function addToDataMap(x, y, worldXSize, worldYSize, imageNumber, rotation, layerIndex, layerSize) {
	
	var entry = getFromDataMap(x, y); 
	if(entry == null || entry.length != layerSize) {
		entry = [];
		globalState.prevDataMap.set(x*32768+y, entry);
	}

	entry[layerIndex] = {"num" : imageNumber,
					"rot" : rotation };
	
}

function getFromDataMap(x, y, worldXSize, worldYSize) {
	return globalState.prevDataMap.get(x*32768+y);
}


function processAndDrawNewFrame(param, skipdraw) {
	
	var spriteSize = globalState.spriteSize;	

	// The tile (x,y) coords for the top-left hand side of the canvas
	// - (startX, startY) differ from (currWorldPosX, currWorldPosY) as startX/Y uses a smaller rectangle based on sprite size, 
	//   whereas currWorldPosX/Y uses (80, 40)
	var startX = 0, startY = 0;	

	// Width/height of the canvas in # of tiles, for example, 40x40
	var actualWidth = 0, actualHeight = 0;

	if(param.fullSent == true) {
		skipdraw = false;
	}

	let targetWidth = param.currViewWidth;
	let targetHeight = param.currViewHeight;

	if(globalState.viewType != "SERVER_VIEW_WORLD") {

		actualWidth = param.currViewWidth;
		actualHeight = param.currViewHeight;

		targetWidth = Math.floor(myCanvas.width/spriteSize)+2;
		targetHeight = Math.floor(myCanvas.height/spriteSize)+2;
		
		// Find the center point of the agent view
		// let centerPointX = Math.floor(param.currViewWidth/2)+param.currWorldPosX;
		// let centerPointY = Math.floor(param.currViewHeight/2)+param.currWorldPosY;
	
		// // Use the above center point to calculate the top left corner of the (smaller) browser view
		// centerPointX -= Math.floor(actualWidth/2);
		// centerPointY -= Math.floor(actualHeight/2);
	
		startX = param.currWorldPosX;
		startY = param.currWorldPosY;

	} else {

		actualWidth = param.currViewWidth;
		actualHeight = param.currViewHeight;
		startX = param.currWorldPosX;
		startY = param.currWorldPosY;
	}

	if(skipdraw) {
		console.log("Skipping draw of "+param.frame);
	}
		
	var currRedrawManager = new RedrawManager(spriteSize*5, spriteSize*5);
//	globalState.dirtyRedraw = currRedrawManager;
		
	if(param.currWorldPosX != globalState.currWorldX || param.currWorldPosY != globalState.currWorldY || globalState.currWorldX == null || globalState.currWorldY == null) {
		currRedrawManager.flagPixelRect(0, 0, param.currViewWidth*spriteSize, param.currViewHeight*spriteSize);
	}
	
	// An array of delta frames: a delta frame is a rectangles of tiles that were updated in the world { tile_x, tile_y, width of rect, height of rect, data : [ tile data ] }
	// See BrowserWebSocketClientShared
	var mapData = param.frameData;
	
	// For each updated tile on the map, update the dataMap with the latest contents
	var numDelta = mapData.length;
	for(let deltaIndex = 0; deltaIndex < numDelta; deltaIndex++) {
		// For each delta frame...

		let deltaBody = mapData[deltaIndex];
		let width = deltaBody.w;
		let height = deltaBody.h;
		let dbx = deltaBody.x;
		let dby = deltaBody.y;

		// Flag the update rectangle as dirty
		{	
			let x = (dbx+param.currWorldPosX-startX);
			let y = (dby+param.currWorldPosY-startY);
			if(x < 0 ) {
				x = 0;
			}
			if(y < 0) {
				y = 0;
			}
			currRedrawManager.flagPixelRect(x*spriteSize, y*spriteSize, width*spriteSize, height*spriteSize);
		}
		
		let arrayContents = deltaBody.data;
		
		for(let x = 0; x < arrayContents.length; x++) {
			// For each updated tile in the delta frame...
			
			let col = (x % width+dbx);
			let row = (Math.floor(x / width)+dby);
			
			let colPixel = col * spriteSize;
			let rowPixel = row * spriteSize;

			
			for(let layerIndex = arrayContents[x].length-1; layerIndex >= 0; layerIndex--) {
				// For each layer in the tile...
				
				let num = arrayContents[x][layerIndex][0];
				
				let img = globalState.imageMap.get(num);

				let rotation = arrayContents[x][layerIndex].length == 1 ? 0 : arrayContents[x][layerIndex][1];
				
				let worldX = col+param.currWorldPosX;
				let worldY = row+param.currWorldPosY;
				
				addToDataMap(worldX, worldY, param.currViewWidth, param.currViewHeight, num, rotation, layerIndex, arrayContents[x].length);

			}
			
		}
		
	} // end deltaIndex for
	
	if(!skipdraw) {
		
		// There are different optimizations used on world draw, versus on non-world lerp draw. 
		if(globalState.viewType == "SERVER_VIEW_WORLD") {
			// There is no need for LERP for world draw, because the world canvas doesn't move.
			drawFrameForWorld(actualWidth, actualHeight, startX, startY, param, skipdraw, currRedrawManager);	
		} else {
			// Non-world draw can skip some optimizations, because it does not need to draw the whole world at once.
			drawFrameForLerp(actualWidth, actualHeight, startX, startY, param, skipdraw, currRedrawManager);

			// Create lerp queue entries (camera movements over the drawn keyframe image)
			globalState.lerpQueue = createEntry(param.currWorldPosX, param.currWorldPosY, globalState.currWorldX, 
					globalState.currWorldY, targetWidth, targetHeight, param.currViewWidth, param.currViewHeight, spriteSize);

		}
		
	}
	

	globalState.currWorldX = param.currWorldPosX;
	globalState.currWorldY = param.currWorldPosY;
	globalState.startX = startX;
	globalState.startY = startY;

}

function drawFrameForLerp(actualWidth, actualHeight, startX, startY, param, skipdraw, currRedrawManager) {

	var ctx = globalState.keyFrameBufferCtx;
	
	var spriteSize = globalState.spriteSize;	

	if(!globalState.firstFrameDrawn) {
		// Draw the right 200 pixels as a different shade, to match the leaderboard panel
		ctx.fillStyle="rgb(174, 223, 101)";
		ctx.fillRect(0, 0, ctx.canvas.width-200, ctx.canvas.height);
		ctx.fillStyle="#27782e";
		ctx.fillRect(ctx.canvas.width-200, 0, ctx.canvas.width, ctx.canvas.height);
		
		globalState.firstFrameDrawn = true;
	} 
		
	for(let x = 0; x < actualWidth; x++) {
		for(let y = 0; y < actualHeight; y++) {
			// For each tile in the view, check if it is dirty (and we thus we need to redraw it)
			
			let redraw = currRedrawManager.getByPixel(x*spriteSize, y*spriteSize);
			
			if(true || redraw) {
				let cachedTile = getFromDataMap(startX+x, startY+y, param.currViewWidth, param.currViewHeight);
				
				if(cachedTile != null) {
					for(let layerIndex = cachedTile.length-1; layerIndex >= 0; layerIndex--) {
						let layer = cachedTile[layerIndex];
						
						let img = globalState.imageMap.get(layer.num);
						if(img != null) {
							drawRotatedImage(ctx, img, x*spriteSize, y*spriteSize, layer.num,  layer.rot);
						}							
					}
				}
				
			}
		}
	}

	// Draw username/creature damage on the key frame buffer
	for(let x = 0; x < param.creatures.length; x++) {
		
		let creature = param.creatures[x];
		let percent = Math.max(0, creature.hp) / creature.maxHp;
		
		let posX = creature.position[0]-startX;
		let posY = creature.position[1]-startY;

		if(posY >= actualHeight-2) {
			continue;
		}
		
		let percentIndex = Math.max( 0, Math.min(99, 100*percent));
		
		if(creature.username == GLOBAL_USERNAME){
			globalState.playerCurrHp = creature.hp;
			globalState.playerMaxHp = creature.maxHp;
		}
		
//		if(false && creature.username == GLOBAL_USERNAME){
//			// Draw colour part of damage bar
//			var offset = (myCanvas.width/5);
//			ctx.fillStyle=globalState.damageGradient[Math.floor(percentIndex)]; 
//			ctx.fillRect(offset, myCanvas.height-50, (myCanvas.width-(2*offset))*percent, 24);
//			
//			// Draw grey part of damage bar
//			ctx.fillStyle="#666666";
//			ctx.fillRect((myCanvas.width-(2*offset))*percent+offset, myCanvas.height-50, (myCanvas.width-(2*offset))*(1-percent), 24);
//			ctx.font = "20px Arial";
//			ctx.fillStyle = "white";
//			ctx.textAlign = "center";
//			var displayHealth = Math.max(0, creature.hp)+" / "+creature.maxHp;
//			ctx.fillText(displayHealth.trim(),myCanvas.width/2,myCanvas.height-30);
//			ctx.restore();
//			
//		} else {
			
			// Draw colour part of damage bar
			ctx.fillStyle=globalState.damageGradient[Math.floor(percentIndex)]; 
			ctx.fillRect(posX*spriteSize,(posY+1)*spriteSize+5,spriteSize*percent,4);
			// globalState.dirtyRedraw.flagPixelRect(posX*spriteSize,(posY+1)*spriteSize+5,spriteSize*percent,4);
			
			// Draw grey part of damage bar
			ctx.fillStyle="#666666";
			ctx.fillRect(posX*spriteSize+spriteSize*percent,(posY+1)*spriteSize+5,spriteSize*(1-percent),4);
			// globalState.dirtyRedraw.flagPixelRect(posX*spriteSize+spriteSize*percent,(posY+1)*spriteSize+5,spriteSize*(1-percent),4);
			
//		}
		
		
		// Draw username as white on black text
		if(creature.username != null /*&& (globalState.viewType == "SERVER_VIEW_FOLLOW" || globalState.viewType == "CLIENT_VIEW")*/ ) {
			
			ctx.font = globalState.viewType == "SERVER_VIEW_WORLD" ? '9px sans-serif' : '12px sans-serif';				
			
			let textSizeWidth = ctx.measureText(creature.username).width;
			let textSizeHeight = ctx.measureText("M").width;
			
			let yDelta = globalState.viewType == "SERVER_VIEW_WORLD" ? 10: 0;
			
			let xPos = posX*(spriteSize) + Math.floor(spriteSize/2) - Math.floor(textSizeWidth/2)
			
			ctx.fillStyle="black";
			ctx.fillRect(xPos, (posY+2)*(spriteSize)-textSizeHeight+2+yDelta, textSizeWidth+6, textSizeHeight+8);
			// globalState.dirtyRedraw.flagPixelRect(xPos, (posY+2)*(spriteSize)-textSizeHeight+2+yDelta, textSizeWidth+6, textSizeHeight+8);
			
			
			ctx.fillStyle="white";	
//				ctx.font = '12px sans-serif';				
			ctx.fillText(creature.username, xPos+3, (posY+2)*spriteSize+4+yDelta);
			
		}

	}
	
	if(globalState.viewType == "SERVER_VIEW_FOLLOW") {
		serverViewFollowPos = { x : param.currWorldPosX, y : param.currWorldPosY, w: param.currViewWidth, h: param.currViewHeight };
		
	} else if(globalState.viewType == "SERVER_VIEW_WORLD") {
		
		// Blue rectangle 
		if(serverViewFollowPos != null) {
			
			let lineWidth = 3;
			ctx.beginPath();
			ctx.lineWidth=""+lineWidth;
			ctx.strokeStyle="#000099";
			
			let rectX = (serverViewFollowPos.x*spriteSize)-lineWidth;
			let rectY = (serverViewFollowPos.y*spriteSize)-lineWidth;
			
			ctx.rect(rectX, rectY, serverViewFollowPos.w*spriteSize, serverViewFollowPos.h*spriteSize);
			// globalState.dirtyRedraw.flagPixelRect( (rectX)-lineWidth, (rectY)-lineWidth, (serverViewFollowPos.w*spriteSize)+(lineWidth*2), (serverViewFollowPos.h*spriteSize)+(lineWidth*2) );
			ctx.stroke();
		}
		  
	}
	
	// Draw frame rate
	ctx.fillStyle="white";	
	ctx.font = '15px sans-serif';
	ctx.fillText(param.frame, 40, 40);

	
	// Draw floating damage text
	if(globalState.entityList != null) {
		
		let fontSize = 20;
		
		if(spriteSize < 10) {
			fontSize = 10;
		}
		
		for(let c = 0; c < globalState.entityList.length; c++) {
			let entity = globalState.entityList[c];

			entity.frame++;
			
			let col = entity.worldx - startX; 
			let row = entity.worldy - startY; 
			
			entity.xoffset += entity.directionX*5;
			entity.yoffset += entity.directionY*5;
			
			
			if(row >= 0 && col >= 0 && row < param.currViewHeight && col <= param.currViewWidth) {
				let colX = col*globalState.spriteSize;
				let colY = row*globalState.spriteSize;
				
				// Don't draw text outside the visible world on the canvas 
				if(colX + entity.xoffset < actualWidth*spriteSize && colY + entity.yoffset < actualHeight*spriteSize) {
					
					ctx.fillStyle="red";
					ctx.font = fontSize+'px sans-serif';
					ctx.fillText(entity.damage, colX+entity.xoffset, colY+entity.yoffset);
					// globalState.dirtyRedraw.flagPixelRect(colX+entity.xoffset-30, colY+entity.yoffset-30, 60, 60);
					
				}
			}
			
			// If an entity has drawn for more than 10 frames, remove it
			if(entity.frame > 10) {
				globalState.entityList.splice(c,1);
				c--;
			}
		}
	} 
	
	// In the world view, draw a black dividing line between the frames, in the centre of the screen.   
	if(globalState.viewType == "SERVER_VIEW_WORLD") {
	      ctx.beginPath();
	      ctx.lineWidth=1;
	      ctx.fillStyle="rgb(0, 0, 0)";
	      ctx.moveTo(0, 0);
	      ctx.lineTo(0, 2000);
	      ctx.stroke();
	}
		
} 


function drawFrameForWorld(actualWidth, actualHeight, startX, startY, param, skipdraw, currRedrawManager) {

	var ctx = globalState.ctx;
	
	var spriteSize = globalState.spriteSize;	

	// Replace the primary context with a clean drawing of the world (without usernames/damage drawn)
	if(secondaryCtxDrawn) {
		ctx.drawImage(secondaryCanvas, 0, 0);
	}

	if(!globalState.firstFrameDrawn) {
		// Draw the right 200 pixels as a different shade, to match the leaderboard panel
		ctx.fillStyle="rgb(174, 223, 101)";
		ctx.fillRect(0, 0, ctx.canvas.width-200, ctx.canvas.height);
		ctx.fillStyle="#27782e";
		ctx.fillRect(ctx.canvas.width-200, 0, ctx.canvas.width, ctx.canvas.height);
		
		globalState.firstFrameDrawn = true;
	} 
		
	for(let x = 0; x < actualWidth; x++) {
		for(let y = 0; y < actualHeight; y++) {
			// For each tile in the view, check if it is dirty (and we thus we need to redraw it)
			
			let redraw = currRedrawManager.getByPixel(x*spriteSize, y*spriteSize);
			
			if(redraw) {
				let cachedTile = getFromDataMap(startX+x, startY+y, param.currViewWidth, param.currViewHeight);
				
				if(cachedTile != null) {
					for(let layerIndex = cachedTile.length-1; layerIndex >= 0; layerIndex--) {
						let layer = cachedTile[layerIndex];
						
						let img = globalState.imageMap.get(layer.num);
						if(img != null) {
							drawRotatedImage(ctx, img, x*spriteSize, y*spriteSize, layer.num,  layer.rot);
						}							
					}
				}
				
			}
		}
	}

	// Copy the up-to-date main canvas to the secondary ctx
	secondaryCtx.drawImage(myCanvas, 0, 0);
	secondaryCtxDrawn = true;

	// Now that we have a clean copy of the canvas stored in secondaryCtx, we are going to draw username/creature damage on the main canvas
	for(let x = 0; x < param.creatures.length; x++) {
		
		let creature = param.creatures[x];
		let percent = Math.max(0, creature.hp) / creature.maxHp;
		
		let posX = creature.position[0]-startX;
		let posY = creature.position[1]-startY;

		if(posY >= actualHeight-2) {
			continue;
		}
		
		let percentIndex = Math.max( 0, Math.min(99, 100*percent));
		
		// Draw colour part of damage bar
		ctx.fillStyle=globalState.damageGradient[Math.floor(percentIndex)]; 
		ctx.fillRect(posX*spriteSize,(posY+1)*spriteSize+5,spriteSize*percent,4);
		// globalState.dirtyRedraw.flagPixelRect(posX*spriteSize,(posY+1)*spriteSize+5,spriteSize*percent,4);
		
		// Draw grey part of damage bar
		ctx.fillStyle="#666666";
		ctx.fillRect(posX*spriteSize+spriteSize*percent,(posY+1)*spriteSize+5,spriteSize*(1-percent),4);
		// globalState.dirtyRedraw.flagPixelRect(posX*spriteSize+spriteSize*percent,(posY+1)*spriteSize+5,spriteSize*(1-percent),4);
		
		// Draw username as white on black text
		if(creature.username != null /*&& (globalState.viewType == "SERVER_VIEW_FOLLOW" || globalState.viewType == "CLIENT_VIEW")*/ ) {
			
			ctx.font = globalState.viewType == "SERVER_VIEW_WORLD" ? '9px sans-serif' : '12px sans-serif';				
			
			let textSizeWidth = ctx.measureText(creature.username).width;
			let textSizeHeight = ctx.measureText("M").width;
			
			let yDelta = globalState.viewType == "SERVER_VIEW_WORLD" ? 10: 0;
			
			let xPos = posX*(spriteSize) + Math.floor(spriteSize/2) - Math.floor(textSizeWidth/2)
			
			ctx.fillStyle="black";
			ctx.fillRect(xPos, (posY+2)*(spriteSize)-textSizeHeight+2+yDelta, textSizeWidth+6, textSizeHeight+8);
			// globalState.dirtyRedraw.flagPixelRect(xPos, (posY+2)*(spriteSize)-textSizeHeight+2+yDelta, textSizeWidth+6, textSizeHeight+8);
			
			
			ctx.fillStyle="white";	
//				ctx.font = '12px sans-serif';				
			ctx.fillText(creature.username, xPos+3, (posY+2)*spriteSize+4+yDelta);
			
		}

	}
	
	if(globalState.viewType == "SERVER_VIEW_FOLLOW") {
		serverViewFollowPos = { x : param.currWorldPosX, y : param.currWorldPosY, w: param.currViewWidth, h: param.currViewHeight };
		
	} else if(globalState.viewType == "SERVER_VIEW_WORLD") {
		
		// Blue rectangle 
		if(serverViewFollowPos != null) {
			
			let lineWidth = 3;
			ctx.beginPath();
			ctx.lineWidth=""+lineWidth;
			ctx.strokeStyle="#000099";
			
			let rectX = (serverViewFollowPos.x*spriteSize)-lineWidth;
			let rectY = (serverViewFollowPos.y*spriteSize)-lineWidth;
			
			ctx.rect(rectX, rectY, serverViewFollowPos.w*spriteSize, serverViewFollowPos.h*spriteSize);
			// globalState.dirtyRedraw.flagPixelRect( (rectX)-lineWidth, (rectY)-lineWidth, (serverViewFollowPos.w*spriteSize)+(lineWidth*2), (serverViewFollowPos.h*spriteSize)+(lineWidth*2) );
			ctx.stroke();
		}
		  
	}
	
	// Draw frame rate
	ctx.fillStyle="white";	
	ctx.font = '15px sans-serif';
	ctx.fillText(param.frame, 20, 20);

	
	// Draw floating damage text
	if(globalState.entityList != null) {
		
		let fontSize = 20;
		
		if(spriteSize < 10) {
			fontSize = 10;
		}
		
		for(let c = 0; c < globalState.entityList.length; c++) {
			let entity = globalState.entityList[c];

			entity.frame++;
			
			let col = entity.worldx - startX; 
			let row = entity.worldy - startY; 
			
			entity.xoffset += entity.directionX*5;
			entity.yoffset += entity.directionY*5;
			
			
			if(row >= 0 && col >= 0 && row < param.currViewHeight && col <= param.currViewWidth) {
				let colX = col*globalState.spriteSize;
				let colY = row*globalState.spriteSize;
				
				// Don't draw text outside the visible world on the canvas 
				if(colX + entity.xoffset < actualWidth*spriteSize && colY + entity.yoffset < actualHeight*spriteSize) {
					
					ctx.fillStyle="red";
					ctx.font = fontSize+'px sans-serif';
					ctx.fillText(entity.damage, colX+entity.xoffset, colY+entity.yoffset);
					// globalState.dirtyRedraw.flagPixelRect(colX+entity.xoffset-30, colY+entity.yoffset-30, 60, 60);
					
				}
			}
			
			// If an entity has drawn for more than 10 frames, remove it
			if(entity.frame > 10) {
				globalState.entityList.splice(c,1);
				c--;
			}
		}
	} 
	
	// In the world view, draw a black dividing line between the frames, in the centre of the screen.   
	if(globalState.viewType == "SERVER_VIEW_WORLD") {
	      ctx.beginPath();
	      ctx.lineWidth=1;
	      ctx.fillStyle="rgb(0, 0, 0)";
	      ctx.moveTo(0, 0);
	      ctx.lineTo(0, 2000);
	      ctx.stroke();
	}
		
} 
	
function loadSprite(src, id) {
	return $.Deferred(function(promise) {
		var sprite = new Image();

		sprite.onload = promise.resolve.bind(null, [sprite, id] );
		sprite.src = src;
	}).promise();
}

function loadAndCall(event, nextFunction) {
	var loaders = [];
	
	GLOBAL_TILES_JSON.forEach( function(x) {
		loaders.push(loadSprite('resources/tiles/'+x[0]+'.png', x[0]) );
	});
	
	$.when.apply(null, loaders).done(function() {
		
		var argsAsArray = Array.from(arguments);
		
		for(var i = 0; i < argsAsArray.length; i++) {
			globalState.imageMap.set(argsAsArray[i][0][1], argsAsArray[i][0][0])
		}

		globalState.imagesLoaded = true;
		nextFunction(event);
	});
	
}

function drawRotatedImage(context, image, x, y, num, angle) {

	var TO_RADIANS = Math.PI/180; 

	let spriteSize = globalState.spriteSize;

	let scaleCache = globalState.scaleCache;

	let offscreenCanvas = globalState.offscreenCanvas;
	let offscreenContext = globalState.offscreenContext;
	
	// Fast path (no rotation required)
	if(angle == 0 && scaleCache != null) {

		let inCache = false;
		
		let val = scaleCache.get(num);

		// If we have previously cached the tile 'num', then draw it
		if(val != null) {
			inCache = true;
			if(val.imgdata.complete) {				
				context.drawImage(val.imgdata, x, y);
				return;
			} 		
		}
		
		context.drawImage(image, x, y, spriteSize, spriteSize);

		if(!inCache) {
			offscreenContext.clearRect(0, 0, spriteSize, spriteSize);
			offscreenContext.drawImage(image, 0, 0, spriteSize, spriteSize);
		
			var newimg = new Image();
			newimg.src = offscreenCanvas.toDataURL('image/png'); 
			
			var entry = { "imgdata" :  newimg  };
			scaleCache.set(num, entry);
		}
		
		return;
	}
	
	context.save(); 
 
	context.translate(x, y);
 
	context.rotate(angle * TO_RADIANS);
 
	if(angle == 270) {
		context.drawImage(image, -spriteSize, 0, spriteSize, spriteSize);
	} else if(angle == 180) {
		context.drawImage(image, -spriteSize, -spriteSize, spriteSize, spriteSize);
	} else if(angle == 90) {
		context.drawImage(image, 0, -spriteSize, spriteSize, spriteSize);
	} else {
		context.drawImage(image, 0, 0, spriteSize, spriteSize);				
	}
 
	context.restore(); 
}

function generateDamageGradient() {
	var result = [];

	var step = 255/100;
	
	for(var x = 0; x < 100; x++) {
		
		var r = 255-Math.floor(x*step);
		var g = Math.floor(x*step);
		var b = 255-Math.max(r, g);
		
		var str = "#"+ convertToHex(r)+convertToHex(g)+convertToHex(b);
		
		result.push(str);
		
	}
	
	return result;
	
}

function updateLeaderboardUI(leaderboardDomElement,  json /* : JsonUpdateBrowserUI */,  leaderboardData /* : globalState.leaderboardData */) {

	// Update using roundState
	if(json.roundState != null) {
		
		if(json.roundState.roundId != null) {
			leaderboardData.currRound = json.roundState.roundId;
		}
		
		var element = document.getElementById('roundPopover');

		if(json.roundState.type == "ACTIVE") {
			
			leaderboardData.timeleft = "Round #"+json.roundState.roundId+"<br/>";
			leaderboardData.timeleft += "Round time: "+ convertSecondsToMinutes(json.roundState.timeLeftInSeconds)+"<br/>";
			
			// document.getElementById("roundPopover_timeleft").innerHTML = leaderboardData.timeleft;
			if(element != null) {
				element.style.display="none";	
			}
			
			
		} else if(json.roundState.type == "INACTIVE") {
			
			leaderboardData.timeleft = "Next round is #"+json.roundState.nextRoundId+"<br/>";
			leaderboardData.timeleft += "Next round in: "+ convertSecondsToMinutes(json.roundState.nextRoundStartInSecs)+"<br/>";
			// leaderboardData.timeleft += "<br/>";
			// document.getElementById("roundPopover_timeleft").innerHTML = leaderboardData.timeleft;
			
			if(element != null) {
				element.style.display="block";
				$('#roundPopover').css({ 'top' : '25%' });
				$('#roundPopover').css({ 'left' : '25%' });
			}

		}
	}
	
	// Update using currentPlayerScore
	if(json.currentPlayerScore != null) {
		leaderboardData.yourscore = "Your round score: " + formatScore(json.currentPlayerScore)+"<br/>";
		// document.getElementById("roundPopover_yourscore").innerHTML = leaderboardData.yourscore;
	}
	
	// Update lbd.leaderboard
	if(json.currentPlayerRank != null || json.currentRoundScores != null) {
				
		if(json.currentPlayerRank != null) {
			leaderboardData.leaderboard = "Your round rank: <a href='"+SERVER_WEB_URL+"/database/round/recent' target='_blank'>#" + json.currentPlayerRank+"</a><br/>";
		}
				
		if(json.currentRoundScores != null) {
			leaderboardData.leaderboard_list = "<br/><br/>";
			
			for(var x = 0; x < json.currentRoundScores.length; x++) {
				var entry = json.currentRoundScores[x];
				leaderboardData.leaderboard_list += "#"+entry.rank+" - "+entry.username+" ("+formatScore(entry.score)+")<br/>";
				
				if(x == 0) {
					// Keep track of the player that is currently on top of the leaderboard, for use later
					leaderboardData.leaderboard_curr_winner = "The winner is <b>"+entry.username+"</b> with score <b>"+formatScore(entry.score)+"</b>!<br/>";
				}
			}
			
			if(json.currentRoundScores.length > 0) {
				leaderboardData.leaderboard_list += "<br/>";				
			}
		}
		
	}
	
	if(json.currentPlayerBestTotalScore != null || json.currentPlayerBestTotalRank != null) {
		
		if(leaderboardData.overall == "") {
		
			if(json.currentPlayerBestTotalScore != null) {
				leaderboardData.overall  += "Your overall best score: "+formatScore(json.currentPlayerBestTotalScore)+"<br/>";
			}
			
			if(json.currentPlayerBestTotalRank != null) {
				leaderboardData.overall  += "Your overall best rank: <a href='"+SERVER_WEB_URL+"/database/round/recent' target='_blank'>#"+json.currentPlayerBestTotalRank+"</a><br/>";
			}
			
		}
	}
	
	// Update leaderboard.stats
	if(json.stats != null && json.stats.length > 0) {
		
		leaderboardData.stats = "<b>Metrics</b>:<br/><br/><table id='metrics_table'>";
		
		leaderboardData.stats += "<tr>";
		leaderboardData.stats += "<th>User</th>";
		leaderboardData.stats += "<th>Health Check</th>";
		leaderboardData.stats += "<th>Actions/sec</th>";
		leaderboardData.stats += "<th># of deaths</th>";
		leaderboardData.stats += "<th>Time between actions</th>";
		leaderboardData.stats += "</tr>";
		
		for(var x = 0; x < json.stats.length && x < 5; x++) {
			
			var entry = json.stats[x];
			
			leaderboardData.stats += "<tr class=\""+(x % 2 == 0 ? "metric-even" : "metric-odd")+"\">";
			
			leaderboardData.stats += "<td>"+entry.username+"</td>";
			leaderboardData.stats += "<td>"+(entry.passLastHealthCheck ? "true" : "false")+"</td>";
			leaderboardData.stats += "<td>"+entry.actionsPerSecond+"</td>";
			leaderboardData.stats += "<td>"+entry.numberOfTimesDied+"</td>";
			leaderboardData.stats += "<td>"+entry.averageTimeBetweenActions+" msecs</td>";
			
			leaderboardData.stats += "</tr>";
		}
		
		leaderboardData.stats += "</table>";
	}

	// Update leaderboarDomElement
	{
		var str = leaderboardData.timeleft +"<br/>" 
			+ leaderboardData.yourscore 
			+ leaderboardData.leaderboard 
			+ leaderboardData.leaderboard_list 
			+ leaderboardData.overall;
		
		leaderboardDomElement.innerHTML = "<span id='leaderboard_span'>"+str+"</span>";
	}

	// Update roundPopover dom elements
	{
		document.getElementById("roundPopover_timeleft").innerHTML = leaderboardData.leaderboard_curr_winner+"<br/>"+leaderboardData.timeleft+"<br/>";
		document.getElementById("roundPopover_yourscore").innerHTML = leaderboardData.yourscore;
		document.getElementById("roundPopover_leaderboard").innerHTML = leaderboardData.leaderboard+"<br/>";		
		document.getElementById("roundPopover_overall").innerHTML = leaderboardData.overall;		
	}
	
	// Update metrics
	metricsDomElement.innerHTML = "<span id='metrics_span'>"+leaderboardData.stats+"</span>";  ;
		
}

function updateConsoleUI(console, consoleDomElement) {
	while(console.length > 8) {
		console.splice(console.length-1, 1);
	}
	
	var str = "";
	
	for(var x = 0; x < console.length; x++) {
		str += console[x]+"<br/>\n";
	}	
	
	consoleDomElement.innerHTML = "<span id='console_span'><b>Event Log</b>:<br/><br/>"+str+"</span>"; 

}

function updateInventoryUI(inventoryDomElement, json) {
	var invStr = "<b>Inventory</b>: <br/></br>";
	if(json.inventory) {
		for(var x = 0; x < json.inventory.length; x++) {
			invStr += "<span>" + json.inventory[x].name + (json.inventory[x].quantity > 1 ? " x" +json.inventory[x].quantity : "") + "</span><br/>";
		}
	}
	
	inventoryDomElement.innerHTML = "<span id='items_span'>" + invStr + "</span>";
}

function updateEquipmentUI(equipmentDomElement, json) {
	var equipStr = "<b>Equipment</b>: <br/></br>";
	if(json.equipment) {
		for(var x = 0; x < json.equipment.length; x++) {
			equipStr += "<span>" + json.equipment[x].name + "</span><br/>";
		}
	}
	equipmentDomElement.innerHTML = "<span id='items_span'>" + equipStr + "</span>";
}

function formatScore(score) {
	return score.toLocaleString();
}


function addCanvasListeners() {

	myCanvas.addEventListener("mouseout", function(e) {
		var element = document.getElementById('creatureInfo'); 
		element.innerHTML = ""; 
		element.style.display="none";

	});

	myCanvas.addEventListener("mousemove", function(e) {
		
		if(!globalState.mouseMoveEnabled) {
			return;
		}

		var pageX = e.clientX; 
		var pageY = e.clientY;

		var worldCol = Math.max(0, Math.floor((pageX + globalState.lerpViewCurrPosX_pixels)/globalState.spriteSize) - 4);
		var worldRow = Math.max(0, Math.floor((pageY + globalState.lerpViewCurrPosY_pixels)/globalState.spriteSize) - 4);

//		var worldCol = Math.max(0, Math.floor(pageX/globalState.spriteSize) + globalState.startX - 4);
//		var worldRow = Math.max(0, Math.floor(pageY/globalState.spriteSize) + globalState.startY - 4);
		
		var localTileMap = new Map();
		
		var newHtml = "<span id='creatureInfoSpan'>";
		
		newHtml += "<table>";
		
		var containsValue = false;
		
		for(let x = worldCol; x < worldCol+8; x++) {
			for(let y = worldRow; y< worldRow+8; y++) {
				let tiles = getFromDataMap(x, y, globalState.currWorldX, globalState.currWorldY);
				if(tiles == null) { continue; }
				localTileMap.set(tiles[0].num, tiles[0].num );
			}
		}
		
		localTileMap.forEach(function(value, key) {
			
			let name = globalState.globalTileMap.get(value);
			if(name == null || name.trim().length == 0) {
				return;
			}
			newHtml += "<tr><td><img src='resources/tiles/"+value+".png' width='42' height='42'/></td><td><span style='margin-left: 20px; margin-right: 20px;'>"+name+"</span></td>"
			containsValue = true;

		});
		
		newHtml += "</table>";

		newHtml += "</span>";
		
		// newHtml += "( "+worldCol+", "+worldRow+")<br/>";
		
		// $('div.moveAble').innerHTML = "new content!";
		
		var element = document.getElementById('creatureInfo'); 
		element.innerHTML = newHtml; 
		
		if(!containsValue) {
			element.style.display="none";	
		} else {
			element.style.display="block";
		}
		
		// $(document).getElementById("test").innerHTML = "new content"
		
		$('div.moveAble').css({
			'top' : pageY + 20
		});
		
		$('div.moveAble').css({
			'left' : pageX + 20
		});

		
	}, false);

	addEvent(window, "resize", function(event) {
		if(currCanvasWidth != myCanvas.width || currCanvasHeight != myCanvas.height) {
			currCanvasWidth = myCanvas.width;
			currCanvasHeight = myCanvas.height;
			console.log("resize in canva?")
			reestablishConnection();
		}
	});

}


// A function to play game sounds
function sound(src) {
    this.sound = document.createElement("audio");
    this.sound.src = src;
    this.sound.setAttribute("preload", "auto");
    this.sound.setAttribute("controls", "none");
    this.sound.style.display = "none";
    document.body.appendChild(this.sound);
    this.play = function(){
        this.sound.play();
    }
    this.stop = function(){
        this.sound.pause();
    }
}


}

