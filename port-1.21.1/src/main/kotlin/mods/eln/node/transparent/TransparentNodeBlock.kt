package mods.eln.node.transparent

import net.minecraft.world.level.block.BaseEntityBlock
import com.mojang.serialization.MapCodec
import net.minecraft.world.level.LevelReader
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.CollisionContext
import mods.eln.node.NodeBlock
import mods.eln.node.NodeBlockEntity
import mods.eln.registry.ElnRegistries
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.HitResult
import mods.eln.registry.RegistryObject

/**
 * Block of the full-block ("transparent node") machines. Drops come from the element (TransparentNode.dropElement),
 * so the block has no loot table, as 1.7.10 dropped nothing itself.
 */
class TransparentNodeBlock : NodeBlock(
    Properties.of().strength(1.0f).noOcclusion().noLootTable().sound(SoundType.METAL).dynamicShape(),
    { pos, state -> TransparentNodeEntity(pos, state) },
    0
) {
    // 1.7.10 removedByPlayer: remember who broke it so the element knows whether to drop its item.
    override fun onDestroyedByPlayer(state: BlockState, level: Level, pos: BlockPos, player: Player, willHarvest: Boolean, fluid: FluidState): Boolean {
        if (!level.isClientSide) {
            val node = (level.getBlockEntity(pos) as? NodeBlockEntity)?.node
            if (node is TransparentNode) {
                node.removedByPlayer = player as? ServerPlayer
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
    }

    // The shape comes from the machine: the server reads it from the node, the client from the element render.
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val entity = level.getBlockEntity(pos) as? TransparentNodeEntity ?: return Shapes.block()
        val descriptor = if (entity.level?.isClientSide != false) entity.elementRender?.transparentNodeDescriptor
            else (entity.node as? TransparentNode)?.element?.transparentNodeDescriptor
        return descriptor?.getShape() ?: Shapes.block()
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = simpleCodec { _ -> TransparentNodeBlock() }

    override fun getCloneItemStack(state: BlockState, target: HitResult, level: LevelReader, pos: BlockPos, player: Player): ItemStack {
        val entity = level.getBlockEntity(pos) as? TransparentNodeEntity ?: return ItemStack.EMPTY
        return entity.elementRender?.transparentNodeDescriptor?.newCreativeTabStack() ?: ItemStack.EMPTY
    }

    val nodeUuid: String
        get() = NODE_UUID

    companion object {
        const val NODE_UUID = "t"

        @JvmField
        val BLOCK: RegistryObject<TransparentNodeBlock> = ElnRegistries.BLOCKS.register("transparent_node") { _: ResourceLocation -> TransparentNodeBlock() }

        @JvmField
        val ENTITY: RegistryObject<BlockEntityType<TransparentNodeEntity>> = ElnRegistries.BLOCK_ENTITY_TYPES.register("transparent_node") { _: ResourceLocation ->
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            BlockEntityType.Builder.of(::TransparentNodeEntity, BLOCK.get()).build(null)
        }
    }
}
