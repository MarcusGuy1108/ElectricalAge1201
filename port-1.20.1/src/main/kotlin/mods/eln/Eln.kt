package mods.eln

import mods.eln.misc.FunctionTable
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeBlock
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamage
import com.mojang.logging.LogUtils
import mods.eln.cable.CableRenderDescriptor
import mods.eln.config.JsonConfig
import mods.eln.generic.GenericCreativeTab
import mods.eln.generic.GenericItemBlockUsingDamage
import mods.eln.misc.Obj3DFolder
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.node.six.SixNodeBlock
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeBlockRegistration
import mods.eln.node.NodeManager
import mods.eln.node.NodeServer
import mods.eln.server.ServerEventListener
import mods.eln.sim.Simulator
import mods.eln.sim.ThermalLoadInitializer
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
    const val cableHeatingTime = 30.0
    const val cableWarmLimit = 130.0
    const val cableThermalConductionTao = 0.5

    @JvmField
    val cableThermalLoadInitializer = ThermalLoadInitializer(cableWarmLimit, -100.0, cableHeatingTime, cableThermalConductionTao)
    @JvmField
    val sixNodeThermalLoadInitializer = ThermalLoadInitializer(cableWarmLimit, -100.0, cableHeatingTime, 1000.0)

    // 1.7.10 code reaches these through Eln.instance; the object is its own instance.
    @JvmField
    val instance: Eln = this

    fun LVP(): Double = 1000 * config.getDoubleOrElse("balance.cables.powerFactor", 1.0)
    fun MVP(): Double = 2000 * config.getDoubleOrElse("balance.cables.powerFactor", 1.0)
    fun HVP(): Double = 5000 * config.getDoubleOrElse("balance.cables.powerFactor", 1.0)
    fun VVP(): Double = 15000 * config.getDoubleOrElse("balance.cables.powerFactor", 1.0)

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

    @JvmStatic
    val smallRs: Double
        get() = lowVoltageCableDescriptor!!.electricalRs

    @JvmStatic
    fun applySmallRs(aLoad: NbtElectricalLoad) {
        lowVoltageCableDescriptor!!.applyTo(aLoad)
    }

    @JvmStatic
    fun applySmallRs(r: Resistor) {
        lowVoltageCableDescriptor!!.applyTo(r)
    }

    lateinit var sixNodeItem: GenericItemBlockUsingDamage<SixNodeDescriptor>
    @JvmField
    var batteryVoltageFunctionTable: FunctionTable? = null
    lateinit var transparentNodeItem: GenericItemBlockUsingDamage<TransparentNodeDescriptor>
    val transparentNodeBlock: TransparentNodeBlock
        @JvmStatic get() = TransparentNodeBlock.BLOCK.get()
    lateinit var sharedItem: GenericItemUsingDamage<GenericItemUsingDamageDescriptor>

    @JvmField val obj = Obj3DFolder()

    @JvmField var stdCableRenderSignal: CableRenderDescriptor? = null
    @JvmField var stdCableRender50V: CableRenderDescriptor? = null
    @JvmField var stdCableRender200V: CableRenderDescriptor? = null
    @JvmField var stdCableRender800V: CableRenderDescriptor? = null
    @JvmField var stdCableRender3200V: CableRenderDescriptor? = null
    @JvmField var stdCableRenderCreative: CableRenderDescriptor? = null
    @JvmField var signalCableDescriptor: ElectricalCableDescriptor? = null
    @JvmField var lowVoltageCableDescriptor: ElectricalCableDescriptor? = null
    @JvmField var meduimVoltageCableDescriptor: ElectricalCableDescriptor? = null
    @JvmField var highVoltageCableDescriptor: ElectricalCableDescriptor? = null
    @JvmField var veryHighVoltageCableDescriptor: ElectricalCableDescriptor? = null
    @JvmField var creativeCableDescriptor: ElectricalCableDescriptor? = null
    val sixNodeBlock: SixNodeBlock
        @JvmStatic get() = SixNodeBlockRegistration.BLOCK.get()

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
