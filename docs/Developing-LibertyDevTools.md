## Playing Rogue Cloud using Eclipse and IBM Liberty Developer Tools

Pre-requisite: This requires Java 8 JDK be installed. The Java JDK is available from http://www.oracle.com/technetwork/java/javase/downloads/index.html.



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

### B) Install Liberty Developer Tools in Eclipse

1) Download the 'Eclipse IDE for Java EE Developers' from here: https://www.eclipse.org/downloads/eclipse-packages/
2) Unzip the application and run Eclipse.
3) In Eclipse, select Help (menu item) > Eclipse Marketplace
4) Enter ***Liberty Developer Tools*** into the 'Find:' search box, and click Go.
5) Find the ***IBM Liberty Developer Tools for Oxygen*** entry and click Install.
6) Click Confirm.
7) Accept the licenses, then click Finish.
8) IBM Liberty Developer Tools will now install. When prompted to restart, click Restart Now.

### C) Create a new Liberty Server in Eclipse

1) Select the Window (menu item) > Perspective > Open Perspective > Other. Select 'Java EE' and click OK.
2) Select File menu item > New > Other..., then Server > Server. Click Next.
3) Select IBM > Liberty Server. Ensure that 'Server's host name' is *localhost* and NOT *Cloud*. Click Next.
4) Select 'Install from an archive or a repository'. Click Next.
5) Specify a path to download the Liberty server to, then select 'Download and install a new runtime environment from ibm.com'.
6) Select 'WAS Liberty with Java EE 7 Web Profile'. Click Next.
7) On the 'Install Additional Content' dialog, click Next.
8) Accept the license, and then click Next.
9) Click Next, then Finish.
10) Wait for the WAS Liberty server to download (about 45 seconds.)
11) You should now see your server in the Servers view.

### D) Import your Rogue Cloud git repository into Eclipse.
1) Select File (menu item) > Import..., then select Git > Projects from Git. Click Next.
2) Select 'Existing local repository', and then click Next.
3) Click 'Add...' and a new dialog will open. In the 'Directory:' field, specify the directory containing your Rogue Cloud git repo. Click Search if needed.
4) Select the checkbox that corresponds to your git repo, and then click Finish.
5) Click Next.
6) On the 'Import Projects' page, ensure that all the RogueCloud* projects are checked, and then click Finish.
7) A 'Workspace Migration' dialog may appear after you click Finish. Click Next, then Next, and then Finish.
8) You should now have workspace containing all the RogueCloud projects from the Git repository. None of the projects should have build errors.

### E) Publish the 'RogueCloudClientLiberty' project to the Liberty server

1) Select Window (menu item) > Show View > Other. Select Server > Servers, and then click Open.
2) In the Servers view, right-click on 'Liberty Server at localhost' and then select 'Add and Remove...'.
3) Click 'RogueCloudClientLiberty', then 'Add >'. Click Finish.
4) In the Servers view, right-click on 'Liberty Server at localhost' and then select 'Start'.


### F) Register a user and then make changes to the SimpleAI class

1) In the 'Enterprise Explorer' view (you must be in the 'Java EE' perspective, see section B for details), select ``RogueCloudClientyLiberty > Java Resources > src > com.roguecloud.client.container > StartAgentServlet.java``.
2) Double-click on ``StartAgentServlet.java`` to open StartAgentServlet Java class.

3) Edit the following fields to create a new user and password.
```
public static final String USERNAME = "(specify a username here!)";
public static final String PASSWORD = "(specify a password here!)";
```
* Specifying a username and password here will automatically register it once your code first begins controlling a character on the game map.

4) In the 'Enterprise Explorer' view, select ```RogueCloudClient > src > com.roguecloud.client.ai > SimpleAI.java```.

5) This class is the main AI class. Changes made to this class will be reflected in your AI running on the Liberty Server.

### G) Next steps: watch your agent go, and start coding

To watch your agent as it interacts with the game world, look for following message in the Console view:

```
***********************************************************************************************
*                                                                                             *
*    Agent has started. Watch at: http://localhost:9080/RogueCloudClientLiberty/StartAgent    *
*                                                                                             *
***********************************************************************************************
```
Visit this URL in your browser to view your character. The traditional browser URL is: http://localhost:9080/RogueCloudClientLiberty/StartAgent . The port can be adjusted in RogueCloudClientLiberty pom.xml.

Next, [visit the next steps page to learn more about coding an agent for Rogue Cloud.](Developing-CodingNextSteps.md)


