package mods.eln.ore

import mods.eln.Eln
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.Tags
import java.util.IdentityHashMap

/**
 * How brightly the X-ray scanner shows a block: `tools.xrayScanner.oreFactors`, then, when auto-discovery is on,
 * anything in `forge:ores` (1.7.10 used the ore dictionary). Looked up per block on the render thread.
 */
object OreScanner {
    private val factors = IdentityHashMap<Block, Float>()
    private val configured: Map<Block, Float> by lazy(::loadConfigured)
    private val configuredTags: Map<TagKey<Block>, Float> by lazy(::loadConfiguredTags)

    fun factor(state: BlockState): Float = factors.getOrPut(state.block) {
        configured[state.block]
            ?: configuredTags.entries.firstOrNull { state.`is`(it.key) }?.value
            ?: if (Eln.config.getBooleanOrElse("tools.xrayScanner.autoDiscoverOreDictionaryOres", true) && state.`is`(Tags.Blocks.ORES)) {
                Eln.config.getDoubleOrElse("tools.xrayScanner.autoDiscoveryOreFactor", 0.15).toFloat()
            } else 0f
    }

    // Block keys: "modid:name"; vanilla ores also cover their deepslate twin. "Eln:Eln.Ore:<meta>" are the 1.7.10 ores.
    private fun loadConfigured(): Map<Block, Float> {
        val result = HashMap<Block, Float>()
        for ((key, factor) in Eln.config.getStringDoubleMap("tools.xrayScanner.oreFactors")) {
            if (!key.contains(':')) continue
            legacyOre(key)?.let { result[it.block.get()] = factor.toFloat(); continue }
            val id = ResourceLocation.tryParse(key) ?: continue
            BuiltInRegistries.BLOCK.getOptional(id).ifPresent { result[it] = factor.toFloat() }
            if (id.namespace == "minecraft" && id.path.endsWith("_ore")) {
                BuiltInRegistries.BLOCK.getOptional(ResourceLocation("minecraft", "deepslate_" + id.path)).ifPresent { result[it] = factor.toFloat() }
            }
        }
        return result
    }

    // Ore dictionary keys such as "oreCopper" become forge:ores/copper.
    private fun loadConfiguredTags(): Map<TagKey<Block>, Float> =
        Eln.config.getStringDoubleMap("tools.xrayScanner.oreFactors")
            .filterKeys { !it.contains(':') && it.startsWith("ore") && it.length > 3 }
            .mapKeys { TagKey.create(Registries.BLOCK, ResourceLocation("forge", "ores/" + it.key.substring(3).lowercase())) }
            .mapValues { it.value.toFloat() }

    private fun legacyOre(key: String): ElnOre? {
        if (!key.startsWith("Eln:Eln.Ore:")) return null
        return when (key.substringAfterLast(':')) {
            "1" -> ElnOre.COPPER
            "4" -> ElnOre.LEAD
            "5" -> ElnOre.TUNGSTEN
            "6" -> ElnOre.CINNABAR
            else -> null
        }
    }
}
