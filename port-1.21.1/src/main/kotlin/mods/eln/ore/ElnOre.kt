package mods.eln.ore

import com.mojang.serialization.MapCodec
import net.minecraft.resources.ResourceLocation
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mods.eln.Eln
import mods.eln.generic.CreativeTabPopulator
import mods.eln.registry.ElnRegistries
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.util.RandomSource
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.levelgen.placement.PlacementContext
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import net.minecraft.world.level.levelgen.placement.PlacementModifierType
import net.minecraft.world.level.material.MapColor
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import mods.eln.registry.RegistryObject
import java.util.stream.Stream

/**
 * The Electrical Age ores. 1.7.10 kept them as metadata of one ore block and generated them from code; here each ore
 * is its own block, and its world generation is data (see ElnWorldGen) built from these numbers. Veins per chunk,
 * vein size and height range are the 1.7.10 values, the heights as absolute y.
 */
enum class ElnOre(
    val legacyName: String,
    val texture: String,
    val configPath: String,
    val enabledByDefault: Boolean,
    val veinsPerChunk: Int,
    val sizeMin: Int,
    val sizeMax: Int,
    val minY: Int,
    val maxY: Int
) {
    COPPER("Copper Ore", "copperore", "worldgen.ores.copper.enabled", true, 30, 6, 10, 0, 80),
    LEAD("Lead Ore", "leadore", "worldgen.ores.lead.enabled", true, 8, 3, 9, 0, 24),
    TUNGSTEN("Tungsten Ore", "tungstenore", "worldgen.ores.tungsten.enabled", true, 6, 3, 9, 0, 32),
    CINNABAR("Cinnabar Ore", "cinnabarore", "runtime.worldgen.ores.cinnabar.enabled", false, 3, 3, 9, 0, 32);

    /** Registry path, e.g. eln:copper_ore. */
    val id: String = name.lowercase() + "_ore"

    /** c:ores/<this> holds the ore, as the 1.7.10 ore dictionary held it under ore<Name>. */
    val material: String = name.lowercase()

    lateinit var block: RegistryObject<Block>
        private set

    companion object {
        private val PLACEMENT_MODIFIERS: DeferredRegister<PlacementModifierType<*>> =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Eln.MODID)

        @JvmField
        val CONFIG_ENABLED: RegistryObject<PlacementModifierType<ConfigEnabledPlacement>> =
            PLACEMENT_MODIFIERS.register("config_enabled") { _: ResourceLocation -> PlacementModifierType { ConfigEnabledPlacement.CODEC } }

        fun register(modBus: IEventBus) {
            for (ore in values()) {
                ore.block = ElnRegistries.BLOCKS.register(ore.id) { _: ResourceLocation -> OreBlock(ore) }
                val item = ElnRegistries.ITEMS.register(ore.id) { _: ResourceLocation -> BlockItem(ore.block.get(), Item.Properties()) }
                CreativeTabPopulator.addStack({ Eln.creativeTabOresMaterials }) { ItemStack(item.get()) }
            }
            PLACEMENT_MODIFIERS.register(modBus)
        }
    }
}

class OreBlock(val ore: ElnOre) :
    Block(Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0f, 5.0f)) {
    // The 1.7.10 language files name the ores "Copper_Ore.name" and so on.
    override fun getDescriptionId(): String = ore.legacyName.replace(' ', '_') + ".name"
}

/** Drops an ore's placements while its 1.7.10 config switch is off, so the switch still works with data world gen. */
class ConfigEnabledPlacement(val path: String, val enabledByDefault: Boolean) : PlacementModifier() {
    override fun getPositions(context: PlacementContext, random: RandomSource, pos: BlockPos): Stream<BlockPos> {
        return if (Eln.config.getBooleanOrElse(path, enabledByDefault)) Stream.of(pos) else Stream.empty()
    }

    override fun type(): PlacementModifierType<*> = ElnOre.CONFIG_ENABLED.get()

    companion object {
        val CODEC: MapCodec<ConfigEnabledPlacement> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING.fieldOf("config").forGetter(ConfigEnabledPlacement::path),
                Codec.BOOL.fieldOf("default").forGetter(ConfigEnabledPlacement::enabledByDefault)
            ).apply(instance, ::ConfigEnabledPlacement)
        }
    }
}
