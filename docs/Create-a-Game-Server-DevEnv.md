## To create an Eclipse-based development environment for developing Rogue Cloud

Here is how you setup Eclipse to allow you to develop the Rogue Cloud Client and Rogue Cloud Server. These instructions are for RC developers who are adding __new features to the game or fixing game bugs__. If you just want to PLAY the game, see the [Getting Started](GettingStarted.md) instructions.



#### Some useful keyboard shortcuts:
- **NOTE**: Some keyboard shortcuts mentioned below will be slightly different on Mac (usualy the *Ctrl* becomes the *Command* key).
- Ctrl-Shift-O: Organize imports, which adds all referenced classes to the imports list at the top of the file.
- Ctrl-Shift-R: Find a file with a specific name, no matter what Eclipse project it is in
- Ctrl-Shift-T: Find a class with a specific name, no matter what Eclipse project it is in


#### A) Download and start Eclipse for Java EE Developers
1. Download the Eclipse IDE for Java EE Developers for your platform from https://www.eclipse.org/downloads/eclipse-packages/.
2. Unzip it to a path on your machine.
3. Start Eclipse, and specify a directory that will contain all your development files (your Eclipse 'workspace').

#### B) Install IBM Liberty Tools for Photon
1. Select Help > Eclipse Marketplace...
2. Enter 'Liberty Developer Tools' in the 'Find:' field. 
3. Select 'Install' next to the 'IBM Liberty Tools for Photon' entry. This will bring you to a list of optional features to install.
4. Ensure that 'WebSphere Application Server Liberty Tools' is selected. Click the Confirm button.
8. Accept the terms and conditions and click Finish. Restart Eclipse when prompted.

#### C) Clone the Rogue Cloud repository
1. Create a github.com account, if you don't already have one. Note that this is _Github.com_, not _Github.ibm.com_, which uses a different user id and password..
2. Log-in and visit: https://github.com/codewind-resources/rogue-cloud
3. Click 'Fork', then specify that you want to fork to your local environment (which will be https://github.com/your-username/rogue-cloud)
4. In Eclipse, select Window > Perspective > Open Perspective > Other...
5. Select 'Git' and click Open.
6. Click 'Clone a Git repository'.
7. In the dialog, paste the 'https://github.com/your-username/rogue-cloud' URL into the URI. Enter your Github User and Password. Click Next, Next, then Finish.
8. Wait for the repository to load (it should be quick)

#### D) Create a local Liberty server 
1. In Eclipse, select Window > Perspective > Open Perspective > Other...
2. Select 'Java EE' and click Ok.
3. Select File > New > Other.... 
4. In the 'New' dialog, select Server > Server, and click Next.
5. Select IBM > Liberty Server. Ensure that the 'Server's host name' field is set to 'localhost' (which is the default). Click Next.
6. Click the 'Install from an archive or repository' radio button. Click Next.
7. Under 'Enter a destination path', specify a new directory on your machine. (On later steps, Eclipse will automatically download Websphere Liberty into this directory).
8. Select the 'Download and install a new runtime environment from ibm.com:' radio button.
9. Select the 'WAS Liberty with Java EE 8 Full Platform' item. Click Next.
10. On the following page named "Install Additional Content", you don't need to select anything additional content. Just click Next.
11. Accept the license, and click Next, then click Finish.

Eclipse will now automatically download/install a new WebSphere Liberty into the directory you specified. This server will be visible in the 'Servers' view in Eclipse.

#### E) Configure the Liberty server

1. You may see an error in the Liberty Runtime  project. To correct it, select Window > Show View > Other... Select General > Markers. Click Open. This will open the 'Markers' view at the bottom of the screen
2. Right-click the error, which should be 'The enabled features require that a keyStore element and a user registry are defined in the server configuration.', and select Quick Fix. 
3. A new dialog, 'Quick Fix' will appear. Select 'Add the required server configuration' and click Finish.
4. A new dialog, 'Required Server Configuration' will appear. Enter the following:
	* Keystore password: Enter any password, this will not be used elsewhere and can be changed later.
	* User name: Enter any simple username, this will not be used elsewhere and can be changed later.
	* User password: Enter any password, this will not be used elsewhere and can be changed later.
	- These features are principally for securing applications running in production environments, so we don't need to worry about them here.
5. Switch to the Servers view, by selecting Window > Show View > Other..., then Server > Servers and Click Open. 
6. You will now see your new Liberty Server defined in the Servers view. If you double-click it, and select 'Open server configuration', you can see some of the configuration information.

You can learn more about using WebSphere Liberty (or it's open source version, Open Liberty) at WASDev.net and openliberty.io.

#### F) Import the Rogue Cloud projects from Git
1. Select New > Import..., then select Git > Projects from Git. Click Next.
2. Select 'Existing local repository' and click Next.
3. Select the name of your git repository, then click Next.
4. On this page, named 'Select a wizard to use for importing projects', just click Next.
5. On this page, 'Import Projects from Git', you should see 6 projects, and they should all be selected. 
6. Click Finish to import all the selected projects into your workspace.

The projects should now import into your workspace, and there should be no errors in the Markers view.

#### G) Add and publish the RogueCloudServer and RogueCloudClientLiberty projects to the Liberty Server
1. Open the Servers view. Right-click on 'Liberty server at localhost'. Select 'Add and Remove...'
2. Click 'Add All >>'. You should now see both RogueCloudClientLiberty and RogueCloudServer move to the right hand side of the dialog. Click Finish.
3. The projects will now be published to the server when the server first starts.
4. We next need to configure your player character. Hit 'CTRL-Shift-R' and type 'StartAgentServlet.java', to open that class.
5. Enter a username and password on these lines. These can be any values. This will be the name of your character when you are playing the game.
```
	public static final String USERNAME = "your-username";
	public static final String PASSWORD = "your-password";
```

6. Ensure that the SERVER_HOST_AND_PATH_NON_URL line is pointing to your localhost server on port 9080. It should look like this: 
```
//	public static final String SERVER_HOST_AND_PATH_NON_URL = "roguecloud.space:29080/RogueCloudServer";
	public static final String SERVER_HOST_AND_PATH_NON_URL = "localhost:9080/RogueCloudServer";
```
	**NOTE**: Notice that on the second line the port changes from localhost:29080, to localhost:9080 (eg we remove the leading 2)
	The roguecloud.space server is where public users play the game. Here, we want our character (running on our machine) to connect to our server (also running on our machine).

7. Now, in the Servers View, right click on Liberty Server at localhost, and click Start.

8. In the Console view, you should see this:
```
***********************************************************************************************
*                                                                                             *
*    Agent has started. Watch at: http://localhost:9080/RogueCloudClientLiberty/StartAgent   *
*                                                                                             *
***********************************************************************************************
```
If you instead see an error (like an invalid username/password), correct the issue and then Stop and Start the server again.

9. If you copy-paste the above URL into a browser, you can now watch your character play the game!

### Next Steps

After your environment has ben setup, return to the [Rogue Cloud Developer page](RogueCloudDevelopment.md) for the next steps.
