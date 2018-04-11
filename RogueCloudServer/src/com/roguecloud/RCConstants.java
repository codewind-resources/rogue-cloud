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

import java.util.concurrent.TimeUnit;

/** In this class, game experience specific values can be tweaked to alter various aspects of gameplay. */
public class RCConstants {

	public static final long ROUND_LENGTH_IN_NANOS = TimeUnit.NANOSECONDS.convert(60 * 5, TimeUnit.SECONDS);
	
	public static final long TIME_BETWEEN_ROUNDS_IN_NANOS = TimeUnit.NANOSECONDS.convert(20, TimeUnit.SECONDS);
	
	public static final long MSECS_PER_FRAME = 100;
	
	public static final long NS_PER_FRAME = TimeUnit.NANOSECONDS.convert(MSECS_PER_FRAME, TimeUnit.MILLISECONDS);
	
	public static final long REVIVE_DEAD_PLAYER_AFTER_X_TICKS = 100;

	public static final long TIME_TO_FOLLOW_PLAYER_IN_WORLD_VIEW_NANOS = TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS);

	public static final long LINGER_ON_DEAD_CREATURE_FOR_X_TICKS = 50;
	
	public static final double PLAYER_HP_REDUCTION_ON_DEATH = 0.8d; 

	/* Values below this line must be carefully updated, as they could affect the ability of the Javascript to 
	 * correct display the world, in the browser. */
	
	public static final int WORLD_WIDTH = 161;
	public static final int WORLD_HEIGHT = 191;
	
	public static final int AGENT_CLIENT_VIEW_WIDTH = 80;
	public static final int AGENT_CLIENT_VIEW_HEIGHT = 40;

	public static final int SERVER_FOLLOW_WIDTH = 40;
	public static final int SERVER_FOLLOW_HEIGHT = 40;

}
