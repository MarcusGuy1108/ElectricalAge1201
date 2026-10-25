package mods.eln

import com.mojang.logging.LogUtils
import mods.eln.config.JsonConfig
import net.minecraftforge.fml.loading.FMLPaths
import org.slf4j.Logger
import java.io.File

/**
 * Global mod state, kept as static fields so code ported from the 1.7.10 `Eln` class can use it unchanged.
 */
object Eln {
    const val MODID = "eln"

    @JvmField val LOGGER: Logger = LogUtils.getLogger()
    @JvmField val logger: Logger = LOGGER

    // Replaced with the real config path when the mod loads; unit tests keep this default.
    @JvmField var config = JsonConfig(File("config/Eln.cfg"))

    @JvmField var mqttEnabled = false
    @JvmField var simMetricsEnabled = false
    @JvmField var simMetricsMqttServer = ""
    @JvmField var simMetricsId = "server"
    @JvmField var simMetricsPublishIntervalTicks = 20
    @JvmField var debugEnabled = false

    internal fun loadConfig() {
        config = JsonConfig(FMLPaths.CONFIGDIR.get().resolve("Eln.cfg").toFile())
        config.loadConfig()
    }
}
