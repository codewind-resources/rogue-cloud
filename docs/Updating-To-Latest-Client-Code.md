# Upgrading to the latest Rogue Cloud client code

If you received an error message indicating that your Rogue Cloud client API version is no longer supported or if you just want to ensure you have the most recent version of the Rogue Cloud client code, follow these instructions to get up to date.

## Updating from Codewind

If you use Codewind, then how you update depends on which IDE you are using. Or, regardless of IDE, you can update by using the Git CLI if you prefer.

### If you are using Codewind Visual Studio Code (VS Code) Tools

1. Select `View` from the menu bar and `SCM`.
2. Look for the `...` icon in the `Source Control: Git` view, click the `...` icon, and then select `Pull`.

### If you are using Codewind Eclipse Tools

1. Right-click `gameclient` in the `Enterprise Explorer` or `Project Explorer` view. 
2. Select `Team`>`Pull`.

### Or, if you prefer, call `git pull ` from the command line

1. Locate your project on disk:
- In **Visual Studio Code**, right-click your project in the `Codewind` view and select `Open Project Overview`. Then, look for the `Location on Disk` field.
- In **Eclipse**, right-click your project in the `Codewind Explorer` view and select `Open Project Overview`. Then, look for the `Location on Disk` field.
2. From the command line, enter `cd (path to project on disk)` and `git pull`.

## Updating from Microclimate

You can use any of three methods to upgrade the Rogue Cloud client code from Microclimate.

### Method 1: Use the Microclimate Code Editor (Theia) Git UI.
With this method, you can pull the latest changes from the official [rogue-cloud-client repository](https://github.com/microclimate-dev2ops/rogue-cloud-client) entirely within the code editor UI.

1. In the code editor view, select `View`>`Git`.
2. Inside the Git view, select the `...` icon. Then, select `Pull...`.
3. From `Pick a remote to pull the branch from:`, select `Origin`.

After the latest changes are pulled, Microclimate detects the change, and you see a notification in the `Build logs` view.

### Method 2: Use the Git CLI from the Microclimate code editor terminal.

If you are familiar with the Git command line tool, you can use this method to pull from the in-browser terminal:
1. In the code editor view, select `File`>`New Terminal`, which opens a new command line window inside the code editor.
2. You should already be in the `microclimate-workspace` directory. If not, enter `cd /microclimate-workspace` to go to the correct directory.
3. Pull the latest changes from the remote repository by running the following commands:
```
cd roguecloudclient
git pull
```

### Method 3: Use this method if you encounter errors with the previous methods or if you want to start your project from scratch.

**WARNING**: This method deletes all the Rogue Cloud code including any changes you made to your AI code. Make sure you have backed up any important changes to your AI code, if applicable. Don't forget to note your **username** and **password**!

1. Read and heed the warning. :smile:
2. Select `Projects` to go to the Microclimate Projects view.
3. Click the `roguecloudclient` context icon. Then, select `Delete project`.
4. Follow the **Add Rogue Cloud to Microclimate** steps on the installation page (https://github.com/codewind-resources/rogue-cloud/blob/master/docs/GettingStarted.md). Now, you have a fresh copy of the `rogue-cloud-client` repository.
