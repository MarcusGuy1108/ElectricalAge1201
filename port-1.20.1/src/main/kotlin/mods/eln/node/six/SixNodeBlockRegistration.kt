package mods.eln.node.six

import mods.eln.registry.ElnRegistries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.registries.RegistryObject

/** The single six-node block and its block entity type; touched from ElnMod so they register in time. */
object SixNodeBlockRegistration {
    @JvmField
    val BLOCK: RegistryObject<SixNodeBlock> = ElnRegistries.BLOCKS.register("six_node") { SixNodeBlock() }

    @JvmField
    val ENTITY: RegistryObject<BlockEntityType<SixNodeEntity>> = ElnRegistries.BLOCK_ENTITY_TYPES.register("six_node") {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        BlockEntityType.Builder.of(::SixNodeEntity, BLOCK.get()).build(null)
    }
}
