package mods.eln.sixnode.ukfittings

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.RcInterpolator
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * How far right of a wall fitting's centre a player facing it clicked, in blocks. [wall] is the side the fitting is
 * attached to; v* are the hit coordinates inside the block.
 */
fun wallHitRight(wall: Direction, vx: Float, vy: Float, vz: Float): Double {
    val w = DoubleArray(3)
    wall.applyTo(w, 1.0)
    // Facing the wall, the player's right is up x (out of the wall) = (-w.z, 0, w.x).
    return (vx - 0.5) * -w[2] + (vz - 0.5) * w[0]
}

/** Two wireless switches on one UK faceplate: the left and right rockers each send on their own channel. */
class UkDoubleSwitchDescriptor(name: String, obj: Obj3D, val range: Int) :
    SixNodeDescriptor(name, UkDoubleSwitchElement::class.java, UkDoubleSwitchRender::class.java) {
    private val main = obj.getPart("main")
    private val rockers = arrayOf(obj.getPart("rotLeft"), obj.getPart("rotRight"))

    init {
        voltageLevelColor = VoltageLevelColor.SignalVoltage
        setPlaceDirection(arrayOf(Direction.XP, Direction.XN, Direction.ZP, Direction.ZN))
        setDefaultIcon("ukdoubleswitch")
    }

    // Upright on the wall, like the sockets, so left and right are the player's.
    override fun getFrontFromPlace(side: Direction, player: Player): LRDU = LRDU.Down

    fun draw(on: FloatArray) {
        main?.draw()
        for ((i, rocker) in rockers.withIndex()) {
            if (rocker == null) continue
            val off = rocker.getFloat("alphaOff")
            rocker.draw(on[i] * (rocker.getFloat("alphaOn") - off) + off, 0f, 0f, 1f)
        }
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list?.add(tr("Two wireless switches on one plate"))
        list?.add(tr("Each rocker sends on its own channel"))
    }
}

class UkDoubleSwitchElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, descriptor) {
    private val desc = descriptor as UkDoubleSwitchDescriptor

    inner class Gang(var channelName: String) : IWirelessSignalTx {
        var on = false
        override fun getCoordinate(): Coordinate = sixNode!!.coordinate
        override fun getRange(): Int = desc.range
        override fun getChannel(): String = channelName
        override fun getValue(): Double = if (on) 1.0 else 0.0
    }

    val gangs = arrayOf(Gang("Default channel"), Gang("Default channel 2"))

    init {
        gangs.forEach { WirelessSignalTxElement.channelRegister(it) }
    }

    override fun hasGui(): Boolean = true

    override fun onBlockActivated(entityPlayer: Player, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (Utils.isPlayerUsingWrench(entityPlayer)) return false
        val gang = gangs[if (wallHitRight(this.side, vx, vy, vz) > 0) 1 else 0]
        gang.on = !gang.on
        needPublish()
        return true
    }

    private fun setChannel(gang: Gang, channel: String) {
        WirelessSignalTxElement.channelRemove(gang)
        gang.channelName = channel
        WirelessSignalTxElement.channelRegister(gang)
    }

    override fun destroy(entityPlayer: ServerPlayer?) {
        gangs.forEach { WirelessSignalTxElement.channelRemove(it) }
        super.destroy(entityPlayer)
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        gangs.forEachIndexed { i, g ->
            nbt.putString("channel$i", g.channelName)
            nbt.putBoolean("state$i", g.on)
        }
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        gangs.forEachIndexed { i, g ->
            if (nbt.contains("channel$i")) setChannel(g, nbt.getString("channel$i"))
            g.on = nbt.getBoolean("state$i")
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        for (g in gangs) {
            stream.writeUTF(g.channelName)
            stream.writeBoolean(g.on)
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        val id = stream.readByte()
        if (id == SET_LEFT_CHANNEL || id == SET_RIGHT_CHANNEL) {
            setChannel(gangs[if (id == SET_LEFT_CHANNEL) 0 else 1], stream.readUTF())
            needPublish()
        }
    }

    override fun getWaila(): Map<String, String> = mapOf(
        tr("Left") to gangs[0].channelName + ": " + if (gangs[0].on) "§a" + tr("On") else "§c" + tr("Off"),
        tr("Right") to gangs[1].channelName + ": " + if (gangs[1].on) "§a" + tr("On") else "§c" + tr("Off")
    )

    companion object {
        const val SET_LEFT_CHANNEL: Byte = 1
        const val SET_RIGHT_CHANNEL: Byte = 2
    }
}

class UkDoubleSwitchRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity, side, descriptor) {
    private val desc = descriptor as UkDoubleSwitchDescriptor
    val channels = arrayOf("", "")
    private val states = BooleanArray(2)
    private val interpolators = arrayOf(RcInterpolator(0.05f), RcInterpolator(0.05f))
    private val factors = FloatArray(2)

    override fun draw() {
        super.draw()
        for (i in 0..1) factors[i] = interpolators[i].get()
        desc.draw(factors)
    }

    override fun refresh(deltaT: Float) {
        for (i in 0..1) {
            interpolators[i].target = if (states[i]) 1f else 0f
            interpolators[i].step(deltaT)
        }
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? = Eln.instance.signalCableDescriptor.render

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        for (i in 0..1) {
            channels[i] = stream.readUTF()
            states[i] = stream.readBoolean()
        }
    }

    override fun newGuiDraw(side: Direction, player: Player): Screen = UkDoubleSwitchGui(this)
}

class UkDoubleSwitchGui(private val render: UkDoubleSwitchRender) : GuiScreenEln() {
    private lateinit var left: GuiTextFieldEln
    private lateinit var right: GuiTextFieldEln

    override fun initGui() {
        super.initGui()
        left = newGuiTextField(6, 6, 220)
        left.setText(render.channels[0])
        left.setComment(0, tr("Left rocker channel"))
        right = newGuiTextField(6, 24, 220)
        right.setText(render.channels[1])
        right.setComment(0, tr("Right rocker channel"))
    }

    override fun newHelper(): GuiHelper = GuiHelper(this, 220 + 12, 30 + 12)

    override fun guiObjectEvent(guiObject: IGuiObject) {
        when (guiObject) {
            left -> render.clientSetString(UkDoubleSwitchElement.SET_LEFT_CHANNEL, left.text)
            right -> render.clientSetString(UkDoubleSwitchElement.SET_RIGHT_CHANNEL, right.text)
        }
        super.guiObjectEvent(guiObject)
    }
}
