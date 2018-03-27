
## Developing Rogue Cloud using a Text Editor (Visual Studio Code, Atom, etc) and Maven

This section assumes that you are familiar with an existing developer text editor such as Visual Studio Code, Atom, or Sublime Text. For Visual Studio code, we recommend the 'Java Extension Pack', which includes tighter integration with the Java platform.

### A) Clone the Rogue Cloud repository and run a Maven build
* Pre-requisite: Ensure that git is installed. Git is available for download from https://git-scm.com/.
* Pre-requisite: Ensure that the Java 8 JDK is installed. [Learn more on installing Java](Installing-Java.md).
1) Create a directory to contain your Rogue Cloud git repository. Make a note of this directory as it will be used later on. On the Terminal (Linux/Mac) or Command Prompt (Windows):
```
mkdir (new repo directory)
cd (your new repo directory)
git clone https://github.com/microclimate-dev2ops/rogue-cloud.git
cd rogue-cloud
```
2) Verify the install by building using Maven
* **Linux/Mac**: ``./mvnw clean package ``
* **Windows**: ``mvnw clean package``

### B) Open StartAgentServlet.java and register a new username and password

1) In your editor of choice, open the following file from the root of the cloned git repository: ``RogueCloudClientyLiberty/src/com/roguecloud/client/container/StartAgentServlet.java``
2) Edit the following fields to create a new USERNAME and PASSWORD:
	```
	public static final String USERNAME = "(specify a username here!)";
	public static final String PASSWORD = "(specify a password here!)";
	```
The username and password you specify here are automatically registered the first time they are used.

### C) Open SimpleAI.java -- this is where you will be writing AI code to control your game character.

1) Open the following file from the Git repository: ``RogueCloudClient/src/com/roguecloud/client/ai/SimpleAI.java``

This class is the main class which controls your game character. This class contains a simple sample implementation, which is already ready to run! You can proceed to the next steps to build the code and enter your character into a new game round.

### D) Building and running your Rogue Cloud game client code

To build your game client code, run the following from Terminal (Mac/Linux) or Command Prompt (Windows):
```
cd (path to git repo)
./mvnw package
```

To run your game client code, run the following from Terminal or Command Prompt:
```
cd (path to git repo)
cd RogueCloudClientLiberty
../mvnw liberty:run-server
```
This starts an instance of WebSphere Liberty, and your game client code automatically connects to the game server and begins controlling your character.

To stop the game client, hit CTRL-C (on Windows, you may need to hit Y or CTRL-C, to terminate the batch file).

### E) Next steps: watch your agent go, and start coding

To watch your agent as it interacts with the game world, look for following message in the Maven ``liberty:run-server`` command output:

```
***********************************************************************************************
*                                                                                             *
*    Agent has started. Watch at: http://localhost:19080/RogueCloudClientLiberty/StartAgent   *
*                                                                                             *
***********************************************************************************************
```
Visit this URL in your browser to view your character. The traditional browser URL is: http://localhost:19080/RogueCloudClientLiberty/StartAgent . The port can be adjusted in RogueCloudClientLiberty pom.xml.

Next, [visit the next steps page to learn more about coding an agent for Rogue Cloud.](Developing-CodingNextSteps.md)

