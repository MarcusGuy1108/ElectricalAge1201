package mods.eln.node

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.Utils.fatal
import mods.eln.misc.Utils.notifyNeighbor
import mods.eln.misc.Utils.println
import mods.eln.misc.UtilsClient
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue

abstract class NodeBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    BlockEntity(type, pos, state), ITileEntitySpawnClient, INodeEntity {
    val block: NodeBlock
        get() = blockState.block as NodeBlock
    var redstone = false
    var lastLight = 0xFF
    var firstUnserialize = true
    override fun serverPublishUnserialize(stream: DataInputStream) {
        var light = 0
        try {
            if (firstUnserialize) {
                firstUnserialize = false
                notifyNeighbor(this)
            }
            val b = stream.readByte()
            light = b.toInt() and 0xF
            val newRedstone = b.toInt() and 0x10 != 0
            if (redstone != newRedstone) {
                redstone = newRedstone
                level?.updateNeighborsAt(blockPos, blockState.block)
            } else {
                redstone = newRedstone
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // The light level itself reaches the client through the NodeBlock LIGHT block state property.
        lastLight = light
    }

    override fun serverPacketUnserialize(stream: DataInputStream) {}

    abstract fun isProvidingWeakPower(side: Direction?): Int

    var internalNode: Node? = null

    val node: Node?
        get() {
            if (level!!.isClientSide) {
                fatal()
            }
            if (internalNode == null) {
                val nodeFromCoordonate = NodeManager.instance!!.getNodeFromCoordonate(Coordinate(this))
                if (nodeFromCoordonate is Node) {
                    internalNode = nodeFromCoordonate
                } else if (nodeFromCoordonate != null) {
                    Utils.println("WARN: getNode found non-Node ${nodeFromCoordonate.javaClass.simpleName} at ${Coordinate(this)}")
                }
            }
            return internalNode
        }

    // PORT: newContainer/newGuiDraw return as menu and screen factories with the GUI layer.

    override fun getRenderBoundingBox(): AABB {
        return if (cameraDrawOptimisation()) localRenderBoundingBox() else unoptimizedRenderBoundingBox()
    }

    open fun cameraDrawOptimisation(): Boolean {
        return true
    }

    protected fun localRenderBoundingBox(): AABB {
        return AABB(
            (blockPos.x - 1).toDouble(),
            (blockPos.y - 1).toDouble(),
            (blockPos.z - 1).toDouble(),
            (blockPos.x + 1).toDouble(),
            (blockPos.y + 1).toDouble(),
            (blockPos.z + 1).toDouble()
        )
    }

    open fun unoptimizedRenderBoundingBox(): AABB {
        return localRenderBoundingBox()
    }

    val lightValue: Int
        get() = if (level!!.isClientSide) {
            if (lastLight == 0xFF) {
                0
            } else lastLight
        } else {
            node?.lightValue ?: 0
        }

    // PORT: getMaxRenderDistanceSquared moves to the block entity renderer's getViewDistance with the renderers.

    @Suppress("UNUSED_PARAMETER") fun onBlockPlacedBy(front: Direction?, entityLiving: LivingEntity?, metadata: Int) {}

    // 1.7.10 registered client entities on their first updateEntity(); onLoad runs at the same point.
    override fun onLoad() {
        super.onLoad()
        if (level!!.isClientSide) {
            clientList.add(this)
        }
    }

    fun onBlockAdded() {
        if (!level!!.isClientSide && node == null) {
            level!!.removeBlock(blockPos, false)
        }
    }

    fun onBreakBlock() {
        if (!level!!.isClientSide) {
            if (node == null) return
            node!!.onBreakBlock()
        }
    }

    override fun onChunkUnloaded() {
        if (level!!.isClientSide) {
            destructor()
        }
    }

    //client only
    open fun destructor() {
        clientList.remove(this)
    }

    override fun setRemoved() {
        if (level?.isClientSide == true) {
            destructor()
        }
        super.setRemoved()
    }

    fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (!level!!.isClientSide) {
            if (node == null) return false
            node!!.onBlockActivated(entityPlayer!!, side!!, vx, vy, vz)
            return true
        }
        return true
    }

    fun onNeighborBlockChange() {
        if (!level!!.isClientSide) {
            if (node == null) return
            node!!.onNeighborBlockChange()
        }
    }

    // 1.7.10 sent the node publish packet as the description packet; 1.20.1 carries the same bytes in the update tag.
    override fun getUpdateTag(): CompoundTag {
        val tag = super.getUpdateTag()
        val node = node
        if (node == null) {
            println("ASSERT NULL NODE getUpdateTag() nodeblock entity")
            return tag
        }
        tag.putByteArray(PUBLISH_TAG, node.publishPacket!!.toByteArray())
        return tag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun handleUpdateTag(tag: CompoundTag) {
        super.handleUpdateTag(tag)
        if (tag.contains(PUBLISH_TAG)) applyPublishPacket(tag.getByteArray(PUBLISH_TAG))
    }

    override fun onDataPacket(net: net.minecraft.network.Connection, pkt: ClientboundBlockEntityDataPacket) {
        pkt.tag?.let { handleUpdateTag(it) }
    }

    // Skips the packetNodeSingleSerialized header (id, x, y, z, dimension) and feeds the rest to the client entity.
    private fun applyPublishPacket(bytes: ByteArray) {
        try {
            val stream = DataInputStream(ByteArrayInputStream(bytes))
            stream.readByte()
            stream.readInt()
            stream.readInt()
            stream.readInt()
            Utils.readDimension(stream)
            if (stream.readUTF() == nodeUuid) serverPublishUnserialize(stream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    open fun preparePacketForServer(stream: DataOutputStream) {
        try {
            stream.writeByte(Eln.packetPublishForNode.toInt())
            stream.writeInt(blockPos.x)
            stream.writeInt(blockPos.y)
            stream.writeInt(blockPos.z)
            Utils.writeDimension(stream, level!!.dimension())
            stream.writeUTF(nodeUuid)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    open fun sendPacketToServer(bos: ByteArrayOutputStream?) {
        UtilsClient.sendPacketToServer(bos!!)
    }

    // PORT: getCableRender/getAdjacentCableRender (CableRenderDescriptor lookups) return with the cable renderer.

    open fun getCableDry(side: Direction?, lrdu: LRDU?): Int {
        return 0
    }

    fun canConnectRedstone(@Suppress("UNUSED_PARAMETER") xn: Direction?): Boolean {
        return if (level!!.isClientSide) redstone else {
            if (node == null) false else node!!.canConnectRedstone()
        }
    }

    open fun clientRefresh(deltaT: Float) {}

    companion object {
        private const val PUBLISH_TAG = "elnPublish"

        @JvmField
        val clientList = LinkedBlockingQueue<NodeBlockEntity>()

        // PORT: getEntity(x, y, z) reads the client world; it returns with the client-side renderers.
    }
}
