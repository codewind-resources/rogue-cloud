### JsonWorldState: the main mechanism for communicating what's going on in the game world

The vast majority of the game world and its current state is communicated from the game server to the game client, using the `JsonWorldState` object. Within this JSON object is a description of a small part of the world: the player characters can't see the full game world, they can only see a small section of it.


The driving requirement behind the format/protocol of the JSON object is saving bandwidth: since this is a multiplayer game, and the game is hosted externally (eg in the IBM Cloud), we need to use as little bandwidth as possible per user. Likewise, generating large JSON objects takes more CPU time per user, which reduces the concurrent number of players that can play at the same time.

For this reason, the protocol follows the DRY principle: *don't repeat yourself*. For example, if the game server knows that it has previously sent the game client the contents of the weapon object with id 53, it will not send information about that weapon again (it will just assume that the client remembers it.) Likewise, using partial frame updates (described below), only the parts of the world that have changed (from frame to frame) are sent to the player, rather than a full dump of all the tiles in view for each frame. These, plus other strategies such as compression, are highly effective at reducing game bandwidth.

The contents of this JSON is created by the WorldStateJsonGenerator class, which is called from the game loop.

#### Full vs partial frame updates

In order to save bandwidth, the server may send 2 kinds of update:
- Full frame update
- Partial frame update

A full frame update includes all the data from the full 80x40 grid of tiles (*width x height*) that the character can see. This means the server is sending 3200 tiles, which, even when compressed, is still a lot of data. To avoid having to send all this data 10 times a second, the server will also send partial frame updates. 

A partial frame update only sends to the character the tiles in the world that changed from the last frame. It is a "delta" from the previous frame. Since the game client has already received a full frame update, there is no need to send a bunch of data that has not changed since the last frame. Since only 1-2% of the data changes between frames, a partial update is much smaller than a full update.

In order to determine whether you are being sent a full frame or a partial frame, check the `full` field of the frame update (true if full, false otherwise). 


#### The worldState.frames data:

The 80x40 grid of tiles that surround the player character can be found in  the `worldState.frames` section of the `JsonFrameUpdate` JSON. 

The frames field communicates all of the tile changes that occurred in the world since the previous frame. In the case of a full update, the frames field will contain all 80x40 tiles. However, for non-full frames, it will contain only the individual tiles in that grid that have been updated. 

The frames field is an array of frame updates, with each containing the following the updated tile data for a *width w by height h* square, positioned at world coordinate `(x, y)`:
- **x**: the x coordinate of the position in the world* we are updating
- **y**: the y coordinate of the position in the world* we are updating 
- **w**: the width of the square of data that we are updating
- **h**: the height of the square of data that we are updating
- **data**: an array of tile squares `(an array of w x h)`

(The x and y coordinates are actually relative to the client view coordinates. So the actual x world coordinate is `x + clientViewPosX`, and the actual y world coordinate is `y + clientViewPosY`)

The `data` field is an array that looks like this. 
```
[ 
	 [ tile square data for (x, y)], 
	 [ tile square data for (x+1, y)], 
	 (...), 
	 [ tile square data for (x+w, y) ], 
	 [tile square data for (x, y+1), 
	 (...), 
	 [tile square data for (x+w, y+h) ] 
 ]
```
The array contains a set of arrays, with each internal array object containing data for the next coordinate in the rectangle.

For example, if x = 2, y = 2, w = 3, and h = 3, the array would represent coordinate data in this order: 
```
[ 
  [data for (2, 2) coord], 
  [data for (2, 3) coord], 
  [data for (2, 4) coord], 
  [data for (3, 2) coord], 
  [data for (3, 3) coord],
  [data for (3, 4) coord], 
  [data for (4, 2) coord], 
  [data for (4, 3) coord], 
  [data for (4, 4) coord],
]. 
```
Thus the array is a single dimensional array, that maps onto a 2D rectangle, from coordinate (2,2) at the top left, to coordinate (4,4) at the bottom right (inclusive).

Each tile square data (describing what is at a single `(x,y)` coordinate) object describes two things:
- Whether a player can enter the tile (the player can't enter a wall or river, for example)
- One or more "layers", from top to bottom, which indicate what image graphics to display on the tile

All image graphics in the game come from the RogueCloudResources project, under `com.roguecloud.resources.tiles`. They are numbered PNG files, with names like `202.png`. Each grid square layer contains a number (indicating which PNG file to display at that coordinate).

Layers are displayed on the screen from top to bottom. For example, the first layer in the array might be a player, and the second layer in the array might be some grass, which is underneath the player.

In addition to the png file #, a layer may also contain a rotation # (0 degrees [default], 90 degrees, 180 degress, or 270 degrees), which means the game engine will rotate the tile when it is displayed.

Thus, putting it all together, a tile square, containing n layers looks like this:
```
[
	(1 or 0 = 1 if the player can enter it, 0 otherwise - this is not a layer, just 'can i enter?' boolean), 
	layer 1,
	layer 2,
	(...),
	layer n
]
```

With each layer looking like this:
- Either a tile #, for example: `[ 1733 ]`  (this means show tile `1733.png` in the layer of this coordinate)
- or a tile # and a rotation: `[ 201, 90]`	(this means show tile `201.png`, rotated 90 degrees to the right, in the layer of this coordinate)

So here's a full JSON representation of a single tiles square's data:
```
[
  1,     <---- 1 means the player can enter
  [ 1733 ],     <---- layer 1, contains a number representing the player PNG file (1733.png)
  [ 201 ]     <--- layer 2 contains a number representing one of the grass PNG files (201.png)  
]
```

Putting it together even further, here is the above tile square data inside a frame:

This is a partial frame update where only a single tile changed:
```
    "frames": [
      {
        "x": 40, <--- The tile we are describing is at 40 + clientViewPosX
        "y": 20, <--- The tile we are describing is at 20 + clientViewPosY
        "w": 1,  <--- We are describing a grid rectangle of width 1
        "h": 1,  <--- We are describing a grid rectangle of height 1
        "data": [
          [
            1,   <--- player can enter
            [
              1733  <--- layer 1, as above (in this case, a player graphic)
            ],
            [
              201  <--- layer 2, as above (in this case, a grass graphic)
            ]
          ]
        ]
      }
    ],
```

In this example, the server is telling us that a tile at the position of (40, 20) relative to the clientView (with the absolute world coordinate listed in the example above) has changed.


### Visible objects and visible creatures

You may have noticed that my description of the frame data above does NOT reference which creatures or items are at a particular coordinate (only the PNG graphics that represent those images). The actual data for which monsters/players/items are at which coordinates are contained in the **visibleObjects** and **visibleCreatures** JSON fields. The contents of those fields is described by `JsonVisibleObject` and `JsonVisibleCreature` objects.

Here is an example of one of the creature objects inside the `visibleObjects` field:
```
  {
    "position": {      <--- absolute position of the creature in the world
      "x": 122,
      "y": 206
    },
    "creatureId": 60,  <--- a unique ID refering to only this creature
    "maxHp": 250,      <--- the maximum health the creature may have
    "currHp": 250,     <--- the current health of the player (0 hp == dead character)
    "level": 1,        <--- character level, representing monster difficulty (OTOH all players are always level 1)
    "weaponId": 20,    <--- a unique ID refering to the speecific weapon carried by the player
    "tileTypeNumber": 1733, <--- the PNG graphic used to represent the character
    "name": "jgwjgw",  <--- the name of the character (either a player name for users playing the game, or a monster name for non-players)
    "armourIds": [],   <--- unique IDs refering to the pieces of armour the player has equippred
    "effects": [],     <--- effects ids; these are from healing-over-time potions and a damage-reduction-over-time potions.
    "player": true     <--- whether this object described a human player (ie not a monster)
  }
```

When the game client receives this, it means the player can see a creature at world coordinate (122, 206), their HP, what weapons/armour they have, whether they are another player (in this case yes), the name of the player ("jgwjgw"), and so on.

You notice that the weapon ID is a number, rather than a JSON object describing the weapon. The weapon object can be found in the `weapons` JSON field, which will include a weapon with id 20. Weapons, armour, and items all have a unique ID which refers to a specific weapon/armor/item entity. 

### Items

Here is an example of a weapon object, from the `worldState.weapons` field:
```
  {
    "name": "Axe",
    "numAttackDice": 3,
    "attackDiceSize": 4,
    "attackPlus": 0,
    "hitRating": 20,
    "type": "One-handed",
    "id": 22,
    "tile": 2449
  }
```
The description of each of the fields above, and how they are used by the combat system, can be found in the Rogue Cloud documentation on Github, and in the `Combat.java` class.

Note that the above object does not have a world coordinate. This is because the world position of an object is contained in the `worldState.visibleObjects` field, like so:
```
      {
        "position": {
          "x": 326,
          "y": 76
        },
        "objectId": 49,
        "containedObjectId": 22
      },
```
The containedObjectId refers to the *weapon ID 22*, while the *objectId 49* is the id for a special type of object called a visible object (see `JsonVisibleObject`). A visible object is just an object with a position in the world. The reason that the weapon and visible object objects are kept separate is per the DRY principle mentioned above. This allows us to reduce server -> client bandwidth.

Armour and potions are handled in the same way as above. Armour objects can be found in `worldState.armours` and potions can be found in `worldState.drinkables`. The world coordinates for these objects can likewise be found in `worldState.visibleObjects`.


### Events

Events are things that happened between the previous frame and the current frame. Event objects are contained in the `events` field, and the event JSON is described in the `com.roguecloud.json.events.*` Java classes.
