package mods.eln.network

import mods.eln.Eln
import mods.eln.PacketHandler
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

/**
 * Eln's 1.7.10 protocol is a byte stream whose first byte is the packet id; one payload type carries it both ways.
 */
class RawPacket(val bytes: ByteArray) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<RawPacket> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<RawPacket>(ResourceLocation.fromNamespaceAndPath(Eln.MODID, "main"))
        val CODEC: StreamCodec<FriendlyByteBuf, RawPacket> =
            StreamCodec.of({ buffer, packet -> buffer.writeByteArray(packet.bytes) }, { buffer -> RawPacket(buffer.readByteArray()) })
    }
}

object ElnNetwork {
    private const val PROTOCOL = "1"

    // Mod bus listener. Handlers run on the receiving side's main thread.
    fun register(event: RegisterPayloadHandlersEvent) {
        event.registrar(PROTOCOL).playBidirectional(RawPacket.TYPE, RawPacket.CODEC) { packet, context ->
            PacketHandler.packetRx(packet.bytes, context.player())
        }
    }

    fun sendToPlayer(bytes: ByteArray, player: ServerPlayer) {
        PacketDistributor.sendToPlayer(player, RawPacket(bytes))
    }

    fun sendToServer(bytes: ByteArray) {
        PacketDistributor.sendToServer(RawPacket(bytes))
    }
}
