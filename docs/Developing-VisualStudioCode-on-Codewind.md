# Playing Rogue Cloud with Codewind Visual Studio Code (VS Code) Tools

### A) Install VS Code.
1) Visit [Download Visual Studio Code](https://code.visualstudio.com/Download) and select your operating system.
2) After the download completes, run the installer on Windows or install the package on Linux or MacOS.
3) After the installation completes, run VS Code.
**Note:** For VS Code, use the **Java Extension Pack** for tighter integration with the Java platform.

### B) Install the Codewind prerequisities: Docker, Docker Compose, and Git.
- **On Mac:** Install **Docker Desktop for Mac** and Git. On this platform, Docker Compose is bundled with Docker Desktop.
- **On Windows:** Install *Docker Desktop for Windows* and Git. On this platform, Docker Compose is bundled with Docker Desktop.
- **On Linux:** Install Docker, Docker Compose, and Git. On this platform, unlike on Mac and Windows, Docker Compose must be downloaded separately.

### C) Install Codewind Tools on VS Code.
1) In VS Code, select **View** from the menu. Then, select **Extensions**. From **Search Extensions in Marketplace**, enter `Codewind`.
2) Select **Codewind** and click the **Install** button.
3) Restart VS Code if you are prompted.
4) A window appears and asks, `"Codewind requires the installation of Docker containers to run, which might take a few minutes to download. Do you want to complete the installation now?"`. Click **Install**.
5) A status message appears that says, `Pulling Codewind Docker images`. Wait for this process to complete. After the images are downloaded, a `Starting Codewind` message appears followed by `Codewind installation is complete`. Click **OK**.
6) Select **View** from the menu. Then, select **Explorer**. From **Explorer**, a **Codewind** view is available. Expand this panel if it is not already expanded.

For more information, see [Getting started with Codewind for VS Code](https://www.eclipse.org/codewind/mdt-vsc-getting-started.html).

### D) Git clone the Rogue Cloud client into a Codewind directory.
1) Choose a folder in which you want to clone the Rogue Cloud client repository. Do not create projects in the `~/codewind-data/` or `C:\codewind-data` directories.
  ```
  cd <folder where you want to clone the Rogue Cloud client>
  git clone https://github.com/microclimate-dev2ops/rogue-cloud-client-codewind
  ```
2) Back in VS Code, from the **Codewind** view, right-click **Projects (Local)** and select **Add Existing Project**. Specify the path of the `rogue-cloud-client-codewind` folder that you cloned and click **Add to Codewind**.
3) A `Processing...` status message appears, followed by a `Please confirm the project type` message. Check to see that the following information is correct:
   - The **Type** field is `liberty`.
   - The **Language** field is `Java`.
4) If one or both of these fields are inaccurate, ensure that the correct path is selected.
5) If your project is correctly identified, click **Yes**.
6) Before the code starts building, wait for the container to initialize and download the Java and Maven dependencies for the underlying build system. This process can take between five to ten minutes depending on the CPU and network connection. This initialization is required only before the first time you use Codewind VS Code Tools.

### E) Register a user and then make changes to the SimpleAI class.
1) In the code editor, press `CTRL-P` on Windows and `Command-P` on Mac. Then, type `StartAgentServlet.java` and select `StartAgentServlet.java`.
   * **Note:** Use `CTRL-P` or `Command-P` to quickly find Java classes in VS Code.
2) Edit the following fields in `StartAgentServlet.java` to create a new user and password:
```
public static final String USERNAME = "(specify a username here!)";
public static final String PASSWORD = "(specify a password here!)";
```
   * These values ensure that only you can access and control your character.
   * The user name and password you specify are automatically registered when your code first begins controlling a character on the game map, and they do not have to correspond to an existing email address or account.
3) Press ``CTRL-S`` on Windows or ``Command-S`` on Mac to save your changes.
4) Press ``CTRL-P`` on Windows or ``Command-P`` on Mac. Type ``SimpleAI.java`` and select ``SimpleAI.java``.
5) This class is the main AI class. Changes made to this class are reflected in your AI running on the Liberty server.

### F) Watch your agent go and start coding.
To watch your agent as it interacts with the game world, right-click the `gameclient` project in the **Codewind** view and select **Open in Browser** to open a browser to the root of your application.

Congratulations! Your character is now exploring and interacting with the game world and earning you points on the leaderboard.

Next, [learn more about coding an agent for Rogue Cloud](Developing-CodingNextSteps.md).
