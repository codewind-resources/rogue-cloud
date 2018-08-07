## Introduction

Rogue Cloud is a game, but unlike a traditional game where you control it with a controller, keyboard, or mouse, with Rogue Cloud your game character is controlled directly by your code: fight monsters, quaff potions, build up your character, and try to survive the final swarm. The code that you write is packaged into a websocket-based microservice, which like a traditional web microservice, provides or supports a variety of cloud-native requirements.

Learn more about the [technology behind the game](../README.md#tech).

> “In the farthest corners of your data center, something has awoken: programs are becoming self-aware and thinking for themselves, but they are not alone. Only the best programs will survive the harsh digital landscape and escape the network.”

## Getting Started

To run the Rogue Cloud game client on your local machine see [Rogue Cloud Getting Started](GettingStarted.md).

For developers interested in how the game server is implemented, or that want to add new features to the game, visit the [Rogue Cloud Game Server documentation](RogueCloudDevelopment.md) page.

## Key Concepts

Your character in the game world is referred to as an agent. Your agent is entirely controlled by the code that you write. What the agent sees around it is communicated to your code by the Rogue Cloud game server through JSON over Websockets, and the actions of your agent (move, attack, equip item, and so on) are communicated back to the server over the same channel.

Your agent may perform up to 10 actions per second. Each opportunity to perform an action is referred to as a frame or game engine tick (depending on your terminology). This means your character agent may move up to 10 squares, attack up to 10 times, or some combination thereof.

## The Game World API: SelfState, WorldState, IMap, and IEventLog

You can access information about your character, and the world, using these four classes:
* ``SelfState``: Represents your character: it contains an ``ICreature`` object for your character, which lets you check where they are in the world, their HP, inventory, weapons, armour, and more.
* ``WorldState``: Represents your view of the world: it contains an IMap object, which contains everything your character can see (or has previously seen). But, see section below about 'Fog of War'!
* ``IMap``: Each coordinate in the world is known as a ``Tile``, and each tile can contain others creatures, items, or terrain (walls or buildings).
  * To see what's on a tile, call ``getTile(...)`` on ``IMap``, or use the utility methods in the AIUtils class to search a bunch of different tiles at once.
 * ``IEventLog``: Contains a list of all the events of other players that your character saw (attacks, moves, items picked up, and so on). This includes not only the previous turn, but also a number of previous turns too.

All agent API classes are fully documented in the code itself but do let us know if you have any questions!

## Actions: What can my character do?

Your agent has a full open world to explore and conquer. Interacting with the world is realized by sending the game server the actions you want your character to perform.

Your agent may interact with the world using the following actions:
* **Step action**: Your character may move ("step") one square at a time per frame.
* **Combat action**: You may attack another creature that is within a single tile of your character.
* **Move inventory item (pick up item, or drop item)**: You may pick up an item (potion, weapon, armour) from the ground, or drop an item from your inventory to the ground.
* **Drink item action**: You may drink a potion that is in your inventory.
* **Equip item action**: You may equip or remove a piece of weapon or armour that is in your inventory.

Passive Actions:
* **Defend self action**: Your character automatically defends themselves against all attacks. There is no need to issue a 'defend' action for this.
* **Potion effects**: For potions that last multiple turns, your character is automatically granted the effects of the potion at the beginning of the turn.

## Game rounds: How do I enter and how long are they?

Your agent code automatically enters the current game round as soon as your code starts running. Each round lasts 5 minutes, and  there is only ever a single round running at a time. At the end of a round, there is a 20 second pause, at which point a new round begins. Your agent code will automatically enter a new round as soon as it begins.


## Score

At the end of a round, you receive a score based on how well your character did during the round:
* Killing a monster increases your score by 1000 * monster level.
* Each turn that you survive (are alive) your score is increased by 1.
* Drinking a potion will increase your score by 50.
* Your score increases whenever your character equips a better piece of weapon or armour than what previously existed in the game.
  * For armour, each increased point of defense adds 100 to your score.
  * For weapons, your score increases by 10 * (weapon rating increase), where weapon rating = (attack dice size *  number of attack dice) + attack plus.
  * Only armour or weapon upgrades increase your score: equipping a weaker piece of weapon or armour does not effect your score, nor does re-equipping the same piece of armour more than once.

## Fog of War

Your agent can only see a small part of the world at a time. This 'fog of war' means that there are large parts of the game world that are fully invisible to your agent code at any given time.

Your character can only see within their 'view', which is approximately 80 squares wide by 40 tiles high. These values are the 'view width' and 'view height', respectively.

Your character does not 'forget' what they have previously seen on tiles that are no longer in their view. Instead the tile data of no-longer-visible tiles remains in the IMap object, and can be queried at any time. However, note that the information in these tiles is no longer a reflection of the current reality of the game world, but rather a reflection of the game world as it existed when the tile was last observed.

Tiles which were previously seen, but are not currently in view, are called 'stale tiles'. To determine if a tile is stale, call the ``Tile.getLastTickUpdated(...)`` method, which tells you the last time your character saw this tile. To get up-to-date information on this tile, simple move your character to a position where the tile is visible again.


## My character died, now what?

When your agent's life (their 'hit points', known as HP) drops to 0, they are considered 'dead' and are temporarily no longer able to interact with the world. Fortunately, after 200 game ticks (about 20 seconds), your agent comes back to life, and your character begins interacting with the world again!

However, dying is not zero cost. There is a steep price to be paid:
* When your character dies, they randomly drop 50% of all the items: this includes potions, weapons, and armour (both those equipped, and those in their inventory). If another player finds your stuff before you revive, you won't be able to get it back!
* In addition, when your character revives, their maximum HP is reduced. If your character had 100 max HP before they died, they have 80 max HP after they die. This reduction of max HP occurs on every death, so try not to make a habit of it.
* Finally, and most importantly, when your character dies so does your code! A player death triggers an agent restart, which means that all the data your code had stored is now lost. Like a traditional web-based microservice, it is necessary to preserve your application data in an external database (or persistence layer) to guard against program crashes.
  * Use the ``RemoteClient.getKeyValueStore()`` method to store data so that it can be retrieved between restarts.


## How to improve your character over the sample agent

The sample agent provides an API that can be used to implement a sophisticated and effective game character, howver, the default behaviours of the sample agent leave much to be desired.

#### Here are a few good places to start:
* The default ``shouldIPickUpItem`` implementation picks up the first thing it sees. A better idea is to only pick up items that are an improvement over what the character already has.
* The default ``shouldIAttackCreature`` implementation attacks the first thing it sees. This is often not a good idea: some monsters are more powerful than others, and the character may want to consider avoiding attack if its health is low.
* The default ``whereShouldIGo`` implementation picks random spots on the map. A better strategy is to avoid areas that we have already seen, so as to discover as much of the world as possible.
* The default ``shouldIDrinkAPotion`` implementation drinks a random potion when the character's health drops below 50%, and keeps on drinking potions until it is above 50%. Since potions can heal over multiple turns, some potions may overheal the player, and some potions don't heal at all. A better strategy would be to be carefully analyze the situation and choose potions to reflect the immediate need.
* The default ``shouldIEquipNewItem`` implementation equips anything that is picked up. A better idea is to only equip items that are an improvement over what is already equipped.
* The default ``beingAttackedShouldIAttackBack`` implementation ALWAYS attacks back. Some monsters are much tougher than your player character, and fleeing would likely be a better option!

#### Advanced tips:
* Build your own strategies! Think unconventionally... for example, what if you built a character that didn't attack monsters, but instead waited for other players to die, and then stole their dropped equipement?
* Since your code will be restarted on death, consider using ``getKeyValueStore()`` and ``IKeyValueStore`` to persist information across restarts, or across character rounds.
