## Play Rogue Cloud using Eclipse Codewind

### A) Install Eclipse
- If you already have Eclipse installed, skip to the next section. Microclimate Developer Tools for Eclipse requires Eclipse 2019-03, or newer.

1) Visit the [Eclipse download page](https://www.eclipse.org/downloads/packages/).
2) Locate the `Eclipse IDE for Enterprise Java Developers` section, select your operating system, then click `Download`.
3) Wait for the file to download, then extract it to the directory of your choice.
4) Start Eclipse, specify a workspace directory (the default is fine), and wait for Eclipse to load.


### B) Installing Codewind plugins into Eclipse
- Requires: Eclipse 2019-03, or newer.

1) From within Eclipse, select `Help` (menu item) > `Eclipse Marketplace`.
2) Type `Codewind` in the search bar, then click `Go`.
3) You should now see `Codewind` in the search listings. Click the `Install` button next to these tools.
4) Read and accept the licenses, then click `Finish`.
5) After the install completes, you will be prompted to restart Eclipse, click Restart.

See the Eclipse Codewind documentation for more information [on Installing Codewind into Eclipse](https://www.eclipse.org/codewind/mdteclipsegettingstarted.html).

#### C) Installing Codewind container images

1) Open the Codewind view. Navigate to `Window` (menu item) > `Show View` > `Other...` > `Codewind` > `Codewind Explorer`
2) Codewind requires the installation of additional Docker images to run. Double-click on the Codewind item in the Codewind Explorer view to complete the installation. The installation may take a few minutes to complete.
-  Codewind creates a folder called codewind-workspace within your home directory (C:\codewind-workspace on Windows) to contain your projects.

You are now ready to use the tools. You can use the Codewind Explorer view to create new projects or add existing ones. Right-click an element in the Codewind Explorer to look at the features available.

### D) Git clone the Rogue Cloud client into Codewind workspace directory.

1) Determine the location of the Codewind workspace directory:
- From Mac/Linux: `docker inspect codewind-pfe | grep "HOST_WORKSPACE_DIRECTORY="`
  - Example: `"HOST_WORKSPACE_DIRECTORY=/home/user/codewind/codewind-workspace"` means your workspace can be found in `/home/user/codewind/codewind-workspace`
- From Windows: `docker inspect codewind-pfe | find "HOST_WORKSPACE_DIRECTORY="`
  - Example: `"HOST_WORKSPACE_DIRECTORY=C:\\codewind-workspace"` means the Codewind workspace is `c:\codewind-workspace`
2) From within the `codewind-workspace` directory, clone the Rogue Cloud client repo
  ```
  cd (path to your codewind workspace from the previous step)
  git clone https://github.com/microclimate-dev2ops/rogue-cloud-client
  ```
3) 
