package mods.eln.node

import net.minecraft.world.item.ItemStack
import net.minecraft.world.ItemInteractionResult
import mods.eln.misc.Direction
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.Direction as McDirection

/**
 * Block side of a node. The node itself lives in [NodeManager]; this block forwards world events to it through
 * [NodeBlockEntity]. 1.7.10 created the tile entity from a class; 1.20.1 needs [entityFactory] instead.
 */
abstract class NodeBlock(
    properties: Properties,
    private val entityFactory: (BlockPos, BlockState) -> BlockEntity,
    var blockItemNbr: Int
) : BaseEntityBlock(properties.lightLevel { it.getValue(LIGHT) }) {

    init {
        registerDefaultState(stateDefinition.any().setValue(LIGHT, 0))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(LIGHT)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return entityFactory(pos, state)
    }

    // Rendered by block entity renderers, as 1.7.10 getRenderType() == -1 did.
    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun isSignalSource(state: BlockState): Boolean {
        return true
    }

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: McDirection): Int {
        val entity = level.getBlockEntity(pos) as? NodeBlockEntity ?: return 0
        return entity.isProvidingWeakPower(Direction.from(direction))
    }

    override fun canConnectRedstone(state: BlockState, level: BlockGetter, pos: BlockPos, direction: McDirection?): Boolean {
        val entity = level.getBlockEntity(pos) as? NodeBlockEntity ?: return false
        return entity.canConnectRedstone(Direction.XN)
    }

    //server
    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        if (!level.isClientSide && !oldState.`is`(this)) {
            (level.getBlockEntity(pos) as? NodeBlockEntity)?.onBlockAdded()
        }
    }

    //server
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!state.`is`(newState.block)) {
            (level.getBlockEntity(pos) as? NodeBlockEntity)?.onBreakBlock()
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        if (!level.isClientSide) {
            (level.getBlockEntity(pos) as? NodeBlockEntity)?.onNeighborBlockChange()
        }
    }

    // 1.21 splits right-clicks into useItemOn (holding something) and useWithoutItem; both run the 1.20.1 handler.
    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): ItemInteractionResult {
        // Not used here: let the held item have its go, as 1.20.1 did after a PASS.
        return if (activate(state, level, pos, player, hand, hit).consumesAction()) ItemInteractionResult.sidedSuccess(level.isClientSide)
        else ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        return activate(state, level, pos, player, InteractionHand.MAIN_HAND, hit)
    }

    //client server
    protected open fun activate(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        val entity = level.getBlockEntity(pos) as? NodeBlockEntity ?: return InteractionResult.PASS
        val hitPos = hit.location.subtract(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        val used = entity.onBlockActivated(player, Direction.from(hit.direction), hitPos.x.toFloat(), hitPos.y.toFloat(), hitPos.z.toFloat())
        return if (used) InteractionResult.sidedSuccess(level.isClientSide) else InteractionResult.PASS
    }

    // PORT: damageDropped(metadata) has no 1.20.1 equivalent; drops come from loot tables or the node's own dropItem.

    companion object {
        @JvmField
        val LIGHT: IntegerProperty = IntegerProperty.create("light", 0, 15)

        @JvmStatic
        fun setLight(level: Level, pos: BlockPos, light: Int) {
            val state = level.getBlockState(pos)
            if (state.block is NodeBlock && state.getValue(LIGHT) != light) {
                level.setBlock(pos, state.setValue(LIGHT, light), Block.UPDATE_ALL)
            }
        }
    }
}
