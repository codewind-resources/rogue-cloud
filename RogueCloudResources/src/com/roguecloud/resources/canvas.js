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

'use strict';

function canvasJs(spriteSize, myCanvas, consoleDomElement, leaderboardDomElement, metricsDomElement, viewType, optionalUuid) {


var viewTypeParam = viewType;
	
console.log("view type is "+viewType);


// secondaryCanvas/secondaryCtx are a copy of the last frame that was drawn to the screen BEFORE
// the damage values, hp indicators, and usernames were drawn. This copy can then be used to refresh the 
// screen without a full redraw. 
var secondaryCanvas = document.createElement("canvas"),
	secondaryCtx = secondaryCanvas.getContext("2d");

//  Whether secondaryCtx contains frame data (this is always true after the first frame is drawn)
var secondaryCtxDrawn = false;
{
	secondaryCtx.canvas.width = myCanvas.width;
	secondaryCtx.canvas.height = myCanvas.height;
}

var currCanvasWidth = myCanvas.width;
var currCanvasHeight = myCanvas.height;

var debugMessagesReceived = 0;

addCanvasListeners();

var globalState;

// Create and define globalState object.
{
	{
		globalState =  {};
		
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
		
//		globalState.contextWidthSet = true /*false*/;
		
		globalState.spriteSize = spriteSize;

		// The world (x,y) tile coords for the top-left-hand-side of the canvas
		globalState.startX = null;
		globalState.startY = null;

		
		// The world (x, y) tile coords for the top-left-hand-side of the 80x40 agent data view 
		// - from json property 'currWorldPosX/Y' of browser update json	
		globalState.currWorldX = -1;
		globalState.currWorldY = -1;


		globalState.frameQueue = [];
		
		globalState.entityList = [];
		
		globalState.prevDataMap = new Map( /* x*y coord -> num+rot */);

		globalState.dataArray = null;
		
		globalState.imageMap = new Map( /* num -> img */);

		// Primary canvas context	
		globalState.ctx = myCanvas.getContext("2d");
		
		// Absolute time of last frame draw in milliseconds
		globalState.lastFrameDrawTime = 0;

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

	createWebSocketAndSetInGlobalState();
	
}

function createWebSocketAndSetInGlobalState() {
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
				
		globalState.exampleSocket = new WebSocket(new_uri);

		globalState.wsInterval = setInterval( function() {
			
			if(globalState.ctx == null) { return; }
			
			if(globalState.exampleSocket.readyState != 1) {
				clearInterval(globalState.wsInterval);
				console.log("ready state: "+globalState.exampleSocket.readyState);				
				reestablishConnection();

			}

		}, 5000);
	} catch(err) {
		console.log(err);
		// reestablishConnection();
		return;
	}
	console.log("post?");
	
	globalState.exampleSocket.onopen = function() {

		if(globalState.exampleSocket == null) {
			return;
		}
		
		if(globalState.ctx == null) { globalState.exampleSocket.close(); }
		
		var browserConnect = {
			"type" : "JsonBrowserConnect",
			"uuid" : optionalUuid == null ? generateUuid() : optionalUuid, /* GLOBAL_UUID,*/
			"username" : GLOBAL_USERNAME,
			"password" : GLOBAL_PASSWORD,
			"viewType" : viewTypeParam
			/* "viewOnly" : true*/
		}
		
		globalState.exampleSocket.onclose = function() {
			console.log("on close called.");

			// Return if we've already disposed.
			if(globalState.ctx == null) { return; }
		
			// reestablishConnection();
		}
		
		var browserConnectJson = JSON.stringify(browserConnect);
		globalState.exampleSocket.send(browserConnectJson);
		console.log("open: "+browserConnectJson);
	}

	globalState.exampleSocket.onerr = function(event) {
		console.log("An error occurred: "+event)
		
		// Return if we've already disposed.
		// reestablishConnection();		
	}
	
	globalState.exampleSocket.onmessage = function(event) {
		
		var json = jQuery.parseJSON(event.data);

		if(debugMessagesReceived <= 5) {
			// console.log("Received:");
			debugMessagesReceived++;
			// console.log(json);
		}
		
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
			loadAndCall(event, drawFrame);
		} else {
			drawFrame(event);
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
		canvasJs(spriteSize, myCanvas, consoleDomElement, leaderboardDomElement, metricsDomElement, viewTypeLocal, optionalUuid);
	}, 200);
	if(globalState.exampleSocket != null) {
		globalState.exampleSocket.close();
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
	
	if(json.combatEvents != null && json.combatEvents.length > 0) {
		
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
			
			globalState.entityList.push(newEntry);
		}
		
	}	
}


globalState.interval = setInterval( function() {
	
	var frameQueue = globalState.frameQueue;

	//	console.log("fq length: "+frameQueue.length);

	// Depending on how far behind we are in drawing the latest frames, we wait between 30 and 100 msecs.
	var minimumElapsed = 30+7*Math.max(0, (10-frameQueue.length));	
	if(window.performance.now() - globalState.lastFrameDrawTime < minimumElapsed ) {
		return;
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
		
//		console.log("lowest frame: "+lowestFrame);
		
		for(var x = 0; x < frameQueue.length; x++) {
			
			if(frameQueue[x].frame < lowestFrame) {
				frameQueue.splice(x, 1);
				x--;
				
			} else if(frameQueue[x].frame == lowestFrame) {
				globalState.nextFrameId = lowestFrame+1;
				drawFrameNewer(frameQueue[x],  /*globalState.nextFrameId % 10 != 0 &&*/ frameQueue.length > 10  );
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
}, 5);

function drawFrame(event) {
	
	var frameJson = jQuery.parseJSON(event.data);			
	globalState.frameQueue.push(frameJson);
	
	if(globalState.frameQueue.length > 100) {
		console.log("Closing!");
		globalState.exampleSocket.close();
	}

}

function disposeGlobalState() {
	globalState.mouseMoveEnabled = false;
	// globalState.exampleSocket.onclose = null;
	// globalState.exampleSocket.onmessage = null;
	// globalState.exampleSocket = null;
	globalState.ctx = null;
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

//	globalState = {};	
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


function drawFrameNewer(param, skipdraw) {
	
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

	if(globalState.viewType != "SERVER_VIEW_WORLD") {
		actualWidth = Math.floor(myCanvas.width/spriteSize)+2;
		actualHeight = Math.floor(myCanvas.height/spriteSize)+2;
		
		let centerPointX = Math.floor(param.currViewWidth/2)+param.currWorldPosX;
		let centerPointY = Math.floor(param.currViewHeight/2)+param.currWorldPosY;
	
		centerPointX -= Math.floor(actualWidth/2);
		centerPointY -= Math.floor(actualHeight/2);
	
		startX = centerPointX;
		startY = centerPointY;

	} else {

		actualWidth = param.currViewWidth;
		actualHeight = param.currViewHeight;
		startX = param.currWorldPosX;
		startY = param.currWorldPosY;
	}

	if(skipdraw) {
		console.log("skipping draw of "+param.frame);
	}
		
	var currRedrawManager = new RedrawManager(spriteSize*5, spriteSize*5);
//	globalState.dirtyRedraw = currRedrawManager;
		
	if(param.currWorldPosX != globalState.currWorldX || param.currWorldPosY != globalState.currWorldY || globalState.currWorldX == null || globalState.currWorldY == null) {
		currRedrawManager.flagPixelRect(0, 0, param.currViewWidth*spriteSize, param.currViewHeight*spriteSize);
	}
	
	// An array of delta frames: a delta frame is a rectangles of tiles that were updated in the world { tile_x, tile_y, width of rect, height of rect, data : [ tile data ] }
	// See BrowserWebSocketClientShared
	var mapData = param.frameData;
	
//	if(!globalState.contextWidthSet) {
//		// I think this whole block can be removed
//		ctx.canvas.width = window.innerWidth;
//		ctx.canvas.height = window.innerHeight;
//		globalState.contextWidthSet = true;
//	}

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
		drawFrameInner(actualWidth, actualHeight, startX, startY, param, skipdraw, currRedrawManager);
	}
	 
	globalState.currWorldX = param.currWorldPosX;
	globalState.currWorldY = param.currWorldPosY;
	globalState.startX = startX;
	globalState.startY = startY;

}

function drawFrameInner(actualWidth, actualHeight, startX, startY, param, skipdraw, currRedrawManager) {

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
			
			let col = entity.worldx - startX; // param.currWorldPosX;
			let row = entity.worldy - startY; // param.currWorldPosY;
			
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
		
		var worldCol = Math.max(0, Math.floor(pageX/globalState.spriteSize) + globalState.startX - 4);
		var worldRow = Math.max(0, Math.floor(pageY/globalState.spriteSize) + globalState.startY - 4);
		
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



}

