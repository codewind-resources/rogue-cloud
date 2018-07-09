'use strict';

// Global utility functions -----------------------

function generateUuid() {
	function gen_rand4() {
		return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
	}
	return gen_rand4() + gen_rand4() + '-' + gen_rand4() + '-' + gen_rand4() + '-' + gen_rand4() + '-' + gen_rand4() + gen_rand4() + gen_rand4();
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

// Global constants -------------------------

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
