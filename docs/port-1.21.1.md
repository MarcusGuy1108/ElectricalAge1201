# NeoForge 1.21.1 port

The NeoForge 1.21.1 port lives in [`port-1.21.1/`](../port-1.21.1). It started as a copy of
[`port-1.20.1/`](port-1.20.1.md) with every API change applied, so the layout, package names, asset pipeline,
lang conversion and GameTests described there apply here too. This page only lists what differs.

* Minecraft 1.21.1, NeoForge 21.1.219, ModDevGradle 2.0, Kotlin for Forge (NeoForge) 5.10, Java 21.
* Same mod id (`eln`) and the same item and block ids as the 1.20.1 port. Worlds do not move between the two
  versions (Minecraft itself changed the item stack format).

## Two source copies

`port-1.20.1/src` and `port-1.21.1/src` are separate copies. A fix or feature in one has to be applied to the other
by hand. Most files are identical; the differences are the API migrations below. `diff -r` between the two `src`
trees shows exactly where they diverge.

## Building

```
cd port-1.21.1
./gradlew build               # compile + unit tests; release jar: build/libs/eln-<version>.jar
./gradlew runClient
./gradlew runServer
./gradlew runGameTestServer   # same GameTests as 1.20.1
./gradlew runData             # regenerates src/generated/resources
```

ModDevGradle has no reobfuscation step, so there is a single jar, and it already carries Apache Commons Numbers
through jar-in-jar. Players need Kotlin for Forge 5.10 or newer for NeoForge: install the `kotlinforforge-<v>-all.jar`
release from Modrinth or CurseForge. The `kotlinforforge-neoforge` Maven artifact only bundles the Kotlin libraries,
not the language loader, so the game refuses to load Eln with it. The jar has been checked on a NeoForge 21.1.219
dedicated server installed with the official installer (`--install-server`): it loads and generates a world with no
errors.

Jade (15.10) and JEI (19.28) are compile-time only and run in development, as on 1.20.1. JEI 19.70 and later need
NeoForge 21.1.238 or newer.

## API migrations applied

These are recorded so that porting a later 1.20.1 change goes faster.

* Packages: `net.minecraftforge.*` became `net.neoforged.neoforge.*` (`fml` is `net.neoforged.fml`, the event bus is
  `net.neoforged.bus.api`); `MinecraftForge.EVENT_BUS` became `NeoForge.EVENT_BUS`; `IForgeMenuType` became
  `IMenuTypeExtension`. `MOD_BUS` comes from `thedarkcolour.kotlinforforge.neoforge.forge`.
* Registries: `DeferredRegister.create(Registries.X, MODID)`. `RegistryObject<T>` is a type alias for
  `DeferredHolder<*, T>` in `ElnRegistries`, so registration code stays the same. Register lambdas are written as
  `{ _: ResourceLocation -> ... }` because the bare lambda is ambiguous between the Supplier and Function overloads.
  `ForgeRegistries` became `BuiltInRegistries`. `ResourceLocation(ns, path)` became `fromNamespaceAndPath`/`parse`.
* Item NBT: item stacks have data components instead of a tag. `LegacyCompat.kt` gives `ItemStack.tag` (a copy of
  `CUSTOM_DATA`) and `editTag {}` to write; descriptors use `editNbt(stack) {}`. Saving and loading stacks goes
  through `registries()`, `itemStackOf(tag)` and `ItemStack.save(tag)` in the same file.
* Ticks: `ServerTickEvent.Pre/Post`, `ClientTickEvent.Pre/Post`, `RenderFrameEvent.Pre`.
* Networking: `ElnNetwork` sends a `RawPacket` custom payload both ways (`playBidirectional`,
  `PacketDistributor.sendToPlayer/sendToServer`); the packet bodies are unchanged.
* Capabilities: registered in `RegisterCapabilitiesEvent`, read with `level.getCapability(Capabilities.X.BLOCK, ...)`.
  `TransparentNodeEntity` calls `invalidateCapabilities` once its element is set.
* Blocks: every block has a `codec()`. `use` is split into `useItemOn` and `useWithoutItem`; both call the old
  `activate`. Render bounding boxes moved from the block entity to the renderer.
* Rendering: `VertexConsumer` uses `addVertex/setColor/setUv/setOverlay/setLight/setNormal`. Screens register in
  `RegisterMenuScreensEvent`. `Screen.render` already draws the background (and the blur may run only once a frame), so
  `GuiHelper` no longer calls `renderBackground`.
* Items: armor materials are registry entries (`ElnRegistries.ARMOR_MATERIALS`); tools take
  `Properties().attributes(...)`. E-coal armor uses `LivingIncomingDamageEvent`.
* Data: datagen recipes use `RecipeOutput`; loot and worldgen providers take the registry lookup. Common tags moved
  from `forge:` to `c:`. Data folders are singular (`structure`, `recipe`, `loot_table`, `advancement`), pack format 34.
* GameTests: 1.21 surrounds each test structure with barrier blocks; tests that build outside their structure call
  `GameTestSpace.clearRoom` first. Unit tests see Minecraft through `neoForge.addModdingDependenciesTo(sourceSets.test)`.
