package mods.eln

import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor
import mods.eln.misc.RecipesList
import mods.eln.fluid.FuelRegistry
import net.minecraftforge.fml.loading.FMLEnvironment
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostManager
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor
import mods.eln.misc.WindProcess
import mods.eln.item.BrushDescriptor
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.item.ArithmeticLogicUnit
import mods.eln.item.NibbleMemory
import mods.eln.item.Thermistor
import mods.eln.item.Transistor
import mods.eln.item.SiliconWafer
import mods.eln.item.TreeResin
import mods.eln.item.CopperCableDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment
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
    // 1.7.10 OreDictionary registrations; the data generator turns them into item tags (see ElnItemTags).
    @JvmField
    val oreDictionary = LinkedHashMap<String, MutableList<GenericItemUsingDamageDescriptor>>()

    @JvmStatic
    fun addToOre(name: String?, descriptor: GenericItemUsingDamageDescriptor) {
        if (name == null) return
        oreDictionary.getOrPut(name) { ArrayList() }.add(descriptor)
    }

    @JvmField var copperIngot: GenericItemUsingDamageDescriptorWithComment? = null
    @JvmField var plumbIngot: GenericItemUsingDamageDescriptorWithComment? = null
    @JvmField var tungstenIngot: GenericItemUsingDamageDescriptorWithComment? = null
    @JvmField var dustCopper: GenericItemUsingDamageDescriptorWithComment? = null
    @JvmField var plateCopper: GenericItemUsingDamageDescriptorWithComment? = null
    lateinit var copperCableDescriptor: CopperCableDescriptor
    @JvmField var treeResin: TreeResin? = null
    lateinit var siliconWafer: SiliconWafer
    lateinit var transistor: Transistor
    lateinit var thermistor: Thermistor
    lateinit var nibbleMemory: NibbleMemory
    lateinit var alu: ArithmeticLogicUnit
    // Never set in 1.7.10 either, so these chips have no dictionary name.
    @JvmField var dictSiliconWafer: String? = null
    @JvmField var dictTransistor: String? = null
    @JvmField var dictThermistor: String? = null
    @JvmField var dictNibbleMemory: String? = null
    @JvmField var dictALU: String? = null
    @JvmField var multiMeterElement: GenericItemUsingDamageDescriptor? = null
    @JvmField var thermometerElement: GenericItemUsingDamageDescriptor? = null
    @JvmField var allMeterElement: GenericItemUsingDamageDescriptor? = null
    @JvmField var wrenchDescriptor: GenericItemUsingDamageDescriptor? = null
    @JvmField var configCopyToolElement: GenericItemUsingDamageDescriptor? = null
    @JvmField var whiteDesc: BrushDescriptor? = null
    lateinit var dataLogsPrintDescriptor: DataLogsPrintDescriptor
    @JvmField var brushSubNames: List<String> = emptyList()

    @JvmStatic lateinit var wind: WindProcess

    // True in a development (deobfuscated) run, like 1.7.10's fml.deobfuscatedEnvironment.
    val isDevelopmentRun: Boolean
        @JvmStatic get() = !FMLEnvironment.production
    @JvmField val ghostManager = GhostManager()
    @JvmField val furnaceList = ArrayList<ItemStack>()
    lateinit var electricalFurnace: ElectricalFurnaceDescriptor
    @JvmField val maceratorRecipes = RecipesList()
    @JvmField val compressorRecipes = RecipesList()
    @JvmField val plateMachineRecipes = RecipesList()
    @JvmField val arcFurnaceRecipes = RecipesList()
    @JvmField val magnetiserRecipes = RecipesList()
    val ghostBlock: GhostBlock
        @JvmStatic get() = GhostBlock.BLOCK.get()
    lateinit var transparentNodeItem: GenericItemBlockUsingDamage<TransparentNodeDescriptor>
    val transparentNodeBlock: TransparentNodeBlock
        @JvmStatic get() = TransparentNodeBlock.BLOCK.get()
    lateinit var sharedItem: GenericItemUsingDamage<GenericItemUsingDamageDescriptor>
    // For items that keep per-stack state in NBT (turbine blades), which must not stack.
    lateinit var sharedItemStackOne: GenericItemUsingDamage<GenericItemUsingDamageDescriptor>

    @JvmField val obj = Obj3DFolder()

    @JvmField var stdCableRenderSignal: CableRenderDescriptor? = null
    @JvmField var stdCableRender50V: CableRenderDescriptor? = null
    @JvmField var stdCableRender200V: CableRenderDescriptor? = null
    @JvmField var stdCableRender800V: CableRenderDescriptor? = null
    @JvmField var stdCableRender3200V: CableRenderDescriptor? = null
    @JvmField var stdCableRenderCreative: CableRenderDescriptor? = null
    @JvmField var lowCurrentCableRender: CableRenderDescriptor? = null
    @JvmField var mediumCurrentCableRender: CableRenderDescriptor? = null
    @JvmField var highCurrentCableRender: CableRenderDescriptor? = null
    lateinit var lowCurrentCableDescriptor: CurrentCableDescriptor
    lateinit var mediumCurrentCableDescriptor: CurrentCableDescriptor
    lateinit var highCurrentCableDescriptor: CurrentCableDescriptor
    lateinit var signalCableDescriptor: ElectricalCableDescriptor
    lateinit var lowVoltageCableDescriptor: ElectricalCableDescriptor
    lateinit var meduimVoltageCableDescriptor: ElectricalCableDescriptor
    lateinit var highVoltageCableDescriptor: ElectricalCableDescriptor
    lateinit var veryHighVoltageCableDescriptor: ElectricalCableDescriptor
    lateinit var creativeCableDescriptor: ElectricalCableDescriptor
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
        FuelRegistry.init(FMLPaths.CONFIGDIR.get().resolve("Eln.cfg").toFile())
        simulator = Simulator(
            0.05,
            1 / config.getDoubleOrElse("simulation.electrical.frequency", 20.0),
            config.getIntOrElse("simulation.electrical.interSystemOverSampling", 50),
            1 / config.getDoubleOrElse("simulation.thermal.frequency", 400.0)
        )
    }
}
