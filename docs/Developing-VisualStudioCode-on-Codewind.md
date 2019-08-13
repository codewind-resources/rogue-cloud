## Play Rogue Cloud using Microclimate Visual Studio Code Tools

### A) Install Visual Studio Code
- If you already have Visual Studio Code installed, skip to the next section.

1) Visit https://code.visualstudio.com/Download and select your operating system.
2) After the download completes, run the installer (Windows) or install the package (Linux/MacOS).
3) After the install is completed, run Visual Studio code.

### B) Install the Codewind prerequisities: Docker, Docker Compose, and Git

Installation prerequisites:
- *On Mac*: Install 'Docker Desktop for Mac' and Git (on this platform Docker Compose is bundled with Docker Desktop)
- *On Windows*: Install 'Docker Desktop for Windows' and Git (on this platform Docker Compose is bundled with Docker Desktop)
- *On Linux*: Install Docker, Docker Compose, and Git (on this platform, unlike Mac/Windows, Docker Compose *must* be downloaded separately)

Additional configuration steps for these platforms may be required. See the [Codewind local install documentation](https://www.eclipse.org/codewind/installlocally.html) for full details.

### C) Installing Codewind Tools into Visual Studio Code

1) In Visual Studio Code, select `View` (menu bar) > `Extensions`. Under `Search Extensions in Marketplace`, enter `Microclimate`.
2) Select `Codewind`, and click the `Install` button on the right-hand screen.

3) If you are asked to restart Visual Studio Code, do so. Otherwise proceed to the next step.

4) You will now be presented with a dialog: `"Codewind requires the installation of Docker container to run, which might take a few minutes to download. Do you want to complete the installation now?"`. Click `Install`.

5)  You should see a status message `Pulling Codewind Docker images` on the bottom right hand corner of the screen. Wait for this to complete. After the images are downloaded, you should see `Starting Codewind`, then `Codewind installation is complete`. Click OK.

6) Select `View` (menu bar) > `Explorer`. On the bottom left hand corner of the explorer, you should see 'Codewind'. Expand this panel if not already expanded.

More information on [installing Codewind into Visual Studio Code](https://www.eclipse.org/codewind/mdt-vsc-getting-started.html) is available from our website.

### D) Git clone the Rogue Cloud client into Codewind workspace directory.

1) Determine the location of the Codewind workspace directory:
- *Mac/Linux*: `docker inspect codewind-pfe | grep "HOST_WORKSPACE_DIRECTORY="`
  - Example: `"HOST_WORKSPACE_DIRECTORY=/home/user/codewind/codewind-workspace"` means your workspace can be found in `/home/user/codewind/codewind-workspace`
- *Windows*: `docker inspect codewind-pfe | find "HOST_WORKSPACE_DIRECTORY="`
  - Example: `"HOST_WORKSPACE_DIRECTORY=C:\\codewind-workspace"` means the Codewind workspace is `c:\codewind-workspace`
2) From within the `codewind-workspace` directory, clone the Rogue Cloud client repo.
  ```
  cd (path to your codewind workspace from the previous step)
  git clone https://github.com/microclimate-dev2ops/rogue-cloud-client
  ```
3) Back in Visual Studio Code, under the `Codewind` view, right-click on `Projects (Local)` and select `Add Existing Project`. Specify the path of `rogue-cloud-client` that you cloned from the previous step, then click `Add to Codewind`
4) You will see a brief `Processing...` status message, followed by a `Please confirm the project type` message.
- The Type field should be: `liberty`
- The Language field should be: `Java`
- If one or both of these are inaccurate, jump back to step 3 and ensure the correct path is selected.
5) Presuming your the project is correctly identified, click `Yes`. 
6) Before the code starts building, the container needs to initialize and download the Java and Maven dependencies for the underlying build system. This can take up to 7 to 10 minutes depending on CPU and network connection (this initialization is only required the first time you use the Codewind tools).

Additional information about [creating and importing projects into Codewind](https://www.eclipse.org/codewind/mdt-vsc-getting-started.html) is available our website.


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
