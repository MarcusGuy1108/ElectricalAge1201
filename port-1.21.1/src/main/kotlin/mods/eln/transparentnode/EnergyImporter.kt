package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.GuiVerticalTrackBar
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtResistor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.energy.IEnergyStorage
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The reverse of the [EnergyExporterDescriptor]: other mods push Forge Energy in (or have it pulled by their cables),
 * and the importer drives its Eln side at the voltage set in its screen, paying for each joule from that buffer at the
 * same FE per joule ratio as the exporter. Most power mods speak FE or convert to it (Mekanism, Thermal, Create
 * addons, GregTech's converters), so FE is the one input.
 */
class EnergyImporterDescriptor(name: String, val maxPower: Double) :
    TransparentNodeDescriptor(name, EnergyImporterElement::class.java, EnergyImporterRender::class.java) {
    val fePerJoule = Eln.config.getDoubleOrElse("balance.integrationConversion.wattsToRf", 1.0 / 3.0 * 4.0)
    val maxVoltage = 3200.0
    // Ohms between the ideal source and the cables: a stiff supply that still lets a short circuit burn the cable.
    val outputResistance = 0.1

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list?.add(tr("Converts Forge Energy into electrical power"))
        list?.add(tr("Set its output voltage in its screen"))
    }
}

class EnergyImporterElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElement(node, descriptor), IEnergyStorage {
    private val desc = descriptor as EnergyImporterDescriptor
    val load = NbtElectricalLoad("load")
    private val inner = NbtElectricalLoad("inner")
    private val source = VoltageSource("source", inner, null)
    private val output = NbtResistor("output", inner, load)
    var energyBuffer = 0.0
    var selectedVoltage = 0.0
    private val maxBuffer get() = desc.maxPower * 2

    /** Watts going out into the cables; negative when another supply pushes back in, which is wasted. */
    val power get() = source.voltage * source.current

    init {
        Eln.applySmallRs(load)
        load.setAsPrivate()
        Eln.applySmallRs(inner)
        inner.setAsPrivate()
        output.resistance = MnaConst.highImpedance
        electricalLoadList.add(load)
        electricalLoadList.add(inner)
        electricalComponentList.add(source)
        electricalComponentList.add(output)
        electricalProcessList.add(IProcess { time ->
            if (power.isFinite()) energyBuffer -= max(0.0, power) * time
        })
        slowProcessList.add(IProcess { _ ->
            // Switching per tick, not per sim step: the buffer may dip below zero for one tick, which the next FE repays.
            val on = selectedVoltage > 0.0 && energyBuffer > 0.0
            source.setVoltage(if (on) selectedVoltage else 0.0)
            output.resistance = if (on) desc.outputResistance else MnaConst.highImpedance
        })
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int = NodeBase.maskElectricalPower

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad = load

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? = null

    override fun initialize() {
        connect()
    }

    override fun hasGui(): Boolean = true

    override fun getWaila(): Map<String, String> = mapOf(
        tr("Voltage") to Utils.plotVolt("", load.voltage),
        tr("Power") to Utils.plotPower("", power),
        tr("Buffer") to Utils.plotEnergy("", energyBuffer)
    )

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(selectedVoltage)
        stream.writeDouble(power)
        stream.writeDouble(energyBuffer / maxBuffer)
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        val id = super.networkUnserialize(stream)
        if (id == SET_VOLTAGE) {
            val volts = stream.readDouble()
            if (volts.isFinite()) selectedVoltage = volts.coerceIn(0.0, desc.maxVoltage)
            needPublish()
        }
        return id
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        nbt.putDouble("energyBuffer", energyBuffer)
        nbt.putDouble("selectedVoltage", selectedVoltage)
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        energyBuffer = nbt.getDouble("energyBuffer")
        selectedVoltage = nbt.getDouble("selectedVoltage")
    }

    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        val accepted = max(0, min(maxReceive, ((maxBuffer - energyBuffer) * desc.fePerJoule).toInt()))
        if (!simulate) energyBuffer += accepted / desc.fePerJoule
        return accepted
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int = 0

    override fun getEnergyStored(): Int = (max(0.0, energyBuffer) * desc.fePerJoule).toInt()

    override fun getMaxEnergyStored(): Int = (maxBuffer * desc.fePerJoule).toInt()

    override fun canExtract(): Boolean = false

    override fun canReceive(): Boolean = true

    companion object {
        const val SET_VOLTAGE: Byte = 1
    }
}

class EnergyImporterRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElementRender(entity, descriptor) {
    val maxVoltage = (descriptor as EnergyImporterDescriptor).maxVoltage
    var selectedVoltage = 0.0
    var power = 0.0
    var bufferFill = 0.0
    var hasChanges = false

    override fun draw() = drawTexturedCube(TEXTURE)

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        selectedVoltage = stream.readDouble()
        power = stream.readDouble()
        bufferFill = stream.readDouble()
        hasChanges = true
    }

    override fun newGuiDraw(side: Direction, player: Player): Screen = EnergyImporterGui(this)

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("eln", "textures/blocks/elntoic2lvu_ic2.png")
    }
}

class EnergyImporterGui(private val render: EnergyImporterRender) : GuiScreenEln() {
    private lateinit var voltageSelector: GuiVerticalTrackBar
    private lateinit var voltageEntry: GuiTextFieldEln

    override fun initGui() {
        super.initGui()
        voltageSelector = newGuiVerticalTrackBar(6, 8, 20, 82)
        voltageSelector.setStepIdMax(100)
        voltageSelector.setEnable(true)
        voltageSelector.setRange(0f, 1f)
        voltageEntry = newGuiTextField(6, 96, 70)
        voltageEntry.setComment(arrayOf(tr("Select the output voltage")))
        sync()
    }

    private fun sync() {
        voltageSelector.value = log(render.selectedVoltage / 10 + 1, render.maxVoltage / 10 + 1).toFloat().coerceIn(0f, 1f)
        voltageEntry.text = render.selectedVoltage.toInt().toString()
        render.hasChanges = false
    }

    // Logarithmic, so the low voltages lamps use get most of the slider.
    private fun sliderToVolts(slider: Float): Double = 10 * ((render.maxVoltage / 10 + 1).pow(slider.toDouble()) - 1)

    override fun guiObjectEvent(guiObject: IGuiObject) {
        super.guiObjectEvent(guiObject)
        val volts = when {
            guiObject === voltageSelector -> sliderToVolts(voltageSelector.value)
            guiObject === voltageEntry -> voltageEntry.text.toDoubleOrNull() ?: return
            else -> return
        }.coerceIn(0.0, render.maxVoltage)
        render.selectedVoltage = volts
        render.clientSendDouble(EnergyImporterElement.SET_VOLTAGE, volts)
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        if (render.hasChanges) sync()
        voltageSelector.setComment(0, Utils.plotVolt("", render.selectedVoltage))
    }

    override fun postDraw(f: Float, x: Int, y: Int) {
        super.postDraw(f, x, y)
        drawString(32, 12, tr("Power:"))
        drawString(32, 22, Utils.plotPower("", render.power))
        drawString(32, 38, tr("Buffer:"))
        drawString(32, 48, Utils.plotPercent("", render.bufferFill))
    }

    override fun newHelper(): GuiHelper = GuiHelper(this, 82, 115)
}
