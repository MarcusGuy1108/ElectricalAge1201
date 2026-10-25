package mods.eln.misc

import mods.eln.Eln
import net.minecraft.world.level.storage.LevelResource
import net.minecraftforge.server.ServerLifecycleHooks
import java.io.File
import java.util.Random

// PORT: partial; the rest of 1.7.10 Utils.kt comes over as the code that needs it is ported.
object Utils {
    const val minecraftDay = (60 * 24).toDouble()
    val random = Random()

    @JvmStatic
    var uuid = 1
        get() {
            if (field < 1) field = 1
            return field++
        }
        private set

    @JvmStatic
    fun rand(min: Double, max: Double): Double {
        return random.nextDouble() * (max - min) + min
    }

    @JvmStatic
    fun println(str: String?) {
        if (Eln.config.getBooleanOrElse("debug.logging.enabled", false)) Eln.logger.info(str)
    }

    @JvmStatic
    fun println(str: Any?) {
        if (str != null) println(str.toString())
    }

    @JvmStatic
    fun println(format: String?, vararg data: Any?) {
        println(String.format(format!!, *data))
    }

    // 1.20.1 resolves the save folder directly instead of building "saves/<name>/" by hand.
    fun getMapFile(name: String): File {
        return ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT).resolve(name).toFile()
    }

    @JvmStatic
    fun mapFileExists(name: String): Boolean {
        return getMapFile(name).exists()
    }
}
