package mods.eln.datagen

import net.neoforged.neoforge.registries.NeoForgeRegistries
import mods.eln.Eln
import mods.eln.block.ArcBlock
import mods.eln.ore.ConfigEnabledPlacement
import mods.eln.ore.ElnOre
import net.minecraft.core.HolderLookup
import net.minecraft.core.HolderSet
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.loot.BlockLootSubProvider
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BiomeTags
import net.minecraft.tags.BlockTags
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.VerticalAnchor
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
import net.minecraft.world.level.levelgen.placement.BiomeFilter
import net.minecraft.world.level.levelgen.placement.CountPlacement
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement
import net.minecraft.world.level.levelgen.placement.InSquarePlacement
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider
import net.neoforged.neoforge.common.world.BiomeModifier
import net.neoforged.neoforge.common.world.BiomeModifiers
import java.util.concurrent.CompletableFuture

/**
 * Ore world generation as data: one configured and one placed feature per ore, added to every overworld biome by a
 * Forge biome modifier. Replaces the 1.7.10 IWorldGenerator, which also skipped superflat worlds; superflat worlds do
 * not run ore features in 1.20.1 either.
 */
class ElnWorldGen(output: PackOutput, lookup: CompletableFuture<HolderLookup.Provider>) :
    DatapackBuiltinEntriesProvider(output, lookup, BUILDER, setOf(Eln.MODID)) {

    companion object {
        private val BUILDER: RegistrySetBuilder = RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, ::configured)
            .add(Registries.PLACED_FEATURE, ::placed)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ::biomeModifiers)

        private fun configuredKey(ore: ElnOre) = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Eln.MODID, ore.id))
        private fun placedKey(ore: ElnOre) = ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Eln.MODID, ore.id))

        private fun configured(context: BootstrapContext<ConfiguredFeature<*, *>>) {
            val stone = TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES)
            for (ore in ElnOre.values()) {
                // shortcut: one vein size, the middle of the 1.7.10 random range; a custom feature could randomise it.
                val size = (ore.sizeMin + ore.sizeMax) / 2
                context.register(configuredKey(ore), ConfiguredFeature(Feature.ORE, OreConfiguration(stone, ore.block.get().defaultBlockState(), size)))
            }
        }

        private fun placed(context: BootstrapContext<PlacedFeature>) {
            val configured = context.lookup(Registries.CONFIGURED_FEATURE)
            for (ore in ElnOre.values()) {
                context.register(
                    placedKey(ore), PlacedFeature(
                        configured.getOrThrow(configuredKey(ore)), listOf(
                            ConfigEnabledPlacement(ore.configPath, ore.enabledByDefault),
                            CountPlacement.of(ore.veinsPerChunk),
                            InSquarePlacement.spread(),
                            HeightRangePlacement.uniform(VerticalAnchor.absolute(ore.minY), VerticalAnchor.absolute(ore.maxY)),
                            BiomeFilter.biome()
                        )
                    )
                )
            }
        }

        private fun biomeModifiers(context: BootstrapContext<BiomeModifier>) {
            val placed = context.lookup(Registries.PLACED_FEATURE)
            val biomes = context.lookup(Registries.BIOME)
            context.register(
                ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(Eln.MODID, "ores")),
                BiomeModifiers.AddFeaturesBiomeModifier(
                    biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                    HolderSet.direct(ElnOre.values().map { placed.getOrThrow(placedKey(it)) }),
                    GenerationStep.Decoration.UNDERGROUND_ORES
                )
            )
        }
    }
}

/** Ore and arc blocks drop themselves, as in 1.7.10 (smelting or macerating ores is the processing step). */
class ElnBlockLoot(registries: HolderLookup.Provider) : BlockLootSubProvider(emptySet(), FeatureFlags.REGISTRY.allFlags(), registries) {
    private val blocks get() = ElnOre.values().map { it.block.get() } + ArcBlock.values().map { it.block.get() }

    override fun generate() {
        for (block in blocks) dropSelf(block)
    }

    override fun getKnownBlocks(): Iterable<Block> = blocks
}

fun elnLootTables(output: PackOutput, registries: CompletableFuture<HolderLookup.Provider>) =
    LootTableProvider(output, emptySet(), listOf(LootTableProvider.SubProviderEntry(::ElnBlockLoot, LootContextParamSets.BLOCK)), registries)
