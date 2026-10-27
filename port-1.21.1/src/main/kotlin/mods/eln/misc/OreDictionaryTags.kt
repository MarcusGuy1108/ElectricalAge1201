package mods.eln.misc

import mods.eln.Eln
import net.minecraft.resources.ResourceLocation

/** Maps 1.7.10 ore dictionary names to 1.20.1 item tags: ingotCopper -> c:ingots/copper, others under eln:. */
object OreDictionaryTags {
    private val FORGE_PREFIXES = listOf("ingot", "dust", "ore", "plate", "gem", "nugget", "block", "gear", "rod", "wire")

    // Ore dictionary names whose 1.20.1 tag does not follow the prefix pattern.
    private val SPECIAL = mapOf(
        "plankWood" to ResourceLocation.fromNamespaceAndPath("minecraft", "planks"),
        "logWood" to ResourceLocation.fromNamespaceAndPath("minecraft", "logs"),
        "treeSapling" to ResourceLocation.fromNamespaceAndPath("minecraft", "saplings"),
        "treeLeaves" to ResourceLocation.fromNamespaceAndPath("minecraft", "leaves"),
        "stickWood" to ResourceLocation.fromNamespaceAndPath("c", "rods/wooden"),
        "materialString" to ResourceLocation.fromNamespaceAndPath("c", "strings"),
    )

    @JvmStatic
    fun tagFor(oreName: String): ResourceLocation {
        SPECIAL[oreName]?.let { return it }
        val prefix = FORGE_PREFIXES.firstOrNull { oreName.startsWith(it) && oreName.length > it.length && oreName[it.length].isUpperCase() }
        return if (prefix != null) {
            // Forge 1.20.1 calls block tags storage_blocks (c:storage_blocks/iron).
            ResourceLocation.fromNamespaceAndPath("c", (if (prefix == "block") "storage_block" else prefix) + "s/" + snake(oreName.substring(prefix.length)))
        } else {
            ResourceLocation.fromNamespaceAndPath(Eln.MODID, snake(oreName))
        }
    }

    private fun snake(name: String) = name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
}
