## Troubleshooting Rogue Cloud

Feel free to open new issues if you are seeing errors or unexpected behaviour that is not covered by these docs.


### *java.net.ConnectException*/*java.net.UnknownHostException* exceptions when connecting to the game's web UI at *(url)/gameclient/StartAgent* (when using Microclimate + Docker)

When you access the game client's web UI, the Rogue Cloud client (running inside a container) will automatically attempt to connect to the main game server at http://roguecloud.space:29080 in order to join the game. If the game client is unable to connect, you will see connection exceptions.

If you are receiving a connection/unknown host exception when attempting to access the game client UI, this is likely an issue with the networking configuration inside your containers. In general, the Docker tool (especially on Windows/Mac) must perform some fairly complex network routing from inside containers to the host's networking stack, and this can occassionally fail. 

To solve this problem, just right-click on the Docker icon and select Restart Docker (or, failing that, restart your computer).

Example messages:
* javax.ws.rs.ProcessingException: java.net.ConnectException: ConnectException invoking http://roguecloud.space: Connection timed out: connect
* Caused by: java.net.ConnectException: ConnectException invoking http://roguecloud.space: Connection timed out: connect
* Caused by: java.net.ConnectException: Connection timed out: connect
* javax.ws.rs.ProcessingException: java.net.UnknownHostException: UnknownHostException invoking 


