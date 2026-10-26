package mods.eln.misc

import mods.eln.Eln
import net.minecraft.resources.ResourceLocation

/** Maps 1.7.10 ore dictionary names to 1.20.1 item tags: ingotCopper -> forge:ingots/copper, others under eln:. */
object OreDictionaryTags {
    private val FORGE_PREFIXES = listOf("ingot", "dust", "ore", "plate", "gem", "nugget", "block", "gear", "rod", "wire")

    @JvmStatic
    fun tagFor(oreName: String): ResourceLocation {
        val prefix = FORGE_PREFIXES.firstOrNull { oreName.startsWith(it) && oreName.length > it.length && oreName[it.length].isUpperCase() }
        return if (prefix != null) {
            ResourceLocation("forge", prefix + "s/" + snake(oreName.substring(prefix.length)))
        } else {
            ResourceLocation(Eln.MODID, snake(oreName))
        }
    }

    private fun snake(name: String) = name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
}
