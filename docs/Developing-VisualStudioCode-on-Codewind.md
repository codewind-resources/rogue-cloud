## Play Rogue Cloud using Microclimate Visual Studio Code Tools

### A) Install Visual Studio Code
- If you already have Visual Studio Code installed, skip to the next section.

1) Visit https://code.visualstudio.com/Download and select your operating system.
2) After the download completes, run the installer (Windows) or install the package (Linux/MacOS).
3) After the install is completed, run Visual Studio code.

### B) Install the Codewind prerequisities: Docker, Docker Compose, and Git

Installation prerequisites:
- *On Mac*: Install 'Docker Desktop for Mac' and Git (on this platform Docker Compose is bundled with Docker Desktop)
- *On Windows*: Install 'Docker Desktop for Windows' and Git (on this platform Docker Compose is bundled with Docker Desktop)
- *On Linux*: Install Docker, Docker Compose, and Git (on this platform, unlike Mac/Windows, Docker Compose *must* be downloaded separately)

Additional configuration steps for these platforms may be required. See the [Codewind local install documentation](https://www.eclipse.org/codewind/installlocally.html) for full details.

### C) Installing Codewind Tools into Visual Studio Code

1) In Visual Studio Code, select `View` (menu bar) > `Extensions`. Under `Search Extensions in Marketplace`, enter `Microclimate`.
2) Select `Codewind`, and click the `Install` button on the right-hand screen.

3) If you are asked to restart Visual Studio Code, do so. Otherwise proceed to the next step.

4) You will now be presented with a dialog: `"Codewind requires the installation of Docker container to run, which might take a few minutes to download. Do you want to complete the installation now?"`. Click `Install`.

5)  You should see a status message `Pulling Codewind Docker images` on the bottom right hand corner of the screen. Wait for this to complete. After the images are downloaded, you should see `Starting Codewind`, then `Codewind installation is complete`. Click OK.

6) Select `View` (menu bar) > `Explorer`. On the bottom left hand corner of the explorer, you should see 'Codewind'. Expand this panel if not already expanded.

More information on [installing Codewind into Visual Studo Code](https://www.eclipse.org/codewind/mdt-vsc-getting-started.html) is available from our website.

### D) Git clone the Rogue Cloud client into Codewind workspace directory.

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

