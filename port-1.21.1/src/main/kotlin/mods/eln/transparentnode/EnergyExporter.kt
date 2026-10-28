package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.client.GL11
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.GuiVerticalTrackBar
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.node.NodeBase
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtResistor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 1.7.10's Eln-to-other energy converter (EnergyConverterElnToOther*), which fed IC2, RF and OpenComputers. In 1.20.1
 * Forge Energy replaces RF, so the exporter turns the electrical power its resistor draws into FE, at the 1.7.10 RF
 * ratio, and pushes it into the machines on all six sides (they may also pull it).
 */
class EnergyExporterDescriptor(name: String, val maxPower: Double) :
    TransparentNodeDescriptor(name, EnergyExporterElement::class.java, EnergyExporterRender::class.java) {
    val feperJoule = Eln.config.getDoubleOrElse("balance.integrationConversion.wattsToRf", 1.0 / 3.0 * 4.0)

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list?.add(tr("Converts electrical power into Forge Energy"))
        list?.add(tr("Set its load resistance in its screen"))
    }
}

class EnergyExporterElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElement(node, descriptor), IEnergyStorage {
    private val desc = descriptor as EnergyExporterDescriptor
    val load = NbtElectricalLoad("load")
    val powerInResistor = NbtResistor("powerInResistor", load, null)
    var energyBuffer = 0.0
    var selectedOhms = MnaConst.highImpedance

    init {
        Eln.applySmallRs(load)
        load.setAsPrivate()
        powerInResistor.resistance = MnaConst.highImpedance
        electricalLoadList.add(load)
        electricalComponentList.add(powerInResistor)
        electricalProcessList.add(IProcess { time ->
            val power = powerInResistor.power
            if (power.isFinite()) energyBuffer += power * time
        })
        slowProcessList.add(IProcess { _ ->
            // A full buffer stops drawing power, as in 1.7.10; below 10 ohms means off.
            powerInResistor.resistance = when {
                energyBuffer >= desc.maxPower * 2 || selectedOhms < 10.0 -> MnaConst.highImpedance
                else -> max(Eln.smallRs, selectedOhms)
            }
            pushToNeighbours()
        })
    }

    private fun pushToNeighbours() {
        val level = node!!.coordinate.world()
        val pos = node!!.coordinate.blockPos
        val sinks = net.minecraft.core.Direction.values().mapNotNull { side ->
            level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(side), side.opposite)
                ?.takeIf { it.canReceive() }
        }
        for (sink in sinks) {
            val share = (energyBuffer * desc.feperJoule / sinks.size).toInt()
            if (share <= 0) break
            energyBuffer -= sink.receiveEnergy(share, false) / desc.feperJoule
        }
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
        tr("Power") to Utils.plotPower("", powerInResistor.power),
        tr("Buffer") to Utils.plotEnergy("", energyBuffer)
    )

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(selectedOhms)
        stream.writeDouble(powerInResistor.power)
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        val id = super.networkUnserialize(stream)
        if (id == SET_OHMS) {
            val ohms = stream.readDouble()
            if (ohms in 10.0..MnaConst.highImpedance) selectedOhms = ohms
            needPublish()
        }
        return id
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        nbt.putDouble("energyBuffer", energyBuffer)
        nbt.putDouble("selectedOhms", selectedOhms)
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        energyBuffer = nbt.getDouble("energyBuffer")
        if (nbt.contains("selectedOhms")) selectedOhms = nbt.getDouble("selectedOhms")
    }

    // Forge Energy, for machines and cables that pull instead of waiting to be pushed to.
    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int = 0

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        val extract = max(0, min(maxExtract, (energyBuffer * desc.feperJoule).toInt()))
        if (!simulate) energyBuffer -= extract / desc.feperJoule
        return extract
    }

    override fun getEnergyStored(): Int = (energyBuffer * desc.feperJoule).toInt()

    override fun getMaxEnergyStored(): Int = (desc.maxPower * 2 * desc.feperJoule).toInt()

    override fun canExtract(): Boolean = true

    override fun canReceive(): Boolean = false

    companion object {
        const val SET_OHMS: Byte = 1
    }
}

class EnergyExporterRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElementRender(entity, descriptor) {
    var selectedOhms = MnaConst.highImpedance
    var power = 0.0
    var hasChanges = false

    // 1.7.10 drew a plain block with this texture on every face.
    override fun draw() = drawTexturedCube(TEXTURE)

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        selectedOhms = stream.readDouble()
        power = stream.readDouble()
        hasChanges = true
    }

    override fun newGuiDraw(side: Direction, player: Player): Screen = EnergyExporterGui(this)

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("eln", "textures/blocks/elntoic2lvu_side.png")
    }
}

// Normal, then the corners counter-clockwise from the outside, around the block centre the render sits at.
private val CUBE_FACES = arrayOf(
    arrayOf(floatArrayOf(0f, 1f, 0f), floatArrayOf(-.5f, .5f, -.5f), floatArrayOf(-.5f, .5f, .5f), floatArrayOf(.5f, .5f, .5f), floatArrayOf(.5f, .5f, -.5f)),
    arrayOf(floatArrayOf(0f, -1f, 0f), floatArrayOf(-.5f, -.5f, .5f), floatArrayOf(-.5f, -.5f, -.5f), floatArrayOf(.5f, -.5f, -.5f), floatArrayOf(.5f, -.5f, .5f)),
    arrayOf(floatArrayOf(0f, 0f, -1f), floatArrayOf(.5f, .5f, -.5f), floatArrayOf(.5f, -.5f, -.5f), floatArrayOf(-.5f, -.5f, -.5f), floatArrayOf(-.5f, .5f, -.5f)),
    arrayOf(floatArrayOf(0f, 0f, 1f), floatArrayOf(-.5f, .5f, .5f), floatArrayOf(-.5f, -.5f, .5f), floatArrayOf(.5f, -.5f, .5f), floatArrayOf(.5f, .5f, .5f)),
    arrayOf(floatArrayOf(-1f, 0f, 0f), floatArrayOf(-.5f, .5f, -.5f), floatArrayOf(-.5f, -.5f, -.5f), floatArrayOf(-.5f, -.5f, .5f), floatArrayOf(-.5f, .5f, .5f)),
    arrayOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(.5f, .5f, .5f), floatArrayOf(.5f, -.5f, .5f), floatArrayOf(.5f, -.5f, -.5f), floatArrayOf(.5f, .5f, -.5f))
)


/** A unit cube around the block centre the render sits at, with [texture] on every face. */
fun drawTexturedCube(texture: ResourceLocation) {
    UtilsClient.bindTexture(texture)
    GL11.glBegin(GL11.GL_QUADS)
    for (face in CUBE_FACES) {
        GL11.glNormal3f(face[0][0], face[0][1], face[0][2])
        for ((i, corner) in face.drop(1).withIndex()) {
            GL11.glTexCoord2f(if (i == 1 || i == 2) 1f else 0f, if (i >= 2) 1f else 0f)
            GL11.glVertex3f(corner[0], corner[1], corner[2])
        }
    }
    GL11.glEnd()
}

class EnergyExporterGui(private val render: EnergyExporterRender) : GuiScreenEln() {
    private lateinit var resistanceSelector: GuiVerticalTrackBar
    private lateinit var resistanceEntry: GuiTextFieldEln
    private val maxResistance = 100_000.0

    override fun initGui() {
        super.initGui()
        resistanceSelector = newGuiVerticalTrackBar(6, 8, 20, 82)
        resistanceSelector.setStepIdMax(100)
        resistanceSelector.setEnable(true)
        resistanceSelector.setRange(0f, 1f)
        resistanceEntry = newGuiTextField(6, 96, 70)
        resistanceEntry.setComment(arrayOf(tr("Select the resistance")))
        sync()
    }

    private fun sync() {
        resistanceSelector.value = log(render.selectedOhms / 100 + 1, maxResistance / 100 + 1).toFloat().coerceIn(0f, 1f)
        resistanceEntry.text = if (render.selectedOhms >= MnaConst.highImpedance) "" else render.selectedOhms.toInt().toString()
        render.hasChanges = false
    }

    private fun sliderToOhms(slider: Float): Double = 100 * ((maxResistance / 100 + 1).pow(slider.toDouble()) - 1)

    override fun guiObjectEvent(guiObject: IGuiObject) {
        super.guiObjectEvent(guiObject)
        val ohms = when {
            guiObject === resistanceSelector -> sliderToOhms(resistanceSelector.value)
            guiObject === resistanceEntry -> resistanceEntry.text.toDoubleOrNull() ?: return
            else -> return
        }
        render.selectedOhms = ohms
        render.clientSendDouble(EnergyExporterElement.SET_OHMS, ohms)
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        if (render.hasChanges) sync()
        resistanceSelector.setComment(0, Utils.plotOhm(render.selectedOhms))
    }

    override fun postDraw(f: Float, x: Int, y: Int) {
        super.postDraw(f, x, y)
        drawString(32, 12, tr("Power:"))
        drawString(32, 22, Utils.plotPower("", render.power))
    }

    override fun newHelper(): GuiHelper = GuiHelper(this, 82, 115)
}
