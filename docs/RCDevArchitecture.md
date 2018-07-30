Additional information for all of the  classes listed below is available from the class-level documentation at the top of those class files. All classes in the project contain a description of the purpose of the class, and how it interfaces with others, in the Javadocs comments at the top of the class (with some exceptions, like `Json*` classes).

## Rogue Cloud overview by source project

The Rogue Cloud codebase consists of five projects. These are the central projects in the Git repository, and if you import these from Eclipse they correspond to Eclipse Web projects.

**The Rogue Cloud codebase consists of five projects**:

- **RogueCloudServer**: The game server logic, including world simulation, realtime communication to players, and monster AI.

- **RogueCloudClient**: Contains the sample simple AI implementation (SimpleAI) and advanced AI implementation (AdvancedAI), as well as the code that receives messages from the game server and uses that to update the client's game world state (`ClientState` and `ClientWorldState`).

- **RogueCloudShared**: Utility code shared between both the server and client. This includes JSON messages, actions/action responses, shared world state objects (`SelfState/WorldState`), shared interfaces (`IMap/ICreature/IMonster/IEvent/IGroundObject/IObject/ITerrain`), and methods used for AI implementation (`FastPathSearch/AStartSearch/Coord`).

- **RogueCloudClientLiberty**: This code contains OpenLiberty/WebSphere Liberty specific code for listening on WebSockets (for the user's browser to connect) and establishing a connection from your game client to the game server. Most of the client-side logic is in RogueCloudClient. RogueCloudClientLiberty handles the mechanics of sending/receiving WebSocket messages, with the contents of those messages predominantly coming from RogueCloudClient.

- **RogueCloudResources**: Game graphics, tiles, maps, and HTML, are stored in this file. 


## The Rogue Cloud game server architecture

The Rogue Cloud main game server is hosted on IBM Cloud, and is connected to by Rogue Cloud game clients running on a user's local machine. The game server is responsible for handling monster AI, for simulating the game world, for communicating the game world state between itself and clients, and for responding to actions from game player's game clients in real time. 

You may host the server on your local machine when doing local development.

### Endpoints

The server listens on two WebSocket endpoints:
- `(server url)/api/client` - This is what the Rogue Cloud clients connect to, in order to receive world updates and to send client actions. All actions from the client, and all world state updates to the client, will go through the WebSocket created here. Implements the Java WebSocket server API, in `WebSocketClientEndpoint`.
- `(server url)/api/browser` - This is used to allow the administrator to see the full game world from the browser. Implements the Java WebSocket server API, in in `WebSocketBrowserEndpoint`.

The server listens on a few HTTP endpoints, using Servlets and JAX-RS:
- `(server url)/Credentials` - Get/create usernames/passwords, and implemented by `CredentialsServlet`.
- `(server url)/RogueCloudServerHealth` - Used only when hosting in Kubernetes, implemented by `HealthEndpoint`.
- `(server url)/resources/*` - Serves tile graphics and HTML to the browser, implemented by `ResourcesServlet`.
- `(server url)/TileList` -  Provides a list of all the PNG tiles that are available on the server (which is required by the browser UI), implemented by `TileListServlet`.
- `(server url)/services/apiVersion` - The server and client API versions must match; this allows the client code to ensure it is not behind the server version. Implemented by `RsApiVersion`.
- `(server url)/database` - The browser-based leaderboard UI, implemented by `RsDatabase`.

### Important classes

These are some of the classes in the RogueCloudServer project that drive major functionality.

Important classes in the RogueCloudServer project:
- **GameEngine**: All modern games have a central 'game loop', and this is Rogue Cloud's. This class maintains the world state, handles players actions/responses, determines what to send clients re: updated world state, ensures that monsters are created/destroyed as needed, simulates monster AI, and more.
- **WorldGeneration**: The building/river/terrain tiles in game world are generated from a file called `map.txt`, with monsters/weapons/armour added on top of that map. The central generation logic is in this class.
- **WebSocketClientEndpoint**: This class listens on the WebSocket URL that all game clients connect to. Once a WebSocket is established, the WebSocket is packaged into an `ActiveWSClient` and `ActiveWsSession`.
- **ActiveWSClient**: This class tracks a single player's game client; no matter how many times the user connects/reconnects, they will always be tracked by a single instance of this object. It it this class' job to know what the active ActiveWSClientSession object is for a particular player (if they are connected), as well as to maintain various game-state related fields for each client (such as actions they have sent us).
- **ActiveWSClientSession**: This class wraps a Session object (from the `Session` class in the WebSocket API), and adds asynchronous write through the `writeToClientAsync(...)` method, as well as various other fields related to game state.
- **MemoryDatabase**: When a user view the leaderboard, the data from past rounds is retrieved from this class. 



## The Rogue Cloud game client architecture

The Rogue Cloud game client connects to the game server (through a WebSockets), and registers itself as a agent AI. From there, the game server will send the updated world state every tenth-of-a-second. The game client will then send game actions, which tell the game server what the client wishes its character to do (move, attack, drive a potion, pick up an item, etc.)

The client listens on one WebSocket endpoint:
- `(game client url)/api/browser`: The HTML+Javascript-based browser UI will connect to this endpoint (using Javascript WebSockets), and this API will then send world updates to the browser. These updates allow the browser code to display in an on-screen HTML canvas what is happening to the player's character.

The client listens on a couple of HTTP endpoints:
- `(game client url)/StartAgent` - When the user accesses this URL from the browser, the AI game logic is kicked off (which will cause the code to connect to the game server on a separate thread, and start playing), and this URL also serves the HTML+JavaScript browser-based UI to the user.
- `(game client url)/resources/*` - Serves tile graphics and HTML to the browser, implemented by `ResourcesServlet`. Same as resources servlet server code above.


### Important classes in the RogueCloudClient project:
- **ClientState**: This class encompasses all state data that is used to provide support for the API that the player uses to play the game, and also handles the lifecycle of a player's individual AI character.
- **ClientWorldState**: Contains the current state of the world for the current round, based on `JsonFrameUpdate`s received from the server. This includes everything the player can see in their current view (monsters/players/items), as well as any observed events seen during the frame.
- **RemoteClient**: Both simple and advanced AIs interface with the server by subclassing this class. The `SimpleAI` mostly hides the details of this class from the player, while the `AdvancedAI` requires the player to specifically write methods that call `RemoteClient`.


### Naming conventions around synchronization of objects across multiple threads

While looking at the code, you may see variable names that look like this: `myVariable_synch`, `myVariable2_synch_lock`, or `myVariable3_synch_someOtherVariable`. This naming convention is used to ensure correct concurrent thread access to these variables.

Many of the classes in the project contain methods and data that may be concurrently accessed by multiple threads simultaneously. Since sharing data between threads is very difficult to get correct (and even more difficult to verify as correct), I have used the above naming convention throughout the code to describe the required synchronization characteristics of an object.

This naming convention is used to indicate how those variables should be accessed (and if/where synchronization is required). If  you see a method call containing _synch_ that is NOT accessed inside a `synchronized(...) {}` statement, this is a bug.

You may safely assume that all classes in the project are thread-safe, unless the documentation at the top of the class file indicates otherwise ("thread unsafe"). However, some classes are designed to be only run from a single thread in order to reduce the implementation complexity (and those class' methods must specifically be called on the game loop's thead). 

If a method contains the following line at the top of the method `RCRuntime.assertGameThread()`, this indicates that the method MUST be run from the game loop thread. If a method contains `RCRuntime.assertNotGameThread()`, then the opposite is true: calling that method from the game loop thread will cause thread problems. (Note that these assertions are actually disabled by default to prevent a performance penalty, but should be enabled during development)

### How the naming convention translates to code usage

So while looking at the code, you will see the following naming convention style used for some variables:
- A) myVariable_synch
- B) myVariable2_synch_lock
- C) myVariable3_synch_myVariable4
- D) myVariable

**A**:  

When a variable ends in `_synch`, synchronize on the variable itself when accessing it:
```
synchronized(myVariable_synch) {   
	myVariable_synch.someMethod();
}
```
This is the simplest case, where the data of a specific object must be updated atomically, but the data itself is not related to any other data.

**B**:

When a variable ends in `_synch_(some other variable name)`, in this case a variable named *lock*, then synchronize on the variable named on the right-hand-side:
```
synchronized(lock) {
	myVariable2_synch_lock.someMethod();
}
```

This is the advanced case, where the data of a specific object must be kept in sync with another parent object (or group of objects). The object named on the right is usually a lock object, which handles the synchronization for all related objects that must be kept in sync.

**C**: 

This is the general case of B. A variable may end in `_synch_lock`, in which case it refers to a variable named *lock*, but the variable named on the right-hand-side may be any field in the class, and in the case of C it is *myVariable4*. 

**D**: 

If an object does NOT contain the `_synch` or `_synch_` substring, it is safe to access from any thread without synchronization. This is usually the case for constants, for immutable objects, or for objects whose synchronization is managed by the class (using the RCRuntime.assert* methods).
```
	myVariable.someMethod();
```
