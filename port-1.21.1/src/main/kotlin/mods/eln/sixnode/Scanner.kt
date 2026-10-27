package mods.eln.sixnode

import mods.eln.compat.*
import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.Direction as McDirection
import net.neoforged.neoforge.capabilities.Capabilities
import net.minecraft.world.level.block.entity.BlockEntity
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*

/**
 * A comparator-alike. It doesn't "compare" anything, though.
 */
class ScannerDescriptor(name: String, obj: Obj3D) : SixNodeDescriptor(name, ScannerElement::class.java, ScannerRender::class.java) {

    val main = obj.getPart("main")!!
    val leds = arrayOf("LED_0", "LED_1").map { obj.getPart(it) }.requireNoNulls()

    init {
        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }

    fun draw(mode: ScanMode) {
        main.draw()
        leds[mode.value.toInt()].draw()
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Scans blocks to produce signals."))
        list.add(tr("- For tanks, outputs fill percentage."))
        // This string sucks. I can't use the normal Java method to fix this problem. TODO: fix this so that it is readable on windowed games.
        list.add(tr("- For inventories, outputs either total fill or fraction of slots with any items."))
        list.add(tr("Right-click to change mode."))
        list.add(tr("Otherwise behaves as a vanilla comparator."))
    }
}

enum class ScanMode(val value: Byte) {
    SIMPLE(0), SLOTS(1);

    companion object {
        private val map = ScanMode.values().associateBy(ScanMode::value);
        fun fromByte(type: Byte) = map[type]
    }
}

class ScannerElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {
    val output = NbtElectricalGateOutput("signal")
    val outputProcess = NbtElectricalGateOutputProcess("signalP", output)

    var mode = ScanMode.SIMPLE

    val updater = IProcess {
        val appliedLRDU = side.applyLRDU(front)
        val scannedCoord = Coordinate(coordinate!!).apply {
            move(appliedLRDU)
        }
        val targetSide = appliedLRDU.inverse.toForge()
        val te = scannedCoord.tileEntity
        // TODO: Throttling.
        var out: Double? = null
        if (te != null) {
            out = scanTileEntity(te, targetSide)
        }
        if (out == null) {
            out = scanBlock(scannedCoord)
        }
        outputProcess.outputNormalized = out
    }

    init {
        electricalLoadList.add(output)
        electricalComponentList.add(outputProcess)
        slowProcessList.add(updater)
    }

    private fun scanBlock(scannedCoord: Coordinate): Double {
        val world = scannedCoord.world()
        val pos = scannedCoord.blockPos
        val state = world.getBlockState(pos)
        return when {
            state.hasAnalogOutputSignal() -> state.getAnalogOutputSignal(world, pos) / 15.0
            state.isAir -> 0.0
            state.isSolidRender(world, pos) -> 1.0
            else -> 1.0 / 3.0
        }
    }

    // 1.20.1 exposes tanks and inventories through Forge capabilities; the 1.7.10 HBM tank reflection is gone with that mod.
    private fun scanTileEntity(te: BlockEntity, targetSide: McDirection): Double? {
        val fluids = te.level?.getCapability(Capabilities.FluidHandler.BLOCK, te.blockPos, targetSide)
        if (fluids != null) {
            val tanks = (0 until fluids.tanks).filter { fluids.getTankCapacity(it) > 0 }
            if (tanks.isEmpty()) return 0.0
            return tanks.sumOf { fluids.getFluidInTank(it).amount.toDouble() / fluids.getTankCapacity(it) } / tanks.size
        }
        val items = te.level?.getCapability(Capabilities.ItemHandler.BLOCK, te.blockPos, targetSide) ?: return null
        if (items.slots == 0) return 0.0
        var sum = 0.0
        var limit = 0.0
        for (slot in 0 until items.slots) {
            val stack = items.getStackInSlot(slot)
            when (mode) {
                ScanMode.SIMPLE -> {
                    sum += stack.count
                    limit += items.getSlotLimit(slot)
                }
                ScanMode.SLOTS -> {
                    if (!stack.isEmpty) sum += 1
                    limit += 1
                }
            }
        }
        return sum / limit
    }

    override fun onBlockActivated(entityPlayer: Player, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (onBlockActivatedRotate(entityPlayer)) return true
        if (Utils.playerHasMeter(entityPlayer)) return false
        mode = when (mode) {
            ScanMode.SIMPLE -> ScanMode.SLOTS
            ScanMode.SLOTS -> ScanMode.SIMPLE
        }
        needPublish()
        return true
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? = output
    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = null

    override fun getConnectionMask(lrdu: LRDU) = when (lrdu) {
        front.inverse() -> NodeBase.maskElectricalOutputGate
        else -> 0
    }

    override fun multiMeterString(): String {
        return "Mode: ${tr(mode.name.lowercase()
            .replaceFirstChar { it.titlecase(Locale.getDefault()) })}, Value: ${Utils.plotPercent("", outputProcess.outputNormalized)}"
    }

    override fun thermoMeterString(): String = ""

    override fun initialize() {
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeByte(mode.value.toInt())
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        nbt.putByte("mode", mode.value)
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        mode = ScanMode.fromByte(nbt.getByte("mode"))!!
    }
}

class ScannerRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(entity, side, descriptor) {
    val desc = descriptor as ScannerDescriptor
    var mode = ScanMode.SIMPLE

    override fun draw() {
        super.draw()
        front!!.glRotateOnX()
        desc.draw(mode)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        mode = ScanMode.fromByte(stream.readByte())!!
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? = Eln.instance.signalCableDescriptor.render
}
