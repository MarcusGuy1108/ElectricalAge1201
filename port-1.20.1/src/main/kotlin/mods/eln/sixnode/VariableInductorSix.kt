package mods.eln.sixnode

import mods.eln.compat.*
import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.IGuiObject
import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.item.FerromagneticCoreDescriptor
import mods.eln.misc.BasicContainer
import mods.eln.misc.Direction
import mods.eln.misc.IFunction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.RealisticEnum
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Inductor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.Container
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import mods.eln.client.GL11
import java.util.HashMap
import kotlin.math.abs
import kotlin.math.roundToInt

class VariableInductorSixDescriptor(
    name: String,
    private val obj: Obj3D,
    private val serie: IFunction,
    val maxCableCount: Int,
    val defaultControlNormalized: Double = 1.0
) : SixNodeDescriptor(name, VariableInductorSixElement::class.java, VariableInductorSixRender::class.java) {
    private val inductorBaseExtension = obj.getPart("InductorBaseExtention")
    private val inductorCables = obj.getPart("InductorCables")
    private val inductorCore = obj.getPart("InductorCore")
    private val base = obj.getPart("Base")

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
    }

    fun draw() {
        base?.draw()
        inductorBaseExtension?.draw()
        inductorCables?.draw()
        inductorCore?.draw()
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        super.addRealismContext(list)
        list?.add(tr("It doesn't really behave well for DC"))
        list?.add(tr("* Missing an inductive voltage spike on field collapse"))
        return RealisticEnum.UNREALISTIC
    }

    override fun getFrontFromPlace(side: Direction, player: Player): LRDU {
        return super.getFrontFromPlace(side, player)!!.left()
    }

    fun getRsValue(inventory: Container): Double {
        val core = inventory.getStackInSlot(VariableInductorSixContainer.coreId)
            ?: return MnaConst.highImpedance
        val coreDescriptor =
            GenericItemUsingDamageDescriptor.getDescriptor(core) as? FerromagneticCoreDescriptor
                ?: return MnaConst.highImpedance
        val coreFactor = coreDescriptor.cableMultiplicator
        return Eln.instance.lowVoltageCableDescriptor.electricalRs * coreFactor
    }

    private fun getlValue(cableCount: Int): Double {
        if (cableCount <= 0) return 0.0
        return serie.getValue((cableCount - 1).toDouble())
    }

    fun inductanceFromControl(control: Double): Double {
        val normalized = control.coerceIn(0.0, 1.0)
        if (maxCableCount <= 0) {
            return getlValue(1)
        }
        val availableTurns = (maxCableCount - 1).coerceAtLeast(0)
        val equivalentTurns = (normalized * availableTurns).roundToInt() + 1
        return getlValue(equivalentTurns.coerceIn(1, maxCableCount))
    }

    fun maxInductance(): Double = getlValue(maxCableCount.coerceAtLeast(1))

    fun minInductance(): Double = getlValue(1)
}

class VariableInductorSixElement(
    sixNode: SixNode,
    side: Direction,
    descriptor: SixNodeDescriptor
) : SixNodeElement(sixNode, side, descriptor) {
    private val descriptor = descriptor as VariableInductorSixDescriptor
    private val positiveLoad = NbtElectricalLoad("positiveLoad")
    private val negativeLoad = NbtElectricalLoad("negativeLoad")
    private val controlLoad = NbtElectricalGateInput("controlLoad")
    private val inductor = Inductor("inductor", positiveLoad, negativeLoad)
    private val controlProcess = ControlProcess()
    private var fromNbt = false

    override var inventory = SixNodeElementInventory(1, 64, this)

    init {
        electricalLoadList.add(positiveLoad)
        electricalLoadList.add(negativeLoad)
        electricalLoadList.add(controlLoad)
        electricalComponentList.add(inductor)
        slowProcessList.add(controlProcess)
        positiveLoad.setAsMustBeFarFromInterSystem()
    }

    inner class ControlProcess : IProcess {
        private var lastInductance = -1.0

        override fun process(time: Double) {
            update(false)
        }

        fun forceUpdate() {
            update(true)
        }

        private fun update(force: Boolean) {
            val normalized = controlNormalized()
            val newInductance = descriptor.inductanceFromControl(normalized)
            if (force || lastInductance < 0 || abs(newInductance - lastInductance) > lastInductance * 0.01) {
                inductor.inductance = newInductance
                lastInductance = newInductance
                needPublish()
            }
        }
    }

    private fun controlNormalized(): Double {
        return if (controlLoad.connectedComponents.isEmpty()) {
            descriptor.defaultControlNormalized
        } else {
            controlLoad.normalized
        }
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        return when (lrdu) {
            front.right() -> positiveLoad
            front.left() -> negativeLoad
            front -> controlLoad
            else -> null
        }
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return when (lrdu) {
            front.right(), front.left() -> NodeBase.maskElectricalPower
            front -> NodeBase.maskElectricalInputGate
            else -> 0
        }
    }

    override fun multiMeterString(): String =
        Utils.plotVolt("U", abs(inductor.voltage)) +
            Utils.plotAmpere("I", inductor.current) +
            Utils.plotValue(inductor.inductance, "H") + " " +
            Utils.plotPercent("Ctl", controlNormalized())

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        info[tr("Inductance")] = Utils.plotValue(inductor.inductance, "H")
        info[tr("Charge")] = Utils.plotEnergy("", inductor.energy)
        info[tr("Control")] = Utils.plotPercent("", controlNormalized())
        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false)) {
            info[tr("Voltage drop")] = Utils.plotVolt("", abs(inductor.voltage))
            info[tr("Current")] = Utils.plotAmpere("", abs(inductor.current))
        }
        return info
    }

    override fun thermoMeterString(): String = ""

    override fun initialize() {
        setupPhysical()
    }

    public override fun inventoryChanged() {
        super.inventoryChanged()
        setupPhysical()
    }

    private fun setupPhysical() {
        val rs = descriptor.getRsValue(inventory)
        positiveLoad.serialResistance = rs
        negativeLoad.serialResistance = rs
        if (fromNbt) {
            fromNbt = false
        } else {
            inductor.resetStates()
        }
        controlProcess.forceUpdate()
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        fromNbt = true
    }

    override fun hasGui(): Boolean = true

    override fun newContainer(side: Direction, player: Player): AbstractContainerMenu {
        return VariableInductorSixContainer(player, inventory)
    }
}

class VariableInductorSixRender(
    tileEntity: SixNodeEntity,
    side: Direction,
    descriptor: SixNodeDescriptor
) : SixNodeElementRender(tileEntity, side, descriptor) {
    @JvmField
    var descriptor = descriptor as VariableInductorSixDescriptor

    override var inventory = SixNodeElementInventory(1, 64, this)

    override fun draw() {
        super.draw()
        front!!.left().glRotateOnX()
        descriptor.draw()
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return when (lrdu) {
            front -> Eln.instance.signalCableDescriptor.render
            front!!.right(), front!!.left() -> Eln.instance.lowVoltageCableDescriptor.render
            else -> null
        }
    }

    override fun newGuiDraw(side: Direction, player: Player): Screen {
        return VariableInductorSixGui(player, inventory)
    }
}

class VariableInductorSixGui(
    player: Player,
    inventory: Container
) : GuiContainerEln(VariableInductorSixContainer(player, inventory)) {
    override fun guiObjectEvent(`object`: IGuiObject) {
        super.guiObjectEvent(`object`)
    }

    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 176, 166 - 54, 8, 84 - 54)
    }
}

class VariableInductorSixContainer(player: Player, inventory: Container) : BasicContainer(
    player,
    inventory,
    arrayOf(
        GenericItemUsingDamageSlot(
            inventory,
            coreId,
            132,
            8,
            1,
            FerromagneticCoreDescriptor::class.java,
            ISlotSkin.SlotSkin.medium,
            arrayOf(tr("Ferromagnetic core slot"))
        )
    )
) {
    companion object {
        const val coreId = 0
    }
}
