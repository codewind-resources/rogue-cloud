## Introduction

To enter a game round, an agent connects to the server on the client WebSocket URL `(server URL)/api/client`. The client sends a `ClientConnection` message, and the server replies. From that point on, the server will send the client 10 frame-updates-per-second, which correspond to what is happening in the part of the world that the agent can see. Every tenth-of-a-second, the client has the opportunity to send an action to the server, such as move, attack, pick up an item, and so on. 

## All messages between the client and server are JSON messages

The contents and format of all JSON messages you see are based on the `Json*` Java classes. If the name of a Java class name begins with `Json`, then its main purpose is to be converted to/from JSON strings. Each of the fields of `Json*` Java classes correspond to a field in the JSON message that is sent between the server and the client.

For example, `JsonClientConnect.java` includes these fields:
```
	private String uuid;
	
	private String username;
	
	private String password;
	
	private String clientVersion;
	
	private Long roundToEnter;

	Long lastActionResponseReceived = null;
	
	private boolean initialConnect = false;
```

When this class is serialized (Java class is turned into JSON) or deserialized (the JSON is turned back into a Java class) each of these fields you see above corresponds exactly to the fields in the JSON payload. Here is a sample of the JSON produced by the above class.
```
{"type":"ClientConnect","uuid":"b061b94c-ca3c-47a3-a2b4-c6beebf5eda2","username":"my-username","password":"mypassword","clientVersion":"18.5","roundToEnter":null,"lastActionResponseReceived":-1,"initialConnect":true}
```
You can see above how the names of the Java fields correspond to the names of the properties of the JSON object. This is true when (de-)serializing between any Java class. You will also see a field 'type' which is not included in the list above. This comes from the superclass of the `JsonClientConnect` object, and is used to determine what type of message is being sent. That field will always contain a unique name which indicates the message type. In this case it is ClientConnect.

In Java, conversion to and from JSON is performed using the Jackson library. This is performed using Jackson's `ObjectMapper` class, which you will see throughout the code. The primary methods we use from `ObjectMapper` are `readValue(...)` (convert a JSON string to Java) and `writeValueAsString(...)` (convert a Java object to JSON string).


## Communication between client and server during a traditional game round

All messages between the client and server, including actions and frame updates, are JSON objects (see examples below).

#### Initial WebSocket connect to the (server URL)/api/client endpoint:
```
client -> server: {"type":"ClientConnect","uuid":"b061b94c-ca3c-47a3-a2b4-c6beebf5eda2","username":"jgwjgw","password":"jgwjgw","clientVersion":"18.5","roundToEnter":null,"lastActionResponseReceived":-1,"initialConnect":true}
```

```
server -> client: {"type":"ClientConnectResponse","connectResult":"SUCCESS","roundEntered":9851}
	connectResult can also be: { SUCCESS, FAIL_INVALID_CREDENTIALS, FAIL_ROUND_OVER, FAIL_ROUND_NOT_STARTED, FAIL_OTHER, FAIL_INVALID_CLIENT_API_VERSION }
```

#### Frame updates

After a sucessful connection, the server will send frame updates 10 times a second, which indicates what is happening in the 80x40 area around the player's current position:
```
server -> client: {"type":"JsonFrameUpdate","gameTicks":1,"frame":1,"selfState":{"inventory":[],"playerId":60},"worldState":{"clientViewPosX":82,"clientViewPosY":186,"clientViewWidth":80,"clientViewHeight":40,"worldWidth":366,"worldHeight":400,"roundSecsLeft":299,"drinkables":[],"weapons":[{"name":"Bare Hands","numAttackDice":1,"attackDiceSize":4,"attackPlus":0,"hitRating":20,"type":"One-handed","id":20,"tile":341}],"armours":[],"frames":[{"x":0,"y":0,"w":80,"h":40,"data":{} ],"visibleCreatures":[{"position":{"x":122,"y":206},"creatureId":60,"maxHp":250,"currHp":250,"level":1,"weaponId":20,"tileTypeNumber":1733,"name":"jgwjgw","armourIds":[],"effects":[],"player":true}],"visibleObjects":[],"tileProperties":[],"events":[]},"full":false}
```

*Included in this frame update JSON are*:
- What the current frame/game tick is. The game world is simulated (known as game engine tick rate) 10 times a second. This means that your character can perform 10 actions per second (and so can the monsters/players around your character!)
- What the player's inventory contains
- What weapons/armour the player has equipped
- What creatures the character can currently see
	- where are they in the world
	- what weapon/armour are they carrying, how much health do they have, etc.
- How large the world is (this is currently hardcoded to 366 tiles width by 400 tiles height)
- How much of the world the player can currently see (this is currently hardcoded to 80 tiles wide by 40 tiles height)
- Events that occurred in the previous frame (did other characters move or attack? did they drink a potion? etc)
- Any items (weapons, armour, potions) that are lying on the ground (which any character can pick up).

The contents of this JSON message can be found in `JsonFrameUpdate.java`. See the [advanced world state topic for more information](RogueCloudJsonWorldState.md) about this object.


#### UI Updates

The server will also periodically send updates about the player's score, and the current round. These are mainly used by the browser-based UI, but can be used by client logic as well.
```
{"type":"JsonUpdateBrowserUI","currentPlayerScore":1,"roundState":{"type":"ACTIVE","roundId":9851,"timeLeftInSeconds":299}}
```
The contents of this JSON message can be found in `JsonUpdateBrowserUI`.


#### The client will send Actions to the server if it wants to do a particular action

The client will send Actions to the server if it wants to do a particular action, and the server will respond to let the client know if the action succeeded. An action can fail if it is invalid (for example, attempting to step into a wall), otherwise it will succeed.

```
client -> server:
	{"type":"JsonActionMessage","action":{"type":"JsonStepAction","destination":{"x":122,"y":207}},"messageId":0}

server -> client:
	{"type":"JsonStepActionResponse","failReason":null,"newPosition":{"x":122,"y":213},"success":true}}	
```

All the actions, and their responses, can be found in the `com.roguecloud.json.actions` package. Each action (sent by the client) has a corresponding action response (sent by the server.) 



#### The server will periodically send a health check to the client, which the client will need to respond to. 
```
server -> client: {"type":"JsonHealthCheck","id":0}
client -> server {"type":"JsonHealthCheckResponse","id":0}
```


## Eavesdropping on communication between the server and the client

To see what the client and server are sending to each other, you can enable logging of client communication. Open the com.roguecloud.utils.Logger class and set these fields to true, like so:
```
	public static final boolean CLIENT_SENT = true;
	public static final boolean CLIENT_RECEIVED = true;
```

You will then see log statements like so:
```
Client received message: {"type":"JsonFrameUpdate","gameTicks":2,"frame":2,"selfState":{"inventory":[],"playerId":60},"worldState":{"clientViewPosX":82,"clientViewPosY":186,"clientViewWidth":80,"clientViewHeight":40,"worldWidth":366,"worldHeight":400,"roundSecsLeft":299,"drinkables":[],"weapons":[],"armours":[],"frames":[{"x":40,"y":20,"w":1,"h":1,"data":[[1,[1733],[201]]]}],"visibleCreatures":[{"position":{"x":122,"y":206},"creatureId":60,"maxHp":250,"currHp":250,"level":1,"weaponId":20,"tileTypeNumber":1733,"name":"jgwjgw","armourIds":[],"effects":[],"player":true}],"visibleObjects":[],"tileProperties":[],"events":[]},"full":false}

Client sending text: {"type":"JsonActionMessage","action":{"type":"JsonStepAction","destination":{"x":122,"y":207}},"messageId":0}
```
