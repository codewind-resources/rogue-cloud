
## Coding a Rogue Cloud Agent

This page focuses on coding for Rogue Cloud within the development environment you've setup.
* Click here to [learn about setting up a development environment](GettingStarted.md).
* Click here to [learn more about the key concepts of Rogue Cloud](README.md).


### Coding the SimpleAI class

The ``SimpleAI`` class provides a sample implementation and framework to easily begin hacking on your agent code. In fact, the ``SimpleAI`` class is ready to go right out of the box! To start your agent code, follow the build and run instructions from the development environment setup page.

When you've got your code running, the next step is to focus on improving it. The ``SimpleAI`` class acts based on the implementation that you provide in each of the following methods:
* **whereShouldIGo**: What coordinate on the map should I start moving towards?
* **shouldIPickUpItem**: I see an item, should I pick it up?
* **shouldIAttackCreature**: I see a creature, should I attack them?
* **shouldIDrinkAPotion**: Should I drink a potion this turn?
* **shouldIEquipNewItem**:  Should I equip a new item that I just picked up?
* **beingAttackedShouldIAttackBack**:  Help, I am being attacked, should I attack back?

It is up to your code to answer these questions, by improving upon the existing code contained in these methods.

Examine the source for each of these methods, and learn more about how the agent API interacts with the game world. The agent API are classes that you can use to interact with the world. All are fully documented to provide you information on what their purpose is and how to use them. Examples include ``SelfState/WorldState``, which provide you with information on yourself/the world, or ``IMap/Tile``, which let you see the contents of a particular map location.

More [information on key concepts are available here](README.md).

### How to locate code, and learn about the Agent API classes

While hacking on your agent code, it is important to be able to learn about the agent API classes and their methods. The best way to do this is to open their source in the git repo.

To locate the source and documentation for a Rogue Cloud class, use the 'Goto' key/shortcut for your development environment of choice.
* **Eclipse**: CTRL-R
* **Visual Studio Code**: CTRL-P
* **Microclimate**: CTRL-P  (Command-P on Mac)
* **Sublime Text**: CTRL-P
* **Atom**: CTRL-P
* *(Note: On Mac, some editors use the Command key rather than CTRL key)*.

So, for example, if you are using Visual Studio Code, and you are looking for the ICreature class, hit CTRL-P, type ``ICreature.java``, and hit Enter.

Likewise in Microclimate, hit CTRL-P (Command-P on Mac), type ``ICreature``, and look for the .java file in the list.

### Javadocs for the Agent API classes

Javadocs descriptions for the [API classes and method are available here](http://www-rogue-cloud.mybluemix.net/).


### Locations of important Agent API classes

You can also find the source files in the project source tree. Here are the locations of some of the most important classes:
```
(First, goto gameclient/src/main/java)

com/roguecloud/client/ai/SimpleAI

com/roguecloud/creatures/ICreature

com/roguecloud/map/IMap
com/roguecloud/map/Tile

com/roguecloud/client/container/StartAgentServlet

com/roguecloud/client/SelfState
com/roguecloud/client/WorldStae
com/roguecloud/client/IEventLog

com/roguecloud/Position

com/roguecloud/utils/AIUtils
com/roguecloud/utils/AStarSearch
com/roguecloud/utils/SimpleMap


com/roguecloud/items/Armour
com/roguecloud/items/Weapon
com/roguecloud/items/Effect
com/roguecloud/items/DrinkableItem
com/roguecloud/items/OwnableObject
com/roguecloud/items/IGroundObject
com/roguecloud/items/IObject
```

