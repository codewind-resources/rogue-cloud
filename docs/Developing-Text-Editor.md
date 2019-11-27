## Playing Rogue Cloud by using a Text Editor and Maven

This section assumes that you are familiar with an existing developer editor, such as Atom, Eclipse, VS Code, or Sublime Text.

### A) Clone the Rogue Cloud repository and run a Maven build.
* Prerequisite: Ensure that Git is installed. Git is available for download from https://git-scm.com/.
* Prerequisite: Ensure that the Java 8 JDK is installed. For more information, see [Installing the Java 8 JDK](Installing-Java.md).
1) Create a directory to contain your Rogue Cloud Git repository. Make a note of this directory because you will use it later. Enter the following commands into your command line:
```
mkdir (new repo directory)
cd (your new repo directory)
git clone https://github.com/microclimate-dev2ops/rogue-cloud.git
cd rogue-cloud
```
2) Verify the installation by building with Maven:
* **Linux/Mac**: ``./mvnw clean package ``
* **Windows**: ``mvnw clean package``

### B) Open StartAgentServlet.java and register a new username and password.
1) In your editor of choice, open the following file from the root of the cloned Git repository: ``RogueCloudClientyLiberty/src/com/roguecloud/client/container/StartAgentServlet.java``
2) Edit the following fields to create a new user name and password:
	```
	public static final String USERNAME = "(specify a username here!)";
	public static final String PASSWORD = "(specify a password here!)";
	```
The user name and password that you specify are automatically registered the first time they are used.

### C) Open SimpleAI.java, which is where you write AI code to control your game character.
Open the following file from the Git repository: ``RogueCloudClient/src/com/roguecloud/client/ai/SimpleAI.java``.

This class is the main class that controls your game character. This class contains a simple sample implementation, which is already ready to run! You can proceed to the next steps to build the code and enter your character into a new game round.

### D) Building and running your Rogue Cloud game client code

To build your game client code, run the following commands from your command line:
```
cd <path to Git repo>
./mvnw package
```

To run your game client code, run the following commands from your command line:
```
cd <path to Git repo>
cd RogueCloudClientLiberty
../mvnw liberty:run-server
```
These commands start an instance of WebSphere Liberty, and your game client code automatically connects to the game server and begins controlling your character.

To stop the game client, press CTRL-C. On Windows, you might need to press Y or CTRL-C to terminate the batch file.

### E) Watch your agent go and start coding

To watch your agent as it interacts with the game world, look for the following message in the Maven ``liberty:run-server`` command output:
```
***********************************************************************************************
*                                                                                             *
*    Agent has started. Watch at: http://localhost:19080/RogueCloudClientLiberty/StartAgent   *
*                                                                                             *
***********************************************************************************************
```
Visit this URL in your browser to view your character. The traditional browser URL is http://localhost:19080/RogueCloudClientLiberty/StartAgent. The port can be adjusted with `RogueCloudClientLiberty` in the `pom.xml` file.

Next, [learn more about coding an agent for Rogue Cloud](Developing-CodingNextSteps.md).
