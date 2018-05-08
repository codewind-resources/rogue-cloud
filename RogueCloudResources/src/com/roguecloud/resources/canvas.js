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
	
var secondaryCanvas = document.createElement("canvas"),
	secondaryCtx = secondaryCanvas.getContext("2d");

secondaryCtx.canvas.width = myCanvas.width;
secondaryCtx.canvas.height = myCanvas.height;

var currCanvasWidth = myCanvas.width;
var currCanvasHeight = myCanvas.height;


var secondaryCtxDrawn = false;

var mostRecentCreaturesList = null;
	

var SPREAD = [
	[0, -1], // N
	[1, -1], // NE
	[1, 0], // E
	[1, 1], // SE
	[0, 1], // S
	[-1, 1], // SW
	[-1, 0], // W
	[-1, -1] // NW
];

myCanvas.addEventListener("mouseout", function(e) {
	var element = document.getElementById('creatureInfo'); 
	element.innerHTML = ""; 
	element.style.display="none";

});

myCanvas.addEventListener("mousemove", function(e) {
	
	if(!globalState.mouseMoveEnabled) {
		return;
	}

	// if(mostRecentCreaturesList != null)	 {

	// 	for(var x = 0; x < mostRecentCreaturesList.length; x++) {
			
	// 		var creature = mostRecentCreaturesList[x];

	// 	}

	// }


	var pageX = e.clientX;
	var pageY = e.clientY;
	
	var worldCol = Math.max(0, Math.floor(pageX/globalState.spriteSize) + globalState.startX - 4);
	var worldRow = Math.max(0, Math.floor(pageY/globalState.spriteSize) + globalState.startY - 4);
	
	var localTileMap = new Map();
	
	var newHtml = "<span id='creatureInfoSpan'>";
	
	newHtml += "<table>";
	
//	newHtml += ""+worldCol+" "+worldRow+" ";
	
	var containsValue = false;
	
	for(var x = worldCol; x < worldCol+8; x++) {
		for(var y = worldRow; y< worldRow+8; y++) {
			var tiles = getFromDataMap(x, y, globalState.currWorldX, globalState.currWorldY);
			if(tiles == null) { continue; }
			localTileMap.set(tiles[0].num, tiles[0].num );
		}
	}
	
	localTileMap.forEach(function(value, key) {
		
		var name = globalState.globalTileMap.get(value);
		if(name == null || name.trim().length == 0) {
			return;
		}
		newHtml += "<tr><td><img src='resources/tiles/"+value+".png' width='42' height='42'/></td><td><span style='margin-left: 20px; margin-right: 20px;'>"+name+"</span></td>"
		containsValue = true;
//		newHtml += "<img src='resources/tiles/"+value+".png' width='42' height='42'/>&nbsp;&nbsp;"+name+"<br/>"

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


var debugMessagesReceived = 0;


var globalState;

{
	globalState =  {};
	globalState.dirtyRedraw = null;
	
	globalState.viewType = viewType;
	globalState.damageGradient = generateDamageGradient();
	globalState.globalTileMap = new Map(/* tile number -> tile name */);
	globalState.imagesLoaded = false;
	
	for(var x = 0; x < GLOBAL_TILES_JSON.length; x++) {
		globalState.globalTileMap.set(GLOBAL_TILES_JSON[x][0], GLOBAL_TILES_JSON[x][1]);
	}

	globalState.nextFrameId = -1;
	
	globalState.contextWidthSet = true /*false*/;
	
	globalState.spriteSize = spriteSize;
	
	globalState.currWorldX = -1;
	globalState.currWorldY = -1;
	globalState.frameQueue = [];
	
	globalState.entityList = [];
	
	// globalState.entitySpreadMap = new Map( /* x*y coord -> spread */);
	
	globalState.prevDataMap = new Map( /* x*y coord -> num+rot */);

	globalState.dataArray = null;
	
	globalState.imageMap = new Map( /* num -> img */);
	
	globalState.ctx = myCanvas.getContext("2d");
	
	console.log("pre?");
	try {
		
		var loc = window.location, new_uri;
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
			
			if(json.newEventHtml != null) {
				for(var x = 0; x < json.newEventHtml.length; x++) {
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
				
				for(var c = 0; c < json.combatEvents.length; c++) {
					var entry = json.combatEvents[c];
					var spread = SPREAD[Math.floor(SPREAD.length * Math.random())]

					var magnitudeSpriteSize = spriteSize > 10 ? spriteSize : 5;
					
					var magnitude = Math.floor(7 * (magnitudeSpriteSize/22));
					
					var newEntry = {
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
			
			return;
		} else {
			
			if(viewType == "SERVER_VIEW_FOLLOW" && globalState.nextFrameId % 10 == 0) {
//				console.log(json);
			}
			
		}
		
		if(globalState.nextFrameId == -1) {
			var frameJson = json; //jQuery.parseJSON(event.data);
			globalState.nextFrameId = frameJson.frame;
		}
		
		if(!globalState.imagesLoaded) {
			console.log("loadAndCall called.");
			loadAndCall(event, drawFrame);
		} else {
			drawFrame(event);
		}
		
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
	}
	
//	globalState.consoleDomElement = consoleDomElement;
	
	globalState.console = new Array();
	
	globalState.mouseMoveEnabled = true;
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

addEvent(window, "resize", function(event) {
	if(currCanvasWidth != myCanvas.width || currCanvasHeight != myCanvas.height) {
		currCanvasWidth = myCanvas.width;
		currCanvasHeight = myCanvas.height;
		console.log("resize in canva?")
		reestablishConnection();
	}
});


var lastFrameTime = 0;


globalState.interval = setInterval( function() {
	
	var frameQueue = globalState.frameQueue;

//	console.log("fq length: "+frameQueue.length);

	// Depending on how far behind we are in drawing the latest frames, we wait between 30 and 100 msecs.
	var minimumElapsed = 30+7*Math.max(0, (10-frameQueue.length));	
	if(window.performance.now() - lastFrameTime < minimumElapsed ) {
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
		
		outer: for(var x = 0; x < frameQueue.length; x++) {
			
			if(frameQueue[x].frame < lowestFrame) {
				frameQueue.splice(x, 1);
				x--;
				
			} else if(frameQueue[x].frame == lowestFrame) {
				globalState.nextFrameId = lowestFrame+1;
				drawFrameNewer(frameQueue[x],  /*globalState.nextFrameId % 10 != 0 &&*/ frameQueue.length > 10  );
				frameQueue.splice(x, 1);
				frameFound = true;
				lastFrameTime = window.performance.now();
				
				return;
				// break outer;
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
	globalState.entityList = {};
	globalState.dirtyRedraw = {};
	clearInterval(globalState.interval);
//	globalState = {};	
}



function addToDataMap(x, y, worldXSize, worldYSize, imageNumber, rotation, layerIndex, layerSize) {
	
	var entry = getFromDataMap(x, y); 
	if(entry == null || entry.length != layerSize) {
		entry = [];
// 				entry = new int[layerSize];
		globalState.prevDataMap.set(x*32768+y, entry);
	}

	entry[layerIndex] = {"num" : imageNumber,
					"rot" : rotation };
	
}

function getFromDataMap(x, y, worldXSize, worldYSize) {
	return globalState.prevDataMap.get(x*32768+y);
}


function drawFrameNewer(param, skipdraw) {
	var ctx = globalState.ctx;
	
	var spriteSize = globalState.spriteSize;	

	var startX = 0, startY = 0;	
	var actualWidth = 0, actualHeight = 0;

	if(param.fullSent == true) {
		skipdraw = false;
	}

	if(globalState.viewType != "SERVER_VIEW_WORLD") {
		actualWidth = Math.floor(myCanvas.width/spriteSize)+2;
		actualHeight = Math.floor(myCanvas.height/spriteSize)+2;
		
	//	var startX = param.currWorldPosX;
	//	var startY = param.currWorldPosY;
		
		var centerPointX = Math.floor(param.currViewWidth/2)+param.currWorldPosX;
		var centerPointY = Math.floor(param.currViewHeight/2)+param.currWorldPosY;
	
		centerPointX -= Math.floor(actualWidth/2);
		centerPointY -= Math.floor(actualHeight/2);
	
		// centerPointX++;
		// centerPointY++;
		
		startX = centerPointX;
		startY = centerPointY;
	} else {
		actualWidth = param.currViewWidth;
		actualHeight = param.currViewHeight;
		startX = param.currWorldPosX;
		startY = param.currWorldPosY;
	}

	
	if(!skipdraw && secondaryCtxDrawn) {
		// ctx.fillStyle="#000000";
		// ctx.fillRect(0, 0, 350, 350);		
		ctx.drawImage(secondaryCanvas, 0, 0);
		// ctx.drawImage(secondaryCanvas, 0, 820); // , 300, 266);
	}

	
	if(skipdraw) {
		console.log("skipping draw of "+param.frame);
	}
	
	
	var currRedrawManager = globalState.dirtyRedraw;
	globalState.dirtyRedraw = new RedrawManager(spriteSize*5, spriteSize*5);
	
	if(currRedrawManager == null) {
		
		// First frame
		currRedrawManager = new RedrawManager(spriteSize*5, spriteSize*5);
		
		// if(globalState.viewType != "SERVER_VIEW_WORLD") {
			ctx.fillStyle="rgb(174, 223, 101)";
		// } else {
		//	ctx.fillStyle="#349D3D";	
		// }
		
		ctx.fillRect(0, 0, ctx.canvas.width-200, ctx.canvas.height);

		ctx.fillStyle="#27782e";
		
		ctx.fillRect(ctx.canvas.width-200, 0, ctx.canvas.width, ctx.canvas.height);
		
		
		
	}
	
	
//	ctx.fillStyle="#CC66CC";
//	ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);

	
	if(param.currWorldPosX != globalState.currWorldX || param.currWorldPosY != globalState.currWorldY || globalState.currWorldX == null || globalState.currWorldY == null) {
		currRedrawManager.flagPixelRect(0, 0, param.currViewWidth*spriteSize, param.currViewHeight*spriteSize);
	}
	
	var mapData = param.frameData;
	
	if(!globalState.contextWidthSet) {
		ctx.canvas.width = window.innerWidth;
		ctx.canvas.height = window.innerHeight;
		globalState.contextWidthSet = true;
	}
		
	var numDelta = mapData.length;
	for(var deltaIndex = 0; deltaIndex < numDelta; deltaIndex++) {
		
		var deltaBody = mapData[deltaIndex];
		
		var width = deltaBody.w;
		var height = deltaBody.h;
		var dbx = deltaBody.x;
		var dby = deltaBody.y;
		
		if(true || globalState.viewType == "SERVER_VIEW_WORLD") {
			
			var x = (dbx+param.currWorldPosX-startX);
			var y = (dby+param.currWorldPosY-startY);
			if(x < 0 ) {
				x = 0;
			}
			if(y < 0) {
				y = 0;
			}
			currRedrawManager.flagPixelRect(x*spriteSize, y*spriteSize, width*spriteSize, height*spriteSize);

			
//			currRedrawManager.flagPixelRect(dbx*spriteSize, dby*spriteSize, width*spriteSize, height*spriteSize);
			
		}
		
//		if(width*height > 1000 && globalState.viewType == "SERVER_VIEW_WORLD") {
//			console.log("update: "+width+" "+height);
//		}
				
		var arrayContents = deltaBody.data;
		
		var count = 0;
		
		for(var x = 0; x < arrayContents.length; x++) {
			
			var col = (x % width+dbx);
			var row = (Math.floor(x / width)+dby);
			
			var colPixel = col * spriteSize;
			var rowPixel = row * spriteSize;

			
			for(var layerIndex = arrayContents[x].length-1; layerIndex >= 0; layerIndex--) {
				
				var num = arrayContents[x][layerIndex][0];
				
				var img = globalState.imageMap.get(num);

				var rotation = arrayContents[x][layerIndex].length == 1 ? 0 : arrayContents[x][layerIndex][1];
				
				var worldX = col+param.currWorldPosX;
				var worldY = row+param.currWorldPosY;
				
				addToDataMap(worldX, worldY, param.currViewWidth, param.currViewHeight, num, rotation, layerIndex, arrayContents[x].length);

			}
			
		}
		
		
		
// 				for(var x = 0; x < param.currViewWidth; x++) {
// 					for(var y = 0; y < param.currViewHeight; y++) {
// 						var cachedTile = getFromDataMap(param.currWorldPosX+x, param.currWorldPosY+y);
// 						if(cachedTile != null) {
// 							var img = imageMap.get(cachedTile.num);
// 							if(img != null) {
// // 								jgwCount++;
// 	 							drawRotatedImage(ctx, img, x*spriteSize, y*spriteSize, cachedTile.rot);
// 							}
// 						}
// 					}
// 				}

		
	} // end deltaIndex for

	if(!skipdraw) {
		

//		ctx.fillStyle="#FFFFFF";
//		ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);		
		
		
//		for(var x = 0; x < param.currViewWidth; x++) {
//			for(var y = 0; y < param.currViewHeight; y++) {
//				var cachedTile = getFromDataMap(param.currWorldPosX+x, param.currWorldPosY+y, param.currViewWidth, param.currViewHeight);
//				if(cachedTile != null) {
//					for(var layerIndex = cachedTile.length-1; layerIndex >= 0; layerIndex--) {
//						var img = globalState.imageMap.get(cachedTile[layerIndex].num);
//						if(img != null) {
////							drawRotatedImage(ctx, img, x*spriteSize, y*spriteSize, cachedTile[layerIndex].num,  cachedTile[layerIndex].rot);
//						}							
//					}
//				} 
//			}
//		}

		
		if(true || globalState.viewType == "SERVER_VIEW_WORLD") {
			
//			actualWidth = Math.floor(myCanvas.width/spriteSize)+2;
//			actualHeight = Math.floor(myCanvas.height/spriteSize)+2;
//			
////			var startX = param.currWorldPosX;
////			var startY = param.currWorldPosY;
//			
//			var centerPointX = Math.floor(param.currViewWidth/2)+param.currWorldPosX;
//			var centerPointY = Math.floor(param.currViewHeight/2)+param.currWorldPosY;
//
//			centerPointX -= Math.floor(actualWidth/2);
//			centerPointY -= Math.floor(actualHeight/2);
//
//			startX = centerPointX;
//			startY = centerPointY;
			
//			console.log("starting: "+startX+" "+startY);
			
			for(var x = 0; x < actualWidth; x++) {
				for(var y = 0; y < actualHeight; y++) {
					
					var redraw = currRedrawManager.getByPixel(x*spriteSize, y*spriteSize);
					// redraw = true;
					if(redraw) {
						var cachedTile = getFromDataMap(startX+x, startY+y, param.currViewWidth, param.currViewHeight);
						
						if(cachedTile != null) {
							for(var layerIndex = cachedTile.length-1; layerIndex >= 0; layerIndex--) {
								var layer = cachedTile[layerIndex];
								
								var img = globalState.imageMap.get(layer.num);
								if(img != null) {
									drawRotatedImage(ctx, img, x*spriteSize, y*spriteSize, layer.num,  layer.rot);
								}							
							}
						}
						
					}
				}
			}	
		}

		secondaryCtx.drawImage(myCanvas, 0, 0);
		secondaryCtxDrawn = true;
		// primaryCtx.drawImage(secondaryCanvas);
		
		mostRecentCreaturesList = param.creatures;
		for(var x = 0; x < param.creatures.length; x++) {
			
			var creature = param.creatures[x];
			var percent = Math.max(0, creature.hp) / creature.maxHp;
			
			var posX = creature.position[0]-startX;
			var posY = creature.position[1]-startY;

			if(posY >= actualHeight-2) {
				continue;
			}
			
			var percentIndex = Math.max( 0, Math.min(99, 100*percent));
			
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
				
				var textSizeWidth = ctx.measureText(creature.username).width;
				var textSizeHeight = ctx.measureText("M").width;
				
				var yDelta = globalState.viewType == "SERVER_VIEW_WORLD" ? 10: 0;
				
				var xPos = posX*(spriteSize) + Math.floor(spriteSize/2) - Math.floor(textSizeWidth/2)
				
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
//			serverViewFollowPos = { x : startX, y : startY, w: param.currViewWidth, h: param.currViewHeight };
			
			
		} else if(globalState.viewType == "SERVER_VIEW_WORLD") {
			
			// Blue rectangle 
			if(serverViewFollowPos != null) {
				
				var lineWidth = 3;
				ctx.beginPath();
				ctx.lineWidth=""+lineWidth;
				ctx.strokeStyle="#000099";
				
				var rectX = (serverViewFollowPos.x*spriteSize)-lineWidth;
				var rectY = (serverViewFollowPos.y*spriteSize)-lineWidth;
				
				ctx.rect(rectX, rectY, serverViewFollowPos.w*spriteSize, serverViewFollowPos.h*spriteSize);
				// globalState.dirtyRedraw.flagPixelRect( (rectX)-lineWidth, (rectY)-lineWidth, (serverViewFollowPos.w*spriteSize)+(lineWidth*2), (serverViewFollowPos.h*spriteSize)+(lineWidth*2) );
				ctx.stroke();
			}
			  
		}
		
		
//		if(viewType != "SERVER_VIEW_FOLLOW") {
//			console.log("-----------------------");
//			for(var gx = 0; gx < gridWidth; gx++) {
//				for(var gy = 0; gy < gridHeight; gy++) {
//					var redrawGrid = redrawMap.map.get(gx*32768/*gridHeight*/+gy);
//					if(redrawGrid == true) {
//	//					console.log("("+gx+", "+gy+")");
//						
//						console.log("("+gx+","+gy+") -> "+gx*redrawMap.gridSizeX*spriteSize+" "+gy*redrawMap.gridSizeY*spriteSize);
//						
//						ctx.fillStyle="black";
//						ctx.fillRect(gx*redrawMap.gridSizeX*spriteSize, gy*redrawMap.gridSizeY*spriteSize, redrawMap.gridSizeX*spriteSize, redrawMap.gridSizeY*spriteSize);
//	//					ctx.fillRect(gx*gridWidth*spriteSize, gy*gridHeight*spriteSize, gridWidth*spriteSize, gridHeight*spriteSize);
//						
//	//					ctx.beginPath();
//	//					ctx.lineWidth="2";
//	//					ctx.strokeStyle="#FF0000";
//	//					ctx.rect(gx*gridWidth, gy*gridHeight, gridWidth, gridHeight);
//	//					ctx.stroke();
//	
//					}
//				}
//			}
//		}

		
		// Draw frame rate
		ctx.fillStyle="white";	
		ctx.font = '15px sans-serif';
		ctx.fillText(param.frame, 20, 20);
		// globalState.dirtyRedraw.flagPixelRect(10, 0, 100, 70); 
	} // end skip draw

	
	// Draw floating damage text
	if(!skipdraw && globalState.entityList != null) {
		
		var fontSize = 20;
		
		if(spriteSize < 10) {
			fontSize = 10;
		}
		
		for(var c = 0; c < globalState.entityList.length; c++) {
			var entity = globalState.entityList[c];

			entity.frame++;
			
			var col = entity.worldx - startX; // param.currWorldPosX;
			var row = entity.worldy - startY; // param.currWorldPosY;
			
			entity.xoffset += entity.directionX*5;
			entity.yoffset += entity.directionY*5;
			
			
			if(row >= 0 && col >= 0 && row < param.currViewHeight && col <= param.currViewWidth) {
				var colX = col*globalState.spriteSize;
				var colY = row*globalState.spriteSize;
				
				// Don't draw text outside the visible world on the canvas 
				if(colX + entity.xoffset < actualWidth*spriteSize && colY + entity.yoffset < actualHeight*spriteSize) {
					
					ctx.fillStyle="red";
					ctx.font = fontSize+'px sans-serif';
					ctx.fillText(entity.damage, colX+entity.xoffset, colY+entity.yoffset);
					// globalState.dirtyRedraw.flagPixelRect(colX+entity.xoffset-30, colY+entity.yoffset-30, 60, 60);
					
				}
			}
			
			if(entity.frame > 10) {
				globalState.entityList.splice(c,1);
				c--;
			}
		}
	}
	
	if(!skipdraw && globalState.viewType == "SERVER_VIEW_WORLD") {
	      ctx.beginPath();
	      ctx.lineWidth=1;
	      ctx.fillStyle="rgb(0, 0, 0)";
	      ctx.moveTo(0, 0);
	      ctx.lineTo(0, 2000);
	      ctx.stroke();
	}
	
	globalState.currWorldX = param.currWorldPosX;
	globalState.currWorldY = param.currWorldPosY;
	globalState.startX = startX;
	globalState.startY = startY;
	
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


var scaleCache = new Map();


//var scaleNum =0;
//var scaleCached = 0;
//var scaleUncached = 0;

var offscreenCanvas = document.createElement('canvas');
var offscreenContext = offscreenCanvas.getContext('2d');
offscreenCanvas.width = spriteSize; // 300px;
offscreenCanvas.height = spriteSize; //300px;


var TO_RADIANS = Math.PI/180; 
function drawRotatedImage(context, image, x, y, num, angle) {

	var spriteSize = globalState.spriteSize;
	
	// Fast path
	if(angle == 0) {

//		scaleNum++;
//		if(scaleNum % 100 == 0) {
//			console.log(scaleNum+" "+scaleCached+" "+scaleUncached);
//		} 

		var inCache = false;
		
		var val = scaleCache.get(num);
		if(val != null) {
			inCache = true;
			if(val.imgdata.complete) {				
				context.drawImage(val.imgdata, x, y);
				return;
			} 		
		}
		
//		scaleUncached++;
		
		
		context.drawImage(image, x, y, spriteSize, spriteSize);

//		offscreenContext.fillStyle="#FFFFFF";
//		offscreenContext.clearRect(0, 0, 300, 300);


		if(!inCache) {
			offscreenContext.clearRect(0, 0, spriteSize, spriteSize);
			offscreenContext.drawImage(image, 0, 0, spriteSize, spriteSize);
		
			var newimg = new Image();
//			offscreenContext.getImageData(0, 0, spriteSize, spriteSize);
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
/*
function myCanvas(myMap) {

	var spriteSize = globalState.spriteSize;
	
	var c = document.getElementById("myCanvas");
	var ctx = c.getContext("2d");
	ctx.canvas.width = window.innerWidth;
	ctx.canvas.height = window.innerHeight;
	
	var arrayContents = mapData.data;
	
	for(var x = 0; x < arrayContents.length; x++) {
	
		var col = (x % mapData.width) * spriteSize;
		var row = Math.floor(x / mapData.width) * spriteSize;

		var img = myMap.get(arrayContents[x][0])

		var rotation = arrayContents[x] == 1 ? 0 : arrayContents[x][1];
		
		drawRotatedImage(ctx, img, col, row, rotation);
		
	}
	
} */

function generateUuid() {
	function gen_rand4() {
		return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
	}
	return gen_rand4() + gen_rand4() + '-' + gen_rand4() + '-' + gen_rand4() + '-' + gen_rand4() + '-' + gen_rand4() + gen_rand4() + gen_rand4();
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

function convertToHex(x) {
	var str = x.toString(16);
	return (str.length==1) ? "0"+str : str;
}

function convertSecondsToMinutes(totalSeconds) {
	
	var minutes = Math.floor(totalSeconds/60);
	
	var seconds = totalSeconds - (minutes*60);
	
	
	return minutes+":"+(seconds < 10 ? "0"+seconds : seconds);
	
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
	
//	var element = document.getElementById('console'); 
	consoleDomElement.innerHTML = "<span id='console_span'><b>Event Log</b>:<br/><br/>"+str+"</span>"; 

}

function formatScore(score) {
	return score.toLocaleString();
}

}

