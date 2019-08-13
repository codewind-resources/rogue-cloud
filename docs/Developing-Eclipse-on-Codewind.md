## Play Rogue Cloud using Eclipse Codewind

### A) Install Eclipse
- If you already have Eclipse installed, skip to the next section. Microclimate Developer Tools for Eclipse requires Eclipse 2019-03, or newer.

1) Visit the [Eclipse download page](https://www.eclipse.org/downloads/packages/).
2) Locate the `Eclipse IDE for Enterprise Java Developers` section, select your operating system, then click `Download`.
3) Wait for the file to download, then extract it to the directory of your choice.
4) Start Eclipse, specify a workspace directory (the default is fine), and wait for Eclipse to load.

### B) Install the Codewind prerequisities: Docker, Docker Compose, and Git

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

1) Open the Codewind view. Navigate to `Window` (menu item) > `Show View` > `Other...` > `Codewind` > `Codewind Explorer`
2) Codewind requires the installation of additional Docker images to run. Double-click on the Codewind item in the Codewind Explorer view to complete the installation. The installation may take a few minutes to complete.
-  Codewind creates a folder called codewind-workspace within your home directory (C:\codewind-workspace on Windows) to contain your projects.

You are now ready to use the tools. You can use the Codewind Explorer view to create new projects or add existing ones. Right-click an element in the Codewind Explorer to look at the features available.

### E) Git clone the Rogue Cloud client into Codewind workspace directory.

1) Determine the location of the Codewind workspace directory:
- *Mac/Linux*: `docker inspect codewind-pfe | grep "HOST_WORKSPACE_DIRECTORY="`
  - Example: `"HOST_WORKSPACE_DIRECTORY=/home/user/codewind/codewind-workspace"` means your workspace can be found in `/home/user/codewind/codewind-workspace`
- *Windows*: `docker inspect codewind-pfe | find "HOST_WORKSPACE_DIRECTORY="`
  - Example: `"HOST_WORKSPACE_DIRECTORY=C:\\codewind-workspace"` means the Codewind workspace is `c:\codewind-workspace`
2) From within the `codewind-workspace` directory, clone the Rogue Cloud client repo
  ```
  cd (path to your codewind workspace from the previous step)
  git clone https://github.com/microclimate-dev2ops/rogue-cloud-client
  ```
3) Import the project into Eclipse: Select `File` (menu item) > `Import...`, then in the dialog select `General` (tree item) > `Existing Projects into Workspace` and click `Next >`.
4) Select `Select root directory` and click `Browse...`, select `(codewind workspace path)/rogue-cloud-client`, then click `Select Folder`. Click `Finish`. Wait for the project to build.
5) Right-click on `Codewind` (in `Codewind Explorer` view) > `Local Projects` > 'Add Existing Project...'.
6) `gameclient` should appear in the checkbox list, select it (if not already selected), then click `Next >`.
7) Select `MicroProfile / Java EE` (if not already selected), then click `Finish`. 
8) Before the code starts building, the container needs to initialize and download the Java and Maven dependencies for the underlying build system. This can take up to 7 to 10 minutes depending on CPU and network connection (this initialization is only required the first time you use the Codewind tools). 

Additional information about creating and [importing projects into Codewind is available our website.](https://www.eclipse.org/codewind/mdteclipsegettingstarted.html)

