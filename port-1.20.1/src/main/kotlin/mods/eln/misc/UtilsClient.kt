package mods.eln.misc

import mods.eln.network.ElnNetwork
import java.io.ByteArrayOutputStream

// PORT: partial; the rest of 1.7.10 UtilsClient.kt (mostly GL rendering helpers) comes over with the renderers.
object UtilsClient {
    @JvmStatic
    fun sendPacketToServer(bos: ByteArrayOutputStream) {
        ElnNetwork.sendToServer(bos.toByteArray())
    }
}
