# 1.20.1 forward port

The Forge 1.20.1 port lives in [`port-1.20.1/`](../port-1.20.1), a standalone Gradle project next to the
1.7.10 build. The 1.7.10 code at the repository root stays untouched and is the reference for every ported feature.

* Minecraft 1.20.1, Forge 47.4.x, Kotlin for Forge 4.12 (Kotlin 2.2), Java 17, official Mojang mappings.
* Mod id is `eln` (1.20.1 requires lowercase ids), asset namespace stays `eln`.
* Package names stay `mods.eln.*` so ported code keeps its imports.
* 1.7.10 worlds cannot be converted; the port starts with fresh worlds.

## Building

```
cd port-1.20.1
./gradlew build        # compile + unit tests
./gradlew runServer    # headless dedicated server (accept the EULA in run/eula.txt first)
./gradlew runClient
./gradlew runGameTestServer   # runs the GameTests in mods.eln.gametest on a headless server, fails the build on failure
```

Plain JUnit tests cannot bootstrap Minecraft on Forge (its event bus needs the mod launcher), so anything touching
registries, levels or blocks (for example `Coordinate`, which stores a dimension key) is tested with GameTests.
GameTests use the empty `data/eln/structures/empty3x3x3.nbt` template.

## Conventions

* Code is copied from the root tree first, then changed only as much as 1.20.1 needs, so diffs against the
  1.7.10 file stay readable. New code is Kotlin.
* `mods.eln.Eln` holds the global state that 1.7.10 kept as static fields on the `Eln` class. The Forge entry
  point is `mods.eln.ElnMod`, so unit tests can use `Eln` without starting Forge.
* Anything left out because its dependencies are not ported yet is marked with a `// PORT:` comment saying what
  brings it back. `grep -rn "PORT:" port-1.20.1/src` lists the open items.

## Milestones

1. [x] Empty mod builds and loads on a 1.20.1 dedicated server.
2. [x] MNA circuit solver (`sim/mna`) ported; its unit tests pass.
3. [x] Rest of the simulation core: thermal loads, processes, watchdogs, `FunctionTable`, the `Simulator` tick
   (server lifecycle and `ServerTickEvent` wired in `ElnMod`). Still waiting on later layers: the signal gate family
   (`NbtElectricalGate*`, `SignalLoadSupport`, needs the signal cable), regulators, `ResistorProcess`, stack machines,
   `NbtBatterySlowProcess`, `MonsterPopFreeProcess`, `CableMelt`, `WorldExplosion`, `ShaftSpeedWatchdog`, and
   `RoomThermalManager` room heat exchange.
4. [x] Node framework: `NodeBase`, `NodeManager`, node connections, per-dimension save files
   (`<dimension folder>/data/electricalAgeWorld.dat` with `.tmp`/`.bak` rotation), `NodeServer` publishing, and the
   network channel that carries Eln's byte-stream packets. GameTests cover connecting two nodes through the simulator
   and a save/load round trip. `NodeBlock`/`NodeBlockEntity` are ported (light level is a block state property) but
   first run once a concrete node block is registered in milestone 5. Client-to-server node packets are now rejected
   unless the player is in the node's dimension and within 64 blocks.
5. [ ] Vertical slice: cable, battery and lamp placed in a world with models, GUI and sync.
6. [ ] Six-node blocks, transparent-node machines and the remaining content, family by family.
7. [ ] Recipes, world generation, language files, jar-in-jar for libraries, first release.

## References

* [Electrical Age: Re-Wired](https://github.com/brambora69123/electrical-age-rewired) is a 1.12.2 port based on
  Eln 1.14.2. Its block state and model JSON and its Forge capability integration are useful references; it still
  uses metadata items and GL11 rendering, which 1.20.1 removes.
