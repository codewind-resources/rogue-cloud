## Installing Microclimate and importing Rogue Cloud

### A) Download Microclimate

1) Visit https://microclimate-dev2ops.github.io/gettingstarted
2) Click 'Download' and download ``microclimate.zip``
3) Install the prerequisites:
* Install Docker 17.06 and up (if not already installed)
* **Windows**
  * Windows 10 or Windows Server 2016
  * Docker for Windows
  * We strongly recommend that you do not run using Experimental Features enabled (which is enabled by default in Docker) when running Windows 10. To turn it off, go to Docker->Settings->Daemon and de-select Experimental Features.
* **MacOS and Linux**
  - Install Docker Compose (if not already installed)
  - Supported architecture: x86-64 only
  - **Linux**: Follow [post-installation steps to run docker as a non-root user](https://docs.docker.com/engine/installation/linux/linux-postinstall/).
4) You can either follow the instructions on the Microclimate Getting Started page under the 'Local microclimate deployment', or follow these setup instructions:
  * **On Linux and MAC OS**:
    - *Linux only*: you may need to install the unzip tool, for example: ``sudo apt-get install unzip``
    - Unzip the ZIP file and run the install script:
    ```
    unzip microclimate.zip
    cd microclimate
    cd cli
    ./install.sh
    cd ..
    ```
    - Add the 'mcdev' utility to your path:
    ```
    export PATH=~/.microclimate:$PATH
    ```
  * **On Windows**:
    - Extract the downloaded file and run the install script in PowerShell:
      1) Open the file properties menu for the downloaded file. 
      2) Tick the ``Unblock`` box on the General tab then click OK.
      3) Unzip the downloaded file in File Explorer.
      4) Open a PowerShell session and change directory into the microclimate directory. 
      5) Run ``cli\install.ps1``, to install the Microclimate CLI.

### B) Start Microclimate

To start Microclimate, run the ``mcdev start -o`` command.
```
mcdev start -o
```

The Microclimate CLI will download the Microclimate Docker images and start the development containers. Once complete, you should see a Microclimate has started message, and the Microclimate browser UI will open in your browser.

If your browser did not automatically open, you may access the browser UI by running ``mcdev open`` or visiting: http://localhost:9090/


### C) Add Rogue Cloud to Microclimate

Next, we will need to download the standalone Rogue Cloud client ZIP and unzip it into the appropriate spot in the ``microclimate-workspace directory``. First, however, we need to create a new project to contain it.

1) In the Microclimate browser UI, click the ``here`` link to open the project creation dialog. Select the '.java' icon.
2) Specify ``gameclient`` as the project name. You must specify this name *EXACTLY*, as we will be importing files into this directory in later steps.
3) Click ``Next``.
4) Click ``Microprofile``, and do NOT edit the 'Context root' field from the default (the default is gameclient.) Click ``Create``.
5) Once the code is generated, click ``Edit Code``. You will be redirected to the code editor.
  *  Note: You might see 'You are not authorized to access /file/anonymous-OrionContent/gameclient', this warning can be safely ignored.
6) Before you start building the code, the container will need to initialize and download the Java and Maven dependencies for the underlying build system. This will take a while -- up to 8 minutes depending on CPU and network connection (this is an early beta of Microclimate: we're working on speeding up this step dramatically! Thanks for your patience! :)   ). You can use ``docker logs -f microclimate-file-watcher`` to watch its progress.
7) Once the build has initialized and downloaded the required dependencies, the build icon displays a red circle notification, like so:
![Orion ready to go](resources/gameclient-orion-ready.png "Orion ready to go")
* In the build logs window, you should the Maven build log with 'BUILD SUCCESS' at the bottom.

8) Download and unzip the standalone game client, as follows:
* Download the file from here: http://www-rogue-cloud.mybluemix.net/RogueCloudLibertyStandalone.zip
* On the Terminal (or Command Prompt):
    #### Linux:
    ```
    cd ~/microclimate-workspace
    sudo unzip -o (path to downloaded ZIP)/RogueCloudLibertyStandalone.zip
    ```

    #### Mac OS:
    ```
    cd ~/microclimate-workspace
    unzip -o (path to downloaded ZIP)/RogueCloudLibertyStandalone.zip
    ```
    
    #### Windows OS:
    1) In File Explorer, go to the previous installation directory of Microclimate (where you downloaded and extracted the microclimate.zip file)
    2) Open the ``microclimate-workspace`` directory.
    3) Download and copy the ``RogueCloudLibertyStandalone.zip`` into this directory, then extract it. (When prompted, overwrite any existing files.)

### D) Register a user and then make changes to the SimpleAI class

1) In the code editor, hit ``CTRL-SHIFT-F`` (``Command-Shift-F`` on Mac) and type ``StartAgentServlet.java``, and select ``StartAgentServlet.java``.
* ``CTRL-SHIFT-F/Command-Shift-F`` is a great way to find quickly Java classes in the the Microclimate code editor.
* *Note*: You *may* need to click inside the code editor for this shortcut key to work.

2) Edit the following fields to create a new user and password.
```
public static final String USERNAME = "(specify a username here!)";
public static final String PASSWORD = "(specify a password here!)";
```
* The  username and password you specify will be automatically registered  once your code first begins controlling a character on the game map.

3) Hit ``CTRL-SHIFT-F`` and type ``SimpleAI`` and select ``SimpleAI.java``. (On Mac, use ``Command-Shift-F``).

4) This class is the main AI class. Changes made to this class will be reflected in your AI running on the Liberty Server.


### E) Next steps: watch your agent go, and start coding

To watch your agent as it interacts with the game world, switch to the application view. You will see a URL at the top of the page:

Add ``/StartAgent`` to the end of the URL, such that it looks like:
``http://localhost:(port)/gameclient/StartAgent``
* where (port) is the randomly generated local port for the server.


Congratulations, your character is now exploring and interacting with the game world, and earning you points on the leaderboard!

Next, [visit the next steps page to learn more about coding an agent for Rogue Cloud.](Developing-CodingNextSteps.md)



## To uninstall Microclimate and Rogue Cloud

#### Linux/Mac OS - Run the following commands:

1) Stop the Microclimate docker containers:
```
mcdev stop
```

2) Remove the .microclimate and microclimate-workspace directories:
```
cd ~
cd .microclimate
rm *.yaml
rm mcdev
rm -r .env
cd ..
rmdir .microclimate
rm -r microclimate-workspace
cd ..
```

3) Remove the Microclimate images
  * Either remove dangling Docker images:
    ```
    docker system prune
    ```
  * Or, remove all Docker images:
    * *Warning*: this will [delete all Docker images](https://docs.docker.com/engine/reference/commandline/system_prune/), not just Microclimate Docker images.
    ```
    docker system prune --all 
    ```


#### Windows - Run the following commands:

1) Stop the Microclimate docker containers:
```
mcdev stop
```

2) Remove the .microclimate and microclimate-workspace directories, from the Command Prompt:
```
cd c:\Users\(your login name)
cd .microclimate
del *.yaml
del mcdev
del .env
cd ..
rmdir .microclimate
rmdir /s /q microclimate-workspace
```

3) Remove the Microclimate images:
```
docker system prune
```

## Troubleshooting Microclimate and Rogue Cloud

### If a change to the code does not result in a rebuild, use 'mcdev stop' and 'mcdev start' to kick Microclimate back into gear.

Any changes you make to the code in the Microclimate code editor should automatically kick off a build, which will then be automatically deployed to the server. If you are finding that your changes are not kicking off the automated builds, it is possible that the build it is out-of-sync (this is an early beta of Microclimate, after all!). 

To resolve this issue, run the following on your Terminal or Command Prompt, to restart Microclimate:
```
mcdev stop
mcdev start
```


### Error message in the 'Open Application' page of Microclimate: "localhost refused to connect" (Chrome), 'Unable to connect' (Firefox), or blank screen (especially after restart of microclimate using mcdev stop then mcdev start)

The port that microclimate listens on can change between restarts of Microclimate. We need to call Docker to find the newly assigned port.

**On Windows, run**:
```
C:\>docker ps | find "microclimate-dev"
51b994aef26e        microclimate-dev-liberty-gameclient-idc-39aa835df45ef522d21eb77b2c0f8cfc1fc20627   "/root/artifacts/newÔÇª"   7 minutes ago       Up 7 minutes        0.0.0.0:32771->9080/tcp, 0.0.0.0:32770->9443/tcp   microclimate-dev-liberty-gameclient-idc-39aa835df45ef522d21eb77b2c0f8cfc1fc20627
```

**On Linux/Mac, run**:
```
docker ps | grep "microclimate-dev"
51b994aef26e        microclimate-dev-liberty-gameclient-idc-39aa835df45ef522d21eb77b2c0f8cfc1fc20627   "/root/artifacts/newÔÇª"   7 minutes ago       Up 7 minutes        0.0.0.0:32771->9080/tcp, 0.0.0.0:32770->9443/tcp   microclimate-dev-liberty-gameclient-idc-39aa835df45ef522d21eb77b2c0f8cfc1fc20627
```

Look for the line that looks like this: 0.0.0.0:**32771**->9080/tcp, 0.0.0.0:32770->9443/tcp"
In this case, **32771** is the new port we Use the port (not 9080 or 9443). 

In Microclimate, click on the **Open application** icon and replace the old port with the new one, by entering the following URL:
* ``http://localhost:(port from above)/gameclient/StartAgent``
* For example: ``http://localhost:32771/gameclient/StartAgent``

### After restarting microclimate using 'mcdev stop' and 'mcdev start', you're not able to see the Rogue Cloud browser UI

If after restarting mcdev, you are able to load the Rogue Client browser UI but the world data does not stream in, you may have tripped logic that prevents players from trying to join twice with two different UUIDs.  Wait for the current round to end and try again (and we hope to remove this restriction in the future.)
