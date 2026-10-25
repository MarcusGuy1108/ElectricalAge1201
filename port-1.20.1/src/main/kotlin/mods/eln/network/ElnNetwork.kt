package mods.eln.network

import mods.eln.Eln
import mods.eln.PacketHandler
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel
import java.util.concurrent.Callable

/**
 * Eln's 1.7.10 protocol is a byte stream whose first byte is the packet id; one message type carries it both ways.
 */
class RawPacket(val bytes: ByteArray)

object ElnNetwork {
    private const val PROTOCOL = "1"

    val channel: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(Eln.MODID, "main"), { PROTOCOL }, PROTOCOL::equals, PROTOCOL::equals
    )

    fun register() {
        channel.messageBuilder(RawPacket::class.java, 0)
            .encoder { packet, buffer -> buffer.writeByteArray(packet.bytes) }
            .decoder { buffer -> RawPacket(buffer.readByteArray()) }
            .consumerMainThread { packet, context ->
                val player: Player? = context.get().sender
                    ?: DistExecutor.unsafeCallWhenOn(Dist.CLIENT) { Callable { ClientAccess.player() } }
                if (player != null) PacketHandler.packetRx(packet.bytes, player)
                context.get().packetHandled = true
            }
            .add()
    }

    fun sendToPlayer(bytes: ByteArray, player: ServerPlayer) {
        channel.send(PacketDistributor.PLAYER.with { player }, RawPacket(bytes))
    }

    fun sendToServer(bytes: ByteArray) {
        channel.sendToServer(RawPacket(bytes))
    }
}

// Kept in its own object so dedicated servers never load Minecraft client classes.
private object ClientAccess {
    fun player(): Player? = Minecraft.getInstance().player
}
