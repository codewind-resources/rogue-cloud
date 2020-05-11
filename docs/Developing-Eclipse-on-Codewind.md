# Playing Rogue Cloud with Eclipse Codewind

### A) Install Eclipse.
1) Visit the [Eclipse download page](https://www.eclipse.org/downloads/packages/). Codewind requires Eclipse 2019-03 or later.
2) Locate the **Eclipse IDE for Enterprise Java Developers** section, select your operating system, and click **Download**.
3) Wait for the file to download. Then, extract it to the directory of your choice.
4) Start Eclipse and specify a workspace directory. You can use the default. Wait for Eclipse to load.

### B) Install the Codewind prerequisites: Docker, Docker Compose, and Git.
- **On Mac:** Install **Docker Desktop for Mac** and Git. On this platform, Docker Compose is bundled with Docker Desktop.
- **On Windows:** Install **Docker Desktop for Windows** and Git. On this platform, Docker Compose is bundled with Docker Desktop.
- **On Linux:** Install Docker, Docker Compose, and Git. On this platform, you need to download Docker Compose separately.

### C) Install Codewind plug-ins into Eclipse.
1) From within Eclipse, select **Help** from the menu and then **Eclipse Marketplace**.
2) Type `Codewind` in the search bar. Then, click **Go**. **Codewind** appears in the search listings.
3) Click **Install**.
4) Read and accept the licenses. Then, click **Finish**.
5) After the installation completes, you are prompted to restart Eclipse. Click **Restart**.

For more information about installing Codewind into Eclipse, see [Getting started with Codewind for Eclipse](https://www.eclipse.org/codewind/mdteclipsegettingstarted.html).

### D) Install Codewind container images.
1) Open the Codewind view. Navigate to **Window** in the menu followed by **Show View**>**Other...** and **Codewind**>**Codewind Explorer**.
2) Codewind requires the installation of more Docker images to run. Double-click the Codewind item in the Codewind Explorer view to complete the installation. The installation might take a few minutes to complete.

You are now ready to use the tools. You can use the Codewind Explorer view to create new projects or add existing ones. Right-click an element in the Codewind Explorer to look at the features available.

### E) Git clone the Rogue Cloud client into a Codewind directory.
1) Choose a folder in which you want to clone the Rogue Cloud client repository. Do not create projects in the `~/codewind-data/` or `C:\codewind-data` directories.
  ```
  cd <folder where you want to clone the Rogue Cloud client>
  git clone https://github.com/codewind-resources/rogue-cloud-client-codewind
  ```
2) Import the project into Eclipse. Select **File** from the menu and **Import...**. Then, in the dialog, select **General** and **Existing Projects into Workspace**. Click **Next >**.
3) Select **Select root directory** and click **Browse...**. Select **(codewind workspace path from above)/rogue-cloud-client-codewind**. Then, click **Select Folder** and **Finish**. Wait for the project to build.
4) Right-click **Codewind** in the **Codewind Explorer** view. Then, click **Local Projects**>**Add Existing Project...**. The **gameclient** appears in the checkbox list.
5) Select **gameclient** if it is not already selected. Then, click **Next >**.
6) Select **MicroProfile / Java EE** if it is not already selected. Then, click **Finish**. 
7) Before the code starts to build, the container needs to initialize and download the Java and Maven dependencies for the underlying build system. This process can take between five to ten minutes depending on the CPU and network connection. This initialization is required only before the first time you use MicroProfile with the Codewind tools. 

### F) Register a user and then make changes to the SimpleAI class.
1) In the code editor, press `CTRL-SHIFT-R` on Windows and `Command-Shift-R` on Mac. Then, type `StartAgentServlet.java` and select `StartAgentServlet.java`.
   * **Note:** Use `CTRL-SHIFT-R` or `Command-Shift-R` to quickly find Java classes in Eclipse.
2) Edit the following fields in `StartAgentServlet.java` to create a new user and password:
```
public static final String USERNAME = "(specify a username here!)";
public static final String PASSWORD = "(specify a password here!)";
```
   * These values ensure that only you can access and control your character.
   * The user name and password you specify are automatically registered when your code first begins controlling a character on the game map, and they do not have to correspond to an existing email address or account.
3) Press `CTRL-S` on Windows or `Command-S` on Mac to save your changes.
4) Press `CTRL-SHIFT-R` on Windows or `Command-Shift-R` on Mac. Type `SimpleAI.java` and select `SimpleAI.java`.
5) This class is the main AI class. Changes made to this class are reflected in your AI running in the MicroProfile Liberty container.

### G) Watch your agent go and start coding
To watch your agent as it interacts with the game world, right-click the `gameclient` project in the **Codewind Explorer** view and select **Open Application** to open a browser to the root of your application.

**Note**: If you are on Windows, copy and paste the URL into an external browser, such as Chrome, Firefox, or Edge, because the internal browser in Eclipse uses IE11, which is an unsupported browser.

Congratulations! Your character is now exploring and interacting with the game world and earning you points on the leaderboard.

Next, [learn more about coding an agent for Rogue Cloud.](Developing-CodingNextSteps.md)
