
### A) First, clone the Rogue Cloud repository and run a Maven build
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


### B) Install the development environment of your choice. 
- [Developing Rogue Cloud using the Microclimate Beta](Developing-Microclimate.md)
- [Developing Rogue Cloud using a Text Editor (Sublime, VSCode) and Maven](Developing-Text-Editor.md)
- [Developing Rogue Cloud using Eclipse and IBM Liberty Developer Tools](Developing-LibertyDevTools.md)

### C) [Edit the agent code and watch it explore the game world from your browser.](Developing-CodingNextSteps.md)
