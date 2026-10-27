package mods.eln.sound

import mods.eln.Eln
import mods.eln.misc.Utils
import mods.eln.misc.Utils.sendPacketToClient
import net.neoforged.neoforge.server.ServerLifecycleHooks
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

object SoundServer {
    fun play(p: SoundCommand) {
        val bos = ByteArrayOutputStream(64)
        val stream = DataOutputStream(bos)
        try {
            stream.writeByte(Eln.packetPlaySound.toInt())
            Utils.writeDimension(stream, p.world!!.dimension())
            p.writeTo(stream)
            val server = ServerLifecycleHooks.getCurrentServer() ?: return
            for (player in server.playerList.players) {
                if (player.level().dimension() == p.world!!.dimension() && player.distanceToSqr(p.x, p.y, p.z) < (p.rangeMax + 2) * (p.rangeMax + 2)) {
                    sendPacketToClient(bos, player)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
