## Upgrading to the latest Rogue Cloud Client code using Microclimate

If you received an error message indicating that your Rogue Cloud client API version is no longer supported, or if just want to ensure you have the
most recent version of the Rogue Cloud client code, you can follow these instructions to get up-to-date.

There are three methods you may use to upgrade the Rogue Cloud client code.

### Recommended - Method #1: Using the Microclimate Code Editor (Theia) Git UI
With this method you can pull the latest changes from the official rogue-cloud-client (https://github.com/microclimate-dev2ops/rogue-cloud-client) repository entirely within the code editor UI.

1. In the code editor view, select `View` > `Git`.
2. Inside the Git view, Select the `...` Icon, then select `Pull...`
3. Under `Pick a remote to pull the branch from:` select `Origin`.

Once the latest changes are pulled, Microclimate will detect the change and you should see a notification in the `Build logs` view.


### Method #2: Using Git CLI from the Microclimate Code Editor's Terminal

For users that are familiar with the git command line tool, you can use this method to pull from the in-browser terminal:
1. In the code editor view, select `File` > `New Terminal`, which will open a new command line terminal inside the code editor.
2. You should already be in the `microclimate-workspace` directory. If not, type `cd /microclimate-workspace`
3. Pull the latest changes from the remote repository by running the following commands:
```
cd roguecloudclient
git pull
```

### Method #3: If you encounter errors using the above methods, and/or want to start your project from a clean slate (ie from scratch):

**WARNING**: This method will delete all the Rogue Cloud code, including any changes you made to your AI code. Make sure you have backed up any important changes to your AI code, if applicable. Don't forget to note your **username** and **password**!

**Steps**:
1. Read and heed the above warning :smile:.
2. Select `Projects`, to bring you to the Microclimate Projects view.
3. Click the `roguecloudclient` context icon (3 vertical dots), and select `Delete project`.
4. Follow the **Add Rogue Cloud to Microclimate** steps of the installation page (https://github.com/microclimate-dev2ops/rogue-cloud/blob/master/docs/Developing-Microclimate.md). This will a fresh copy of the rogue-cloud-client repository.
