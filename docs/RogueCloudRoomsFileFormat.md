## File format of the Rogue Cloud `rooms.txt` file

Note:
- A "room" in the rooms.txt does not necessarily need to be a room (it is a bit of a misnomer) but rather, it corresponds to a cookie cutter structure (a grid of tiles) that can be stamped on the map at any map coordinate.
- The `rooms.txt` file is in `(path to git repository root)\RogueCloudServer\WebContent\universe\rooms.txt`

Each room in the `rooms.txt` file begins with this:
```
(Room name - this is the same as the room name in map-new-mappings.txt):
```


### Letter assignments:

#### 1) `* = # Name`

* *Example*: `F = 1152 Fridge`

Where:
* `*` = A single letter or symbol (example: a, b, 0, C, ., etc)
* `#` = The tile number to display at this location (eg there should exist a file #.png, where # is the number)
Name = This name will be displayed when the user mouses over the tile in the game UI (but otherwise is not used)

#### 2) `* = #, %`

* *Example*: `B = 1141, 90 Table 2`

Where:
* `%` = A number indicating the number of degrees to rotate the tile when it is displayed: 0, 90, 180, 270 (0 is the default)
* `*, #, Name` = Same as above

#### 3) `* = # / # / #  Name`

* *Example*: `r = 105	/ 122	@Passable`

`#` as above, but in this case the tile will have multiple layers (separated by `/`), with the leftmost number corresponding to the image displayed on the top layer.

#### 4) `* = #, % / #, %	Name`

* *Example*: `E = 1967, 90 / 105, 180 	Fence`

You can combine tile number and rotation into a single layer, and still have multiple layers. In the above example there are multiple layers.

#### 5) Annotations: `@Bg` 	`@Passable` `@Door`

`@Passable` - All tiles in the room are impassable by default; this means that monsters and creatures can't walk on them. This is true for walls and furniture, but shouldn't be true for floor tiles. For this reason, floor tiles (or other tiles that can be walked on) should be annotated with `@Passable`.

`@Door` - Add this to doors tiles.

`@Bg` - Indicates that this tile replaces the background if a creature steps on it. Add this only if a tile is disappearing when a creature walks on it.

####6) Special letters: `*`

The `*` letter is used to set the default background for a room. If the room text contains a space (eg " "), then it will have the background listed at the `*` character.

#### 7) Special tile numbers: `-1`

The `-1` tile number is used to indicate transparency. 


### Room tile layout

Each letter corresponds to a letter specified above. Each space on the grid is a tile in the world. 

Note that in most text editors, characters are taller then they are wide. So, for example, the following room looks like it is tall and thin, however in reality when displayed it will be nearly square (20x21, w x h).

```
{ 
####################
#  FF  V  P        #
#                  #
#              CCCCO
#    AB     M  CCCC#
#    GD        CCC #
O              CCC #
#           CCCCCC #
#______.-----O-----#
# I    |    CCC    #
#      |    CCC    #
#---O---    CCC    #
#W     |    CCC    #
#      O    CCC    #
#___O__|    CCC    #
#      |_______O___#
#      |           #
#     T|           #
#      |          T#
# E    |S    E     #
####################
}
```
