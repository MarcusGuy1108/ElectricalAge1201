package mods.eln.client

import net.minecraft.client.Minecraft

/** Duration of the last rendered frame in seconds (at most 0.1 s, 0 while paused), as 1.7.10 FrameTime gave it. */
object FrameTime {
    @JvmStatic
    var deltaT = 0.02f
        internal set

    @JvmStatic
    fun get2(): Float = if (Minecraft.getInstance().isPaused) 0f else deltaT.coerceAtMost(0.1f)

    @JvmStatic
    fun getNotCaped2(): Float = get2()
}
