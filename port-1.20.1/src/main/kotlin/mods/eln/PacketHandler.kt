package mods.eln

import mods.eln.misc.Direction
import mods.eln.gui.ElnMenus
import mods.eln.misc.Coordinate
import mods.eln.misc.Utils
import mods.eln.misc.Utils.println
import mods.eln.node.INodeEntity
import mods.eln.node.NodeManager
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException

/**
 * Dispatches Eln's byte-stream packets by their first byte, on both sides (see [mods.eln.network.ElnNetwork]).
 */
object PacketHandler {
    // A client may only touch nodes it could plausibly be interacting with.
    private const val MAX_NODE_PACKET_DISTANCE = 64.0

    fun packetRx(bytes: ByteArray, player: Player) {
        packetRx(DataInputStream(ByteArrayInputStream(bytes)), player)
    }

    fun packetRx(stream: DataInputStream, player: Player) {
        try {
            when (stream.readByte()) {
                Eln.packetNodeSingleSerialized -> packetNodeSingleSerialized(stream, player)
                Eln.packetPublishForNode -> packetForNode(stream, player)
                Eln.packetForClientNode -> packetForClientNode(stream, player)
                Eln.packetOpenLocalGui -> packetOpenLocalGui(stream, player)
                // PORT: packetPlayerKey, packetPlaySound, packetDestroyUuid,
                // packetClientToServerConnection, packetServerToClientInfo and packetFalstadImport return with
                // the key handler, GUIs, sounds, client UUID manager, shared config and Falstad import.
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Client: the server asked to open a GUI without a container (1.7.10 GuiHandler.nodeBaseOpen + side).
    private fun packetOpenLocalGui(stream: DataInputStream, player: Player) {
        if (!player.level().isClientSide) return
        val side = Direction.fromInt(stream.readInt()) ?: return
        ElnMenus.openLocalGui(side, BlockPos(stream.readInt(), stream.readInt(), stream.readInt()))
    }

    private fun packetForNode(stream: DataInputStream, player: Player) {
        if (player !is ServerPlayer) return
        try {
            val coordinate = Coordinate(stream.readInt(), stream.readInt(), stream.readInt(), Utils.readDimension(stream))
            // 1.7.10 trusted any coordinate from the client; check the player is actually near the node.
            if (coordinate.dimension != player.level().dimension() || coordinate.distanceTo(player) > MAX_NODE_PACKET_DISTANCE) {
                println("packetForNode rejected from ${player.gameProfile.name} for $coordinate")
                return
            }
            val node = NodeManager.instance!!.getNodeFromCoordonate(coordinate)
            if (node != null && node.nodeUuid == stream.readUTF()) {
                node.networkUnserialize(stream, player)
            } else {
                println("packetForNode node found")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun packetForClientNode(stream: DataInputStream, player: Player) {
        clientNodePacket(stream, player) { it.serverPacketUnserialize(stream) }
    }

    private fun packetNodeSingleSerialized(stream: DataInputStream, player: Player) {
        clientNodePacket(stream, player) { it.serverPublishUnserialize(stream) }
    }

    // Shared body of the two server-to-client node packets, which only differ in the unserialize call.
    private fun clientNodePacket(stream: DataInputStream, player: Player, unserialize: (INodeEntity) -> Unit) {
        try {
            val x = stream.readInt()
            val y = stream.readInt()
            val z = stream.readInt()
            val dimension = Utils.readDimension(stream)
            if (player.level().dimension() == dimension) {
                val entity = player.level().getBlockEntity(BlockPos(x, y, z))
                if (entity != null && entity is INodeEntity) {
                    val node = entity as INodeEntity
                    if (node.nodeUuid == stream.readUTF()) {
                        unserialize(node)
                        if (0 != stream.available()) {
                            println("0 != stream.available()")
                        }
                    } else {
                        println("Wrong node UUID warning")
                        val dataSkipLength = stream.readByte().toInt()
                        for (idx in 0 until dataSkipLength) {
                            stream.readByte()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
