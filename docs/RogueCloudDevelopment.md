
## Developing Rogue Cloud: How Rogue Cloud is implemented, and adding new features to the game

This page is for developers who are interested in how Rogue Cloud is implemented, and/or want to add new features to the game (or, perhaps, even fix bugs!).

For developers that only wish to play the game (which involves writing your own Rogue Cloud agent code), see [GettingStarted.md](GettingStarted.md).


## First steps

The first thing you should do is setup a development environment for the Rogue Cloud server and client. Since Rogue Cloud is Maven-based, you may use any Maven-supporting IDE, however, it is easiest to use the Eclipse-based *Liberty Developer Tools* as I have provided instructions for those below.

Here are instructions on how to [setup your Eclipse-based development environment](Create-a-Game-Server-DevEnv.md).

## Next steps

Once you've got that up and running, you can take a look at what role of [each the projects have, important classes in those projects, and websocket/http endpoints used in the game server and game client](RCDevArchitecture.md).

You can also look at the JSON WebSocket API docs, which describe the [JSON-based communication protocol between the client and the server](RogueCloudJsonApi.md).
