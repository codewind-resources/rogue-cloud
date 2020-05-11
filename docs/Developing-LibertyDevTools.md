# Playing Rogue Cloud with Eclipse and IBM Liberty Developer Tools
Use Eclipse and IBM Liberty Developer Tools to play Rogue Cloud.

### A) Clone the Rogue Cloud repository and run a Maven build.
* Prerequisites:
  - [Install Git.](https://git-scm.com/)
  - Install the Java 8 JDK. The Java JDK is available from [Java SE Downloads](http://www.oracle.com/technetwork/java/javase/downloads/index.html). For more information, see [Installing the Java 8 JDK](Installing-Java.md).

1) Create a directory to contain your Rogue Cloud Git repository. Make a note of this directory because you will use it later. Enter the following commands into your command line:
```
mkdir <new repo directory>
cd <your new repo directory>
git clone https://github.com/codewind-resources/rogue-cloud.git
cd rogue-cloud
```
2) Verify the installation by building with Maven:
* **Linux/Mac**: ``./mvnw clean package ``
* **Windows**: ``mvnw clean package``

### B) Install Liberty Developer Tools in Eclipse.
1) Download the [Eclipse IDE for Java EE Developers](https://www.eclipse.org/downloads/eclipse-packages/).
2) Extract the application and run Eclipse.
3) In Eclipse, select **Help** from the menu. Then, select **Eclipse Marketplace**.
4) Enter **Liberty Developer Tools** into the **Find:** search box and click **Go**.
5) Find the **IBM Liberty Developer Tools** entry and click **Install**.
6) Click **Confirm**.
7) Accept the licenses. Then, click **Finish**. IBM Liberty Developer Tools installs.
8) When prompted to restart, click **Restart Now**.

### C) Create a new Liberty Server in Eclipse.
1) Select **Window** from the menu. Then, select **Perspective**>**Open Perspective**>**Other**. Select **Java EE** and click **OK**.
2) Select **File** from the menu. Then, select **New**>**Other...** followed by **Server**>**Server**. Click **Next**.
3) Select **IBM**>**Liberty Server**. Ensure that **Server's host name** is `localhost` and not `Cloud`. Click **Next**.
4) Select **Install from an archive or a repository**. Click **Next**.
5) Specify a path to download the Liberty server to. Then, select **Download and install a new runtime environment from ibm.com**.
6) Select **WAS Liberty with Java EE 7 Web Profile**. Click **Next**.
7) On the **Install Additional Content** window, click **Next**.
8) Accept the license, and then click **Next**.
9) Click **Next** and then **Finish**.
10) Wait for the Liberty server to download. It might take approximately 45 seconds.
11) See your server in the **Servers** view.

### D) Import your Rogue Cloud Git repository into Eclipse.
1) Select the **File** menu item. Then, select **Import...** followed by **Git**>**Projects from Git**. Click **Next**.
2) Select **Existing local repository**. Then, click **Next**.
3) Click **Add...**, and a new window opens. In the **Directory:** field, specify the directory that contains your Rogue Cloud Git repository. Click **Search** if needed.
4) Select the checkbox that corresponds to your Git repo. Then, click **Finish**.
5) Click **Next**.
6) On the **Import Projects** page, ensure that all the `RogueCloud` projects are selected. Then, click **Finish**.
7) A **Workspace Migration** window might appear after you click **Finish**. Click **Next**, then **Next**, and then **Finish**.
8) You now have a workspace that contains all of the `RogueCloud` projects from the Git repository.

### E) Publish the `RogueCloudClientLiberty` project to the Liberty server.
1) Select **Window** from the menu. Then, select **Show View**>**Other**. Select **Server**>**Servers**. Then, click **Open**.
2) In the **Servers** view, right-click **Liberty Server at localhost** and then select **Add and Remove...**.
3) Click **RogueCloudClientLiberty**. Then, select **Add**. Click **Finish**.
4) In the **Servers** view, right-click **Liberty Server at localhost** and select **Start**.

### F) Register a user and then make changes to the SimpleAI class.
1) Go to the **Java EE** perspective. In the **Enterprise Explorer** view, select **RogueCloudClientyLiberty**>**Java Resources**>**src**>**com.roguecloud.client.container**>**StartAgentServlet.java**.
2) Double-click **StartAgentServlet.java** to open the `StartAgentServlet` Java class.
3) Edit the following fields to create a new user and password:
```
public static final String USERNAME = "(specify a username here!)";
public static final String PASSWORD = "(specify a password here!)";
```
* Specify a user name and password here to automatically register them after your code begins to control a character on the game map.
4) In the **Enterprise Explorer** view, select **RogueCloudClient**>**src**>**com.roguecloud.client.ai**>**SimpleAI.java**.
5) This class is the main AI class. Changes made to this class are reflected in your AI that runs on the Liberty server.

### G) Watch your agent go and start coding.
To watch your agent as it interacts with the game world, look for the following message in the **Console** view:
```
***********************************************************************************************
*                                                                                             *
*    Agent has started. Watch at: http://localhost:9080                                       *
*                                                                                             *
***********************************************************************************************
```
Visit this URL in your browser to view your character. The traditional browser URL is   `http://localhost:9080`. The port can be adjusted with `RogueCloudClientLiberty` in the `pom.xml` file.

Next, [learn more about coding an agent for Rogue Cloud](Developing-CodingNextSteps.md).


