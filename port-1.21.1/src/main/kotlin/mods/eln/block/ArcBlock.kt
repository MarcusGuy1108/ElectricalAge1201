package mods.eln.block

import net.minecraft.resources.ResourceLocation
import mods.eln.Eln
import mods.eln.generic.CreativeTabPopulator
import mods.eln.registry.ElnRegistries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.MapColor
import mods.eln.registry.RegistryObject

/**
 * Storage blocks for the arc furnace's ingots. The 1.7.10 ore dictionary listed them as blockAluminum and blockSteel;
 * here they sit in c:storage_blocks/<material>.
 */
enum class ArcBlock(val id: String, val material: String) {
    CLAY("arc_clay_block", "aluminum"),
    METAL("arc_metal_block", "steel");

    lateinit var block: RegistryObject<Block>
        private set

    companion object {
        fun register() {
            for (arc in values()) {
                // 1.7.10 gave them no hardness at all; they break like other metal storage blocks now.
                arc.block = ElnRegistries.BLOCKS.register(arc.id) { _: ResourceLocation ->
                    object : Block(Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f)) {
                        override fun getDescriptionId() = "tile.${arc.id}.name"
                    }
                }
                val item = ElnRegistries.ITEMS.register(arc.id) { _: ResourceLocation -> BlockItem(arc.block.get(), Item.Properties()) }
                CreativeTabPopulator.addStack({ Eln.creativeTabOresMaterials }) { ItemStack(item.get()) }
            }
        }
    }
}
