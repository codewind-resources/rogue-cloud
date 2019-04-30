## Play Rogue Cloud using Microclimate Visual Studio Code Tools

### A) Install Visual Studio Code
- If you already have Visual Studio Code installed, skip to the next section.

1) Visit https://code.visualstudio.com/Download and select your operating system.
2) After the download completes, run the installer (Windows) or install the package (Linux/MacOS).
3) After the install is completed, run Visual Studio code.

### B) Install and start Microclimate, if not already done.

Next, visit the [Installing Microclimate](Installing-Microclimate.md) page to install and start Microclimate. Return to this page when done.

### C)  Installing Microclimate Developer Tools into Visual Studio Code

1) In Visual Studio Code, select `View` (menu bar item) > `Extensions`. Under `Search Extensions in Marketplace`, enter `Microclimate`.
2) Select `Microclimate Developer Tools`, and click the `Install` button on the right-hand screen.
3) Reload Visual Studio Code.


### D) Clone the Rogue Cloud Client Git Repo from the Microclimate browser UI

1) In the [Microclimate browser UI](http://localhost:9090), accept the Microclimate license and select an option on the telemetry page. You should now see the Microclimate introductory splash screen.
2) Select the ``Import Project`` button. On the following page, select ``Git``, then copy and paste the following repository location:
* `https://github.com/microclimate-dev2ops/rogue-cloud-client`
3) Click ``Next``, then click the ``Import`` button.
4) Once the code is imported, click the ``Edit Code`` button. You are now redirected to the code editor.
5) Before the code starts building, the container needs to initialize and download the Java and Maven dependencies for the underlying build system. This can take up to 7 to 10 minutes depending on CPU and network connection (this initialization is only required the first time you using Microclimate). You can use ``docker logs -f microclimate-file-watcher`` to watch its progress.
6) Once the build has initialized and downloaded the required dependencies, the build icon displays a green circle notification, like so: ![Rogue Cloud project is built](resources/gameclient-microclimate-ready.png "Rogue Cloud project is built")

Once the build completes, you can return to the Visual Studio Code window.

### E) Create a dev connection to Microclimate from Visual Studio Code

You should now have both Visual Studio Code and Microclimate installed. Next we need to configure the Visual Studio Code Tools to connect to the Microclimate service.

Ensure that Microclimate is up and running before proceeding with these steps.

1) In Visual Studio Code, select `View` (menu bar item) > `Explorer`.
2) On the left-hand panel, expand the `MICROCLIMATE` view.
3) In this view, click on `No Microclimate connections`.
4) This will bring up a text bar at the top of the screen asking you to `Enter the Port for the local Microclimate instance you want to connect to`:
5) Ensure that `9090` is the default value in this field, and press Enter.
6) You should see a notification indicating that the connection to Microclimate has been successfully created. You should also see the `roguecloudclient` application running in this view. This is the project we imported from Git in a previous step.
7) Right-click on `roguecloudclient` and select `Open folder as workspace`.

See the Microclimate documentation for more information [on connecting to Microclimate instance from Eclipse](https://microclimate-dev2ops.github.io/mdteclipsemanagingconnections#doc).

### F) Register a user and then make changes to the SimpleAI class

1) In the code editor, press ``CTRL-P`` (``Command-P`` on Mac) and type ``StartAgentServlet.java``, and select ``StartAgentServlet.java``.
* ``CTRL-P/Command-P`` is a great way to quickly find Java classes in the Visual Studio Code editor.

2) Edit the following fields in `StartAgentServlet.java` to create a new user and password.
```
public static final String USERNAME = "(specify a username here!)";
public static final String PASSWORD = "(specify a password here!)";
```
* These values are to ensure that *only you* can access and control your character.
* The username and password you specify are automatically registered when your code first begins controlling a character on the game map, and they do not have to correspond to an existing email address or account.

3) Press ``CTRL-S`` (``Command-S`` on Mac) in order to save your changes.

4) Press ``CTRL-P`` (``Command-P`` on Mac) and type ``SimpleAI.java`` and select ``SimpleAI.java``.

5) This class is the main AI class. Changes made to this class are reflected in your AI running on the Liberty Server.


### G) Next steps: watch your agent go, and start coding

To watch your agent as it interacts with the game world, right-click on the `roguecloudclient` project in the `MICROCLIMATE` view and select `Open in Browser`.

This will open a browser to the root of your application.

Add ``gameclient/StartAgent`` to the end of the URL, such that it looks like:
* ``http://localhost:(port)/gameclient/StartAgent``, where (port) is the randomly generated local port for the server.

Congratulations, your character is now exploring and interacting with the game world, and earning you points on the leaderboard!

Next, [visit the next steps page to learn more about coding an agent for Rogue Cloud.](Developing-CodingNextSteps.md)
