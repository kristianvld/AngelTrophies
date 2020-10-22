# AngelTrophies
[Angel's Reach](https://angels-reach.com/) skinable items and placable trophy system, utilising [Oraxen](https://github.com/oraxen/Oraxen).

[![Latest Release](https://img.shields.io/github/v/release/kristianvld/AngelTrophies?include_prereleases&label=Latest%20Release)](https://github.com/kristianvld/AngelTrophies/releases)

# Features
* Sticker system to skin items
  * Supports multi-layer skinning
  * Saves skins on player death
* Placeable trophy system
  * Place and pickup trophies
  * Place on both walls and ground
  * Enable "chair" mode to mound and dismount trophies as chairs

# Building
This plugin depends on [Oraxen](https://github.com/oraxen/Oraxen). To be able to build this project, place a compiled version of the plugin under `build/oraxen`, e.g. `build/oraxen/oraxen-1.58.0.jar`.
After adding the Oraxen dependency, simply build with gradle, as:
```bash
./gradlew build
```
You can find the jar file artifact under `build/libs/AngelTrophies.jar`.
On Windows, replace `gralde` with `./gradlew.bat`.

# Usage
First off, everything is configured inside the Oraxen item files (`plugins/Oraxen/items/*.yml`).

## Skins:
Skin items are items that you apply to other items to change their apperance or skin. You can have a many-to-many mapping, specifying what each item should be skinned to when this skin is applied.

**Notice: Source and target items MUST have the same material id and provide different model ids!**

**Notice 2: Only the item name and model id is updated when applying skins. Variables like amount or item damage is kept to the original item.**

Example:
```YAML:
emerald_skin:
  displayname: '&2Emerald Skin'
  material: EMERALD
  skin:
    minecraft_diamond_sword: emerald_sword
    minecraft_diamond_pickaxe: emerald_pickaxe
    minecraft_diamond_axe: emerald_axe
    minecraft_diamond_shovel: emerald_shovel
    minecraft_diamond_hoe: emerald_hoe
```
This is a new emerald item, which can be applied to any minecraft diamond tool, and when applied would skin it to an emerald tool.  As you can see, a "sticker" can be applied to multiple items, and are consumed when applied.

The source can either be:
* `minecraft_<minecraft name>`, matching a specific material
* `minecraft_<minecraft name>_<model id>`, matching a specific material and specific model id
* `oraxen_id`, matching another Oraxen item

The target has to be another Oraxen item. In the example above, it is assumed that the `emerald_sword`, `emerald_pickaxe` etc are all defined previous as other Oraxen items.

As Oraxen items can be the source items, stickers can be chained. Imagine that you have another sword called `obsidian_sword`, then you could have another sticker apply to the emerald sword from above like so:
```YAML
obsidian_sticker:
  displayname: '&bObsidian Sticker'
  material: DIAMOND
  skin:
    emerald_sword: obsidian_sword
```

If the `emerald_sword` was a diamond sword with the model id of 1, then the above config would be equivalent to this:
```YAML
obsidian_sticker:
  displayname: '&bObsidian Sticker'
  material: DIAMOND
  skin:
    minecraft_diamond_sword_1: obsidian_sword
```

The full format is as follows:
```YAML
new_oraxen_item_name:
  [oraxen options...]
  skin:
    minecraft_id: oraxenitem
    minecraft_id_modelData: oraxenitem
    oraxenitem: oraxenitem
```

### Commands:
In game, there are 3 commands:
* `/skin merge`
* `/skin split`
* `/skin lost`

`merge` merges an item and a sticker. You need to hold the sticker in your off hand and the item in your main hand. E.g. hold the emerald sticker in your off hand and a diamond sword in your main hand, run `/skin merge` to get an emerald sword. This would consume 1 sticker from your off hand.

`split` works in reverse. Hold the skinned item in your main hand and have your off hand clear, then run split and you would have back your diamond sword and the emerald sticker.

As you should also have noticed above with the obsidian sword example, skins are stackable, so you can apply an emerald sticker to a diamond sword and then an obsidian sticker to get an obsidian sword. Likewise if you split the obsidian sword you get the obsidian sticker and the an emerald sword back, which can then be split into a diamond sword and diamond sticker.

### Notes to keep in mind:
You can only split items which have been skinned with the `/skin merge` command. If you give yourself an obsidian sword with the `/oraxen inventory` command or similar, you can not split that item, as it was never skinned.

Also note that all the plugin does is change the display name and model id, meaning items that are going to be skinned needs to be the same type. They are also given the internal Oraxen id, meaning if you have mechanics applied to a skinned item, then those should still work (mostly untested). Also note that other options are not transfered, like if you have specified item amonts/damage or other variables to either the source or target skinned item.

When you die, the plugin removes all applied skins from your items, and also any stickers you have in your inventory. You can get those items back with `/skin lost`. The plugin will split any skinned items until no skins are applied to that item any more and only the stickers are given back to you when using `/skin lost`. The actual item itself, like the bare diamond sword will still be dropped.

You will also be notified any time you respawn or join the server if you have any lost items. If you do not have room in your inventory to get all the items back, the plugin will still keep the "lost" items that it was unable to give to you, until you clear more space. If you die while already having lost items and lose more skins, then those will be added to the tally. Lost stickers are stored in `AngelTrophies/LostSkins.txt` and will be kept as much up-to-date as possible while the server run, meaning nothing should be lost even if the server crashes without shutting down properly.

## Trophies:
Trophies are also applied in the items with the format and default values:
```YAML
item_name:
  [oraxen options...]
  trophies:
    wall:
      small: false             # small or big armor stand
      offset: 0.0              # offset towards the wall. 0 means the center of the block. 0.5 would touching the next block in the direction the item is facing
    floor:
      small: false             # small or big armor stand
      offset: 0.0              # offset from the ground. 0 means at the bottom of the block. 0.5 would be 0.5 of the ground. -1 would be the block bellow.
      place_slab: false        # if a slab should be placed when placing this trophy. This also makes the trophy mountable by right clicking.
      rotation_resolution: 45  # Statues on the ground will be facing towards you. This is the amount of resolution the statues will snap to. 90 would be facing along the lines north, east, south and west. 45 would be facing north, north-east, east, south-east, south, south-west, west and north-west.
```       

Here is an example of a heart trophy that could be placed on walls:
```YAML
yellow_heart:
  displayname: '&eYellow Heart'
  material: PAPER
  Pack:
    generate_model: false
    model: item/trophies/hearts/yellow_heart.json
    custom_model_data: 1
  trophies:
    wall:
      small: true
      offset: 0.4
```

In this example, you can place the obsidian sword into the ground as a trophy:
```YAML
obsidian_sword:
  displayname: '&bObsidian'
  material: DIAMOND_SWORD
  Pack:
    generate_model: false
    model: item/skins/obsidian_sword.json
    custom_model_data: 2
  trophies:
    floor:
      small: false
      offset: 0
```

Or here, you have a chair trophy that when you right click you can mount and sit on it. You can also mount it by sneaking while on top of it:
```YAML
chair:
  material: PAPER
  displayname: "&4Chair"
  Pack:
    generate_model: false
    model: item/trophies/chair.json
    custom_model_data: 2
  trophies:
    floor:
      small: false
      offset: 0
      place_slab: true
      rotation_resolution: 90
```
To place or pickup a trophy, just shift right click. To mount a mountable trophy with a slab, just normal right click or sneak on top of the trophy. Currently trophies are placeable and pickup-able by any player, but should follow other protection plugins. 

Like if you do not have permission to interact with a block in a certain chunk, you should not be able to place or pickup trophies. This has not been tested with other protection plugins.
