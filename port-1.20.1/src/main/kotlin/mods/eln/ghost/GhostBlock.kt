package mods.eln.ghost

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.registry.ElnRegistries
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.registries.RegistryObject

/**
 * Invisible block that fills the volume of a multiblock machine and forwards clicks and breaking to it. 1.7.10 kept
 * the cube/floor/ladder kind in the metadata; here it is the [TYPE] block state property.
 */
class GhostBlock : Block(Properties.of().strength(0.5f).noOcclusion().noLootTable().sound(SoundType.METAL)) {
    init {
        registerDefaultState(stateDefinition.any().setValue(TYPE, tCube))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(TYPE)
    }

    // shortcut: cube ghosts are full blocks; 1.7.10 asked the machine for its own collision boxes, add that if a
    // machine needs a walk-through or partial volume.
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return when (state.getValue(TYPE)) {
            tFloor -> FLOOR
            tLadder -> Shapes.empty()
            else -> Shapes.block()
        }
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE

    override fun isLadder(state: BlockState, level: LevelReader, pos: BlockPos, entity: LivingEntity): Boolean {
        return state.getValue(TYPE) == tLadder
    }

    override fun getCloneItemStack(level: BlockGetter, pos: BlockPos, state: BlockState): ItemStack = ItemStack.EMPTY

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide && !state.`is`(newState.block)) {
            getElement(level, pos)?.breakBlock()
        }
        @Suppress("DEPRECATION")
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS
        if (level.isClientSide) return InteractionResult.SUCCESS
        val element = getElement(level, pos) ?: return InteractionResult.PASS
        val hitPos = hit.location.subtract(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        val used = element.onBlockActivated(player, Direction.from(hit.direction), hitPos.x.toFloat(), hitPos.y.toFloat(), hitPos.z.toFloat())
        return if (used) InteractionResult.SUCCESS else InteractionResult.PASS
    }

    fun getElement(world: Level, pos: BlockPos): GhostElement? {
        return Eln.ghostManager.getGhost(Coordinate(pos.x, pos.y, pos.z, world))
    }

    val nodeUuid: String
        get() = "g"

    companion object {
        const val tCube = 0
        const val tFloor = 1
        const val tLadder = 2

        @JvmField
        val TYPE: IntegerProperty = IntegerProperty.create("type", tCube, tLadder)

        private val FLOOR: VoxelShape = box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0)

        @JvmField
        val BLOCK: RegistryObject<GhostBlock> = ElnRegistries.BLOCKS.register("ghost") { GhostBlock() }

        @JvmStatic
        fun stateOf(block: Block, meta: Int): BlockState {
            return if (block is GhostBlock) block.defaultBlockState().setValue(TYPE, meta) else block.defaultBlockState()
        }
    }
}
