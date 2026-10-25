package mods.eln

import com.mojang.logging.LogUtils
import mods.eln.config.JsonConfig
import mods.eln.generic.GenericCreativeTab
import mods.eln.node.NodeManager
import mods.eln.node.NodeServer
import mods.eln.server.ServerEventListener
import mods.eln.sim.Simulator
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.slf4j.Logger
import java.io.File

/**
 * Global mod state, kept as static fields so code ported from the 1.7.10 `Eln` class can use it unchanged.
 */
object Eln {
    const val MODID = "eln"

    const val packetPlayerKey: Byte = 14
    const val packetNodeSingleSerialized: Byte = 15
    const val packetPublishForNode: Byte = 16
    const val packetOpenLocalGui: Byte = 17
    const val packetForClientNode: Byte = 18
    const val packetPlaySound: Byte = 19
    const val packetDestroyUuid: Byte = 20
    const val packetClientToServerConnection: Byte = 21
    const val packetServerToClientInfo: Byte = 22
    const val packetFalstadImport: Byte = 23

    const val gateInputCurrent = 0.00005
    const val gateOutputCurrent = 0.100
    const val LVU = 50.0
    const val MVU = 200.0
    const val HVU = 800.0
    const val VVU = 3200.0
    const val CCU = 120_000.0
    const val SVU = 5.0
    const val signalVoltageAcceptNegative = -0.5
    const val signalVoltageAcceptPositive = SVU + 0.5
    const val SVII = gateInputCurrent / SVU
    const val SVUinv = 1.0 / SVU
    const val SVP = gateOutputCurrent * SVU

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

    lateinit var simulator: Simulator
    lateinit var nodeManager: NodeManager
    lateinit var nodeServer: NodeServer
    lateinit var serverEventListener: ServerEventListener

    lateinit var creativeTabPowerElectronics: GenericCreativeTab
    lateinit var creativeTabCables: GenericCreativeTab
    lateinit var creativeTabPowerDistribution: GenericCreativeTab
    lateinit var creativeTabSignalProcessing: GenericCreativeTab
    lateinit var creativeTabLighting: GenericCreativeTab
    lateinit var creativeTabToolsArmor: GenericCreativeTab
    lateinit var creativeTabOresMaterials: GenericCreativeTab
    lateinit var creativeTabMachines: GenericCreativeTab
    lateinit var creativeTabCreative: GenericCreativeTab
    lateinit var creativeTabOther: GenericCreativeTab

    // Same labels and icons as 1.7.10, so the "itemGroup.<label>" lang keys still apply.
    internal fun createCreativeTabs() {
        creativeTabPowerElectronics = GenericCreativeTab("ElnPowerElectronics") { ItemStack(Items.REDSTONE) }
        creativeTabCables = GenericCreativeTab("ElnCables") { ItemStack(Items.STRING) }
        creativeTabPowerDistribution = GenericCreativeTab("ElnPowerDistribution") { ItemStack(Items.STRING) }
        creativeTabSignalProcessing = GenericCreativeTab("ElnSignalProcessing") { ItemStack(Items.COMPARATOR) }
        creativeTabLighting = GenericCreativeTab("ElnLighting") { ItemStack(Items.REDSTONE_LAMP) }
        creativeTabToolsArmor = GenericCreativeTab("ElnToolsArmor") { ItemStack(Items.IRON_PICKAXE) }
        creativeTabOresMaterials = GenericCreativeTab("ElnOresMaterials") { ItemStack(Items.IRON_INGOT) }
        creativeTabMachines = GenericCreativeTab("ElnMachines") { ItemStack(Items.DISPENSER) }
        creativeTabCreative = GenericCreativeTab("ElnCreative") { ItemStack(Items.NETHER_STAR) }
        creativeTabOther = creativeTabOresMaterials
    }

    internal fun loadConfig() {
        config = JsonConfig(FMLPaths.CONFIGDIR.get().resolve("Eln.cfg").toFile())
        config.loadConfig()
        config.writeExampleFile()
        simulator = Simulator(
            0.05,
            1 / config.getDoubleOrElse("simulation.electrical.frequency", 20.0),
            config.getIntOrElse("simulation.electrical.interSystemOverSampling", 50),
            1 / config.getDoubleOrElse("simulation.thermal.frequency", 400.0)
        )
    }
}
