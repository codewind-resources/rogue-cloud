## Installing Microclimate 

### A) Download Microclimate

1) Visit https://microclimate-dev2ops.github.io/installlocally
2) Click 'Download' and download ``microclimate-(version number).zip``
3) Install the prerequisites:
* Install Docker 17.06 and up (if not already installed)
* **Windows**
  * Windows 10 or Windows Server 2016
  * Docker for Windows
  * On Windows 10, we strongly recommend that you do not run using Experimental Features which is enabled by default in Docker. To turn it off, go to ``Docker``->``Settings``->``Daemon`` and de-select ``Experimental Features``.
* **MacOS and Linux**
  - Install [Docker Compose](https://docs.docker.com/compose/install/) (if not already installed)
  - Supported architecture: x86-64 only
  - **Linux**: Follow [post-installation steps to run docker as a non-root user](https://docs.docker.com/engine/installation/linux/linux-postinstall/).
4) Follow these setup instructions depending on which operating system you are installing from.
  - More information on installing Microclimate is available from our [Getting Started page](https://microclimate-dev2ops.github.io/installlocally).
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
    - Add the `mcdev` utility to your path:
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

**Windows**:
```
mcdev start -o
```

**Mac/Linux**:
```
~/mcdev start -o
```

The Microclimate CLI downloads the Microclimate Docker images and starts the development containers. Once complete, you should see a Microclimate has started message, and the Microclimate browser UI will open in your browser.

If your browser did not automatically open, you may access the browser UI by running ``mcdev open`` or visiting: http://localhost:9090/


## To uninstall Microclimate and Rogue Cloud

To uninstall, see the instructions on the Microclimate installation page:
* https://microclimate-dev2ops.github.io/gettingstarted
