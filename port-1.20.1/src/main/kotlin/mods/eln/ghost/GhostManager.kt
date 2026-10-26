package mods.eln.ghost

import mods.eln.misc.Coordinate
import mods.eln.misc.Utils.getTags
import mods.eln.node.NodeManager
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import java.util.*

/** Ghost blocks of every loaded dimension and the machines (observers) they belong to. Saved with the node data. */
class GhostManager {
    var ghostTable: MutableMap<Coordinate?, GhostElement> = Hashtable()
    var observerTable: MutableMap<Coordinate?, GhostObserver> = Hashtable()

    fun clear() {
        ghostTable.clear()
        observerTable.clear()
    }

    fun getGhost(coordinate: Coordinate?): GhostElement? {
        return ghostTable[coordinate]
    }

    fun removeGhost(coordinate: Coordinate?) {
        removeGhostNode(coordinate)
        ghostTable.remove(coordinate)
    }

    fun addObserver(observer: GhostObserver) {
        observerTable[observer.ghostObserverCoordonate] = observer
    }

    fun getObserver(coordinate: Coordinate?): GhostObserver? {
        return observerTable[coordinate]
    }

    fun removeObserver(coordinate: Coordinate?) {
        observerTable.remove(coordinate)
    }

    fun removeGhostAndBlockWithObserver(observerCoordinate: Coordinate?) {
        removeGhostsAndBlocks { it.observatorCoordonate == observerCoordinate }
    }

    fun removeGhostAndBlockWithObserver(observerCoordinate: Coordinate?, uuid: Int) {
        removeGhostsAndBlocks { it.observatorCoordonate == observerCoordinate && it.uUID == uuid }
    }

    fun removeGhostAndBlockWithObserverAndNotUuid(observerCoordinate: Coordinate?, uuid: Int) {
        removeGhostsAndBlocks { it.observatorCoordonate == observerCoordinate && it.uUID != uuid }
    }

    // The table entry goes first, so the ghost block's onRemove finds nothing to forward to the machine.
    private fun removeGhostsAndBlocks(predicate: (GhostElement) -> Boolean) {
        val removed = ghostTable.values.filter(predicate)
        for (element in removed) {
            ghostTable.remove(element.elementCoordinate)
            removeGhostNode(element.elementCoordinate)
            setAir(element.elementCoordinate!!)
        }
    }

    fun removeGhostNode(c: Coordinate?) {
        val node = NodeManager.instance!!.getNodeFromCoordonate(c) ?: return
        node.onBreakBlock()
    }

    fun removeGhostAndBlock(coordinate: Coordinate) {
        removeGhost(coordinate)
        setAir(coordinate)
    }

    private fun setAir(coordinate: Coordinate) {
        coordinate.world().setBlock(coordinate.blockPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
    }

    fun loadFromNBT(nbt: CompoundTag) {
        for (o in getTags(nbt)) {
            val ghost = GhostElement()
            ghost.readFromNBT(o, "")
            ghostTable[ghost.elementCoordinate] = ghost
        }
    }

    fun saveToNBT(nbt: CompoundTag, dim: ResourceKey<Level>) {
        var nodeCounter = 0
        for (ghost in ghostTable.values) {
            if (ghost.elementCoordinate!!.dimension != dim) continue
            val nbtGhost = CompoundTag()
            ghost.writeToNBT(nbtGhost, "")
            nbt.put("n" + nodeCounter++, nbtGhost)
        }
    }

    fun unload(dimension: ResourceKey<Level>) {
        ghostTable.values.removeIf { it.elementCoordinate!!.dimension == dimension }
    }

    fun canCreateGhostAt(world: Level, x: Int, y: Int, z: Int): Boolean {
        val pos = BlockPos(x, y, z)
        if (!world.isLoaded(pos)) return false
        val state = world.getBlockState(pos)
        return state.isAir || state.canBeReplaced()
    }

    @JvmOverloads
    fun createGhost(coordinate: Coordinate, observerCoordinate: Coordinate, UUID: Int, state: BlockState = GhostBlock.BLOCK.get().defaultBlockState()) {
        if (coordinate.world().setBlock(coordinate.blockPos, state, Block.UPDATE_ALL)) {
            val element = GhostElement(Coordinate(coordinate), observerCoordinate, UUID)
            ghostTable[element.elementCoordinate] = element
        }
    }
}
