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
./gradlew runData      # regenerates item models and block states into src/generated/resources
```

Rerun `runData` after registering new descriptors and commit `src/generated/resources`. Each descriptor item gets an
`item/generated` model from its 1.7.10 icon; the voltage level background is a second layer shown only in inventories
(Forge `separate_transforms`), as the 1.7.10 item renderer did.

Textures, models and sounds are not copied: the build packs `../src/main/resources/assets` with every path lowercased
and characters outside `[a-z0-9/._-]` replaced by `_`, which 1.20.1 resource locations require. Code that builds a
path from a legacy name goes through `Utils.resourcePath` / `Utils.resourceLocation`, which apply the same rule.
`assets/minecraft/atlases/blocks.json` adds the legacy `blocks/`, `items/` and `voltages/` texture folders to the
block atlas so item models can use them.

Plain JUnit tests cannot bootstrap Minecraft on Forge (its event bus needs the mod launcher), so anything touching
registries, levels or blocks (for example `Coordinate`, which stores a dimension key) is tested with GameTests.
GameTests use the empty `data/eln/structures/empty3x3x3.nbt` template.

## Conventions

* Code is copied from the root tree first, then changed only as much as 1.20.1 needs, so diffs against the
  1.7.10 file stay readable. New code is Kotlin.
* `mods.eln.Eln` holds the global state that 1.7.10 kept as static fields on the `Eln` class. The Forge entry
  point is `mods.eln.ElnMod`, so unit tests can use `Eln` without starting Forge.
* GUIs keep the 1.7.10 `mods.eln.gui` classes. `GuiScreenEln`/`GuiContainerEln` publish the frame's `GuiGraphics` in
  `GuiDraw`, and the widgets draw through it (`Gui` is a stand-in for the 1.7.10 base class). Node GUIs open through
  one menu type, `eln:node` (`ElnMenus`): the server builds the element's container, the client builds the render's
  GUI around its own copy, and `BasicContainer.withContainerId` hands both the window id. A node without a container
  gets the 1.7.10 `packetOpenLocalGui` instead. An access transformer makes `Slot.x/y` writable because Eln GUIs place
  the player inventory after the container is built.
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
5. [x] Vertical slice: cable, battery and lamp placed in a world with models, GUI and sync.
   * [x] Six-node framework and the classic cables: placeable in game, connected rendering, item icons, creative
     tab, simulation (GameTest feeds 10 V through two placed cables), drop when the support block goes, save/load.
   * [x] GUI layer, Electrical Source, lamp sockets (all but the streetlight and emergency lamps), light bulbs and the
     light block. A GameTest lights a 12 V lamp from a placed source through a cable; in game the source voltage is
     set in its GUI and the socket takes its bulb and cable through its GUI.
   * [x] Transparent-node framework (full-block machines), the seven batteries with their GUI, and the creative
     Ground Cable. Batteries float as in 1.7.10, so a lamp powered from one needs the negative terminal grounded; a
     GameTest and an in-game check light a 12 V lamp that way. Hopper/pipe access to machine inventories, custom
     collision boxes, multiblock ghost blocks and the fluid block entity are not ported yet.
6. [ ] Six-node blocks, transparent-node machines and the remaining content, family by family.
   * [x] Meters, wrench, config copy tool, brushes and the basic material items (ingots, dusts, plates, chips).
   * [x] Switches, relays, signal sources, wireless signals, passive components (resistors, diodes, capacitors,
     inductors) and lamp supplies; lamp sockets default to "Powered by lamp supply" again.
   * [x] Utility cables (copper/aluminium, single and multi-conductor, with length-tracked spools), current cables,
     current relays and molten metal piles. As in 1.7.10 the classic voltage cables are hidden from the creative tab
     but stay loadable. A GameTest lights a lamp through a utility cable. Wire scrap and ghost blocks are not ported.
   * [x] Signal processing: voltage/current and thermal sensors, VU meters, alarms, light/weather/humidity/wind/
     temperature/proximity sensors, fire detectors, the scanner (reads tanks and inventories through Forge
     capabilities), redstone input/output, timer, math chip (signal equation), watches, digital display, tutorial
     sign, logic gates, analog chips and regulator chips; plus the thermal cable and the data loggers. A GameTest
     checks NOT and AND gate outputs. MQTT and Modbus controllers, the wiki operator page and the wind state save
     (wind restarts on each server start) are not ported.
   * [x] Power six-nodes: power sockets, breakers, fuse holders and fuses, energy meters, the battery charger (with
     the machine booster), the hub, the current source, conduit and the tree resin collector (finds trunks and
     leaves through the `logs`/`leaves` block tags). A GameTest switches a lamp through a breaker. The MQTT energy
     meter and the breaker's current-limit overlay on its item icon are not ported.
   * [x] Multiblock ghost blocks (`mods.eln.ghost`): an invisible `eln:ghost` block whose `type` state replaces the
     1.7.10 cube/floor/ladder metadata, saved with the node data per dimension. A GameTest places a 2x3 solar panel
     and breaks one of its ghost blocks. Cube ghosts are full blocks; 1.7.10 asked the machine for its collision boxes.
   * [x] Power capacitors and inductors, DC-DC converters (fixed, variable, one-way, legacy), solar panels and the
     solar tracker, wind and water turbines, the heat furnace, the heat turbines and thermal dissipators. The fuel
     generator and fuel heat furnace wait for fluid handling on nodes; the shaft network (steam/gas turbines,
     generators, motors, flywheels) is next.
7. [ ] Recipes, world generation, language files, jar-in-jar for libraries, first release.

## References

* [Electrical Age: Re-Wired](https://github.com/brambora69123/electrical-age-rewired) is a 1.12.2 port based on
  Eln 1.14.2. Its block state and model JSON and its Forge capability integration are useful references; it still
  uses metadata items and GL11 rendering, which 1.20.1 removes.
