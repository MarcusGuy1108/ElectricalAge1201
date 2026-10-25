package mods.eln.lightblock

import mods.eln.misc.Coordinate
import mods.eln.registry.ElnRegistries
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.registries.RegistryObject

/**
 * Invisible light source that lamps place where their beam lands. 1.7.10 kept the light level in the block metadata;
 * here it is the [LIGHT] block state property.
 */
class LightBlock : BaseEntityBlock(
    Properties.of().replaceable().noCollission().noOcclusion().noLootTable().instabreak().lightLevel { it.getValue(LIGHT) }
) {
    init {
        registerDefaultState(stateDefinition.any().setValue(LIGHT, 0))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(LIGHT)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return Shapes.empty()
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.INVISIBLE
    }

    override fun propagatesSkylightDown(state: BlockState, level: BlockGetter, pos: BlockPos): Boolean {
        return true
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return LightBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (level.isClientSide) null else createTickerHelper(type, ENTITY.get(), LightBlockEntity::tick)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!state.`is`(newState.block)) {
            val coord = Coordinate(pos.x, pos.y, pos.z, level)
            for (o in LightBlockEntity.observers) {
                o.lightBlockDestructor(coord)
            }
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }

    companion object {
        @JvmField
        val LIGHT: IntegerProperty = IntegerProperty.create("light", 0, 15)

        @JvmField
        val BLOCK: RegistryObject<LightBlock> = ElnRegistries.BLOCKS.register("light") { LightBlock() }

        @JvmField
        val ENTITY: RegistryObject<BlockEntityType<LightBlockEntity>> = ElnRegistries.BLOCK_ENTITY_TYPES.register("light") {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            BlockEntityType.Builder.of(::LightBlockEntity, BLOCK.get()).build(null)
        }
    }
}
