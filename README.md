# AngelTrophies
[Angel's Reach](https://angels-reach.com/) skinable items and placable trophy system, utilising [Oraxen](https://github.com/oraxen/Oraxen).

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
