# Playing Rogue Cloud with Eclipse Codewind

### A) Install Eclipse.
- If Eclipse is already installed, skip to the next section. Codewind requires Eclipse 2019-03 or later.
1) Visit the [Eclipse download page](https://www.eclipse.org/downloads/packages/).
2) Locate the `Eclipse IDE for Enterprise Java Developers` section, select your operating system, and click **Download**.
3) Wait for the file to download. Then, extract it to the directory of your choice.
4) Start Eclipse and specify a workspace directory. The default is fine to use. Wait for Eclipse to load.

### B) Install the Codewind prerequisites: Docker, Docker Compose, and Git.
Installation prerequisites:
- *On Mac*: Install 'Docker Desktop for Mac' and Git (on this platform Docker Compose is bundled with Docker Desktop)
- *On Windows*: Install 'Docker Desktop for Windows' and Git (on this platform Docker Compose is bundled with Docker Desktop)
- *On Linux*: Install Docker, Docker Compose, and Git (on this platform, Docker Compose must be downloaded separately)

Additional configuration steps for these platforms may be required. See the [Eclipse Codewind documentation for details](https://www.eclipse.org/codewind/installlocally.html).


### C) Installing Codewind plugins into Eclipse
- Requires: Eclipse 2019-03, or newer.

1) From within Eclipse, select `Help` (menu item) > `Eclipse Marketplace`.
2) Type `Codewind` in the search bar, then click `Go`.
3) You should now see `Codewind` in the search listings. Click the `Install` button next to these tools.
4) Read and accept the licenses, then click `Finish`.
5) After the install completes, you will be prompted to restart Eclipse, click Restart.

See the Eclipse Codewind documentation for more information [on Installing Codewind into Eclipse](https://www.eclipse.org/codewind/mdteclipsegettingstarted.html).

### D) Installing Codewind container images

1) Open the Codewind view. Navigate to `Window` (menu item) > `Show View` > `Other...`, then  `Codewind` > `Codewind Explorer`
2) Codewind requires the installation of additional Docker images to run. Double-click on the Codewind item in the Codewind Explorer view to complete the installation. The installation may take a few minutes to complete.

You are now ready to use the tools. You can use the Codewind Explorer view to create new projects or add existing ones. Right-click an element in the Codewind Explorer to look at the features available.

### E) Git clone the Rogue Cloud client into Codewind workspace directory.

Codewind creates a folder called `codewind-workspace` within your home directory to contain your projects. In this step we will locate that folder, and then `git clone` the Rogue Cloud client into that folder.

1) From the terminal, determine the location of the Codewind workspace folder:
- *Mac/Linux*: `docker inspect codewind-pfe | grep "HOST_WORKSPACE_DIRECTORY="`
  - Example: `"HOST_WORKSPACE_DIRECTORY=/home/user/codewind/codewind-workspace"` means your workspace can be found in `/home/user/codewind/codewind-workspace`
- *Windows*: `docker inspect codewind-pfe | find "HOST_WORKSPACE_DIRECTORY="`
  - Example: `"HOST_WORKSPACE_DIRECTORY=C:\\codewind-workspace"` means the Codewind workspace is `c:\codewind-workspace`
2) From within the `codewind-workspace` directory, clone the Rogue Cloud client repo
  ```
  cd (path to your codewind workspace from the previous step)
  git clone https://github.com/microclimate-dev2ops/rogue-cloud-client-codewind
  ```
3) Now, import the project into Eclipse: Select `File` (menubar item) > `Import...`, then in the dialog select `General` (tree item) > `Existing Projects into Workspace` and click `Next >`.
4) Select `Select root directory` and click `Browse...`, select `(codewind workspace path from above)/rogue-cloud-client-codewind`, then click `Select Folder`. Click `Finish`. Wait for the project to build.
5) Right-click on `Codewind` (in `Codewind Explorer` view) > `Local Projects` > `Add Existing Project...`.
6) `gameclient` should appear in the checkbox list, select it (if not already selected), then click `Next >`.
7) Select `MicroProfile / Java EE` (if not already selected), then click `Finish`. 
8) Before the code starts building, the container needs to initialize and download the Java and Maven dependencies for the underlying build system. This can take between 5 to 10 minutes depending on CPU and network connection (this initialization is only required the first time you use MicroProfile with the Codewind tools). 

Additional information about creating and [importing projects into Codewind is available our website.](https://www.eclipse.org/codewind/mdteclipsegettingstarted.html)


### F) Register a user and then make changes to the SimpleAI class

1) In the code editor, press ``CTRL-SHIFT-R`` (``Command-Shift-R`` on Mac) and type ``StartAgentServlet.java``, and select ``StartAgentServlet.java``.
* ``CTRL-SHIFT-R/Command-Shift-R`` is a great way to quickly find Java classes in Eclipse.

2) Edit the following fields in `StartAgentServlet.java` to create a new user and password.
```
public static final String USERNAME = "(specify a username here!)";
public static final String PASSWORD = "(specify a password here!)";
```
* These values are to ensure that *only you* can access and control your character.
* The username and password you specify are automatically registered when your code first begins controlling a character on the game map, and they do not have to correspond to an existing email address or account.

3) Press ``CTRL-S`` (``Command-S`` on Mac) in order to save your changes.

4) Press ``CTRL-SHIFT-R`` (``Command-Shift-R`` on Mac) and type ``SimpleAI.java`` and select ``SimpleAI.java``.

5) This class is the main AI class. Changes made to this class are reflected in your AI running in the MicroProfile Liberty container.


### G) Next steps: watch your agent go, and start coding

To watch your agent as it interacts with the game world, right-click on the `gameclient` project in the `Codewind Explorer` view and select `Open Application`.

This will open a browser to the root of your application. **Note**: If you are on Windows, you will need to copy-paste the URL into an external browser (Chrome, Firefox, Edge) because Eclipse's internal browser uses IE11 (an unsupported browser).

Congratulations, your character is now exploring and interacting with the game world, and earning you points on the leaderboard!

Next, [visit the next steps page to learn more about coding an agent for Rogue Cloud.](Developing-CodingNextSteps.md)
