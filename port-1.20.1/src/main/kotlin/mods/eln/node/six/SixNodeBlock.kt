package mods.eln.node.six

import mods.eln.misc.Direction
import mods.eln.misc.Utils.println
import mods.eln.node.NodeBlock
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * Block holding up to six [SixNodeElement]s, one per face. 1.7.10 found the clicked element with a hand-written ray
 * trace; 1.20.1 does it with the block shape (one thin slab per occupied face) and [elementSideAt].
 */
class SixNodeBlock : NodeBlock(
    Properties.of().strength(0.3f).noOcclusion().dynamicShape().noLootTable().sound(SoundType.STONE),
    { pos, state -> SixNodeEntity(pos, state) },
    0
) {
    init {
        registerDefaultState(defaultBlockState().setValue(CACHED, false))
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val entity = level.getBlockEntity(pos) as? SixNodeEntity ?: return Shapes.block()
        if (entity.hasVolume() || !entity.cacheBlock().isAir) return Shapes.block()
        var shape = Shapes.empty()
        for (direction in Direction.values()) {
            if (entity.isSideEnabled(direction)) shape = Shapes.or(shape, FACE_SHAPES[direction.int])
        }
        // An empty outline would make the block unclickable while its first element syncs to the client.
        return if (shape.isEmpty) Shapes.block() else shape
    }

    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val entity = level.getBlockEntity(pos) as? SixNodeEntity ?: return Shapes.empty()
        return if (entity.hasVolume() || !entity.cacheBlock().isAir) Shapes.block() else Shapes.empty()
    }

    // A hidden node blocks light like a full block. A state property, not the block entity: the server's light
    // engine reads blocks off the main thread, where block entities are not visible.
    override fun getLightBlock(state: BlockState, level: BlockGetter, pos: BlockPos): Int {
        return if (state.getValue(CACHED)) level.maxLightLevel else super.getLightBlock(state, level, pos)
    }

    override fun propagatesSkylightDown(state: BlockState, level: BlockGetter, pos: BlockPos): Boolean {
        return !state.getValue(CACHED) && super.propagatesSkylightDown(state, level, pos)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(CACHED)
    }

    override fun getCloneItemStack(state: BlockState, target: HitResult, level: BlockGetter, pos: BlockPos, player: Player): ItemStack {
        val entity = level.getBlockEntity(pos) as? SixNodeEntity ?: return ItemStack.EMPTY
        val side = elementSideAt(entity, pos, target.location) ?: return ItemStack.EMPTY
        return entity.elementRenderList[side.int]?.sixNodeDescriptor?.newCreativeTabStack() ?: ItemStack.EMPTY
    }

    @Deprecated("Deprecated in Java")
    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        val entity = level.getBlockEntity(pos) as? SixNodeEntity ?: return InteractionResult.PASS
        val side = elementSideAt(entity, pos, hit.location) ?: return InteractionResult.PASS
        val hitPos = hit.location.subtract(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        val used = entity.onBlockActivated(player, side, hitPos.x.toFloat(), hitPos.y.toFloat(), hitPos.z.toFloat())
        return if (used) InteractionResult.sidedSuccess(level.isClientSide) else InteractionResult.PASS
    }

    // 1.7.10 removedByPlayer: break only the element being looked at, and keep the block while others remain.
    override fun onDestroyedByPlayer(state: BlockState, level: Level, pos: BlockPos, player: Player, willHarvest: Boolean, fluid: FluidState): Boolean {
        if (level.isClientSide) return false
        val entity = level.getBlockEntity(pos) as? SixNodeEntity ?: return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
        val sixNode = entity.node as SixNode? ?: return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
        // Breaking a hidden node first takes the cache block off, as in 1.7.10.
        if (!sixNode.sixNodeCacheBlock.isAir) {
            if (!player.abilities.instabuild) sixNode.dropItem(ItemStack(sixNode.sixNodeCacheBlock.block))
            sixNode.setCacheBlock(Blocks.AIR.defaultBlockState())
            return false
        }
        val breakDirection = resolveBreakDirection(entity, pos, player, sixNode) ?: return false
        if (!sixNode.playerAskToBreakSubBlock(player as? ServerPlayer, breakDirection)) return false
        return if (sixNode.ifSideRemain) true else super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide && !state.`is`(newState.block)) {
            val sixNode = (level.getBlockEntity(pos) as? SixNodeEntity)?.node as SixNode?
            if (sixNode != null) {
                for (direction in Direction.values()) {
                    if (sixNode.getSideEnable(direction)) {
                        println("SixNodeBlock.onRemove deleting side=$direction at $pos")
                        sixNode.deleteSubBlock(null, direction)
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean) {
        if (level.isClientSide) return
        val sixNode = (level.getBlockEntity(pos) as? SixNodeEntity)?.node as SixNode? ?: return
        for (direction in Direction.values()) {
            if (sixNode.getSideEnable(direction) && !getIfOtherBlockIsSolid(level, pos.x, pos.y, pos.z, direction)) {
                println("SixNodeBlock.neighborChanged deleting side=$direction at $pos dueTo=${block.javaClass.simpleName}")
                sixNode.deleteSubBlock(null, direction)
            }
        }
        if (!sixNode.ifSideRemain) {
            level.removeBlock(pos, false)
        } else {
            super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        }
    }

    override fun canBeReplaced(state: BlockState, context: net.minecraft.world.item.context.BlockPlaceContext): Boolean {
        return false
    }

    private fun resolveBreakDirection(entity: SixNodeEntity, pos: BlockPos, player: Player, sixNode: SixNode): Direction? {
        // Server player state can lag a few ticks behind under low TPS; a long reach still resolves a side.
        val ray = player.pick(8.0, 1.0f, false)
        if (ray is BlockHitResult && ray.blockPos == pos) {
            val side = elementSideAt(entity, pos, ray.location)
            if (side != null && sixNode.getSideEnable(side)) return side
        }

        // Fallback: pick the enabled face most directly facing the player's look vector.
        val look = player.getViewVector(1.0f)
        var bestDirection: Direction? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (direction in Direction.values()) {
            if (!sixNode.getSideEnable(direction)) continue
            val normal = direction.toForge().normal
            // The clicked face points against the player look direction.
            val score = -(look.x * normal.x + look.y * normal.y + look.z * normal.z)
            if (score > bestScore) {
                bestScore = score
                bestDirection = direction
            }
        }
        return bestDirection
    }

    fun getIfOtherBlockIsSolid(world: Level, x: Int, y: Int, z: Int, direction: Direction): Boolean {
        val vect = intArrayOf(x, y, z)
        direction.applyTo(vect, 1)
        val pos = BlockPos(vect[0], vect[1], vect[2])
        // During chunk load, neighboring chunks can still be unavailable. Treat that as
        // "unknown" instead of "air" so attached six-node parts do not self-delete
        // before their support block has actually loaded.
        if (!world.isLoaded(pos)) return true
        val state = world.getBlockState(pos)
        if (state.isAir) return false
        // 1.7.10 required an opaque cube; solid-render blocks are the 1.20.1 equivalent (glass does not count).
        return state.isSolidRender(world, pos)
    }

    val nodeUuid: String
        get() = NODE_UUID

    companion object {
        const val NODE_UUID = "s"

        /** True while a wrench cache block hides the node. */
        @JvmField
        val CACHED: BooleanProperty = BooleanProperty.create("cached")

        private const val FACE_THICKNESS = 0.2

        // Outline slab for each element side, indexed by Direction.int.
        private val FACE_SHAPES: Array<VoxelShape> = Direction.values().map { side ->
            val h = FACE_THICKNESS
            when (side) {
                Direction.XN -> Shapes.box(0.0, 0.0, 0.0, h, 1.0, 1.0)
                Direction.XP -> Shapes.box(1 - h, 0.0, 0.0, 1.0, 1.0, 1.0)
                Direction.YN -> Shapes.box(0.0, 0.0, 0.0, 1.0, h, 1.0)
                Direction.YP -> Shapes.box(0.0, 1 - h, 0.0, 1.0, 1.0, 1.0)
                Direction.ZN -> Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, h)
                Direction.ZP -> Shapes.box(0.0, 0.0, 1 - h, 1.0, 1.0, 1.0)
            }
        }.toTypedArray()

        /**
         * Element side whose slab contains [location] (a world position on the block), preferring the slab the
         * point is deepest in when slabs overlap at an edge.
         */
        fun elementSideAt(entity: SixNodeEntity, pos: BlockPos, location: Vec3): Direction? {
            if (entity.hasVolume()) return Direction.YN
            val x = location.x - pos.x
            val y = location.y - pos.y
            val z = location.z - pos.z
            val epsilon = 1e-4
            var best: Direction? = null
            var bestDepth = Double.MAX_VALUE
            for (side in Direction.values()) {
                if (!entity.isSideEnabled(side)) continue
                val depth = when (side) {
                    Direction.XN -> x
                    Direction.XP -> 1 - x
                    Direction.YN -> y
                    Direction.YP -> 1 - y
                    Direction.ZN -> z
                    Direction.ZP -> 1 - z
                }
                if (depth <= FACE_THICKNESS + epsilon && depth < bestDepth) {
                    bestDepth = depth
                    best = side
                }
            }
            return best
        }
    }
}
