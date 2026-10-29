# Ice and Fire Fabric

An unofficial port of [Ice and Fire](https://github.com/AlexModGuy/Ice_and_Fire) to **Fabric** for **Minecraft 26.3**,
with new content of its own. *Port no oficial de Ice and Fire a Fabric para Minecraft 26.3, en 16 idiomas.*

Dragons of fire, ice and lightning that grow from eggs to giants, and the original's mythical creatures and places. A
new adventure on top: an exiled pixie who joins you after your first tree, fourteen of her memories to give back, and
Cryona's duel on Black Frost at the end.

[![Ice and Fire Fabric: the video](https://img.youtube.com/vi/JW4DMXVWWH4/maxresdefault.jpg)](https://youtu.be/JW4DMXVWWH4)

> This port is not made or supported by the original mod's authors. Report its problems here, not to the original mod.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 25

The same mod for Minecraft 1.21.1 and 1.20.1 is in the branches
[`1.21.1`](https://github.com/britoandaur2413-afk/IceAndFire-Fabric/tree/1.21.1) and
[`1.20.1`](https://github.com/britoandaur2413-afk/IceAndFire-Fabric/tree/1.20.1).

## What it has

- **Dragons:** fire, ice and lightning dragons in five stages, from eggs to giants. Young ones nest on high ground,
  groups live in caves, the oldest rule great halls with a forge, an arena and a hoard. Tame, ride, breed and arm them,
  forge dragonsteel.
- **Creatures:** hippogryphs, hippocampi, pixies (bonds with levels, gifts and magic), cyclopes, sirens, gorgons,
  hydras, trolls, myrmex colonies, death worms, cockatrices, stymphalian birds, amphitheres, sea serpents, ghosts and
  the dread army.
- **Places:** pillager kingdoms with dragon riders, dread spires, pixie sanctuaries and caverns, witches' cottages,
  hydra caves, graveyards, mausoleums and the gorgon's temple.
- **The exiled pixie:** a companion who talks about what is really around you and guides you with her lantern and a
  beam of light. Each of her fourteen memories is an adventure in a real place of the world.
- **Cryona's citadel:** a duel in two parts. Black Frost with its pylons, dives, breath and claws, then Cryona alone:
  her mourning with the dead rising, her vengeance and her ring of ice. Each attack is shown on the ground first, a fall
  costs nothing (your things wait in her realm and the duel goes on where it stopped), and it has music of its own.
- The bestiary, the original's title screen, and every text in 16 languages.

## Keys

| Key | What it does |
|---|---|
| J | The exiled pixie's menu |
| K | Her memories |
| H | Pixie bonds |
| V | Pixie magic |
| R / G / X | Dragon breath / bite / fly down (riding) |
| F7 | Dragon riding view |

## Config and commands

- `config/iceandfire_fabric.json`: spawns, griefing, the exiled pixie (`exiledPixie`), the title screen
  (`customMainMenu`) and more.
- For game masters: `/iceandfire find <place>` (nest, cave, hall, kingdom, sanctuary, cavern, witches, hydra, spire,
  throne, mausoleum, graveyard, gorgon_temple) and `/iceandfire fairy reset|meet|complete`.

## Building

```
./gradlew build
```

The jar is `build/libs/IceAndFire-Fabric-26.3-<version>.jar`. `./gradlew runGameTest` runs the game tests, and
`./gradlew runClientGameTest` runs the client scenes, which take screenshots (`IAF_ONLY=menu,boss,...` runs some of them).

## Credits

- **Alexthe666**: Ice and Fire, the original mod (LGPL-3.0).
- **Alexthe666**: Citadel (LGPL-3.0), the library the original mod is built on; the parts this port needs are
  included, adapted to Fabric.
- **Raptorfarian**: the original mod's art.
- **TheBv**: the original mod's ports to newer versions, which this port follows.
- **The original mod's contributors and translators.**
- **Escobar**: the Fabric port and its new content (the exiled pixie, the citadel, Cryona's duel and their music).
- The new voices and sound effects were made with ElevenLabs.

## License

LGPL-3.0-only, as the original mod. See [LICENSE](LICENSE); the LGPL-3.0 adds permissions to the GPL-3.0, whose text
is in [COPYING](COPYING).

This is a modified version of Ice and Fire: ported from Forge to Fabric and changed by Escobar (GAMITA_MODELS) since
October 2026. The source code of the latest version is here (`main` for 26.3, the branches `1.21.1` and `1.20.1`);
the source code of an older published version is given to anyone who asks for it.
