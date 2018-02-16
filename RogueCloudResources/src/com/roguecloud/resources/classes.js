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

class RedrawManager {
	
	constructor(gridPixelsWidth, gridPixelsHeight) {
		this.gridPixelsWidth = gridPixelsWidth;
		this.gridPixelsHeight = gridPixelsHeight;
		this.map = new Map();
		
	}
	
	flagPixel(x, y) {
		x = Math.floor(x / this.gridPixelsWidth);
		y = Math.floor(y / this.gridPixelsHeight);
		
		this.map.set(x * 32768 + y, true);
	}

	flagPixelRect(px, py, pw, ph) {
		
		for(var x = px; x <= px + pw + this.gridPixelsWidth; x += this.gridPixelsWidth) {
			for(var y = py; y <= py + ph + this.gridPixelsHeight; y += this.gridPixelsHeight) {
				this.flagPixel(x, y);
			}
			
		}
	}
	
	getByPixel(px, py) {
		px = Math.floor(px / this.gridPixelsWidth);
		py = Math.floor(py / this.gridPixelsHeight);
		
		return this.map.get(px * 32768 + py);
	}
	
	outputGrid(pwidth, pheight) {
		for(var y = 0; y < pheight; y++) {
			var str ="";
			for(var x = 0; x < pwidth; x++) {
				str += this.map.get(x * 32768 + y) ? "1" : "0";
			}
			console.log(str);
		}
		
	}
	
	clear() {
		this.map.clear();
	}

}

