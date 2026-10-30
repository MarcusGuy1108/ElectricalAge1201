package mods.eln.sixnode.powersocket

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.misc.Utils.setGlColorFromDye
import mods.eln.node.six.SixNodeDescriptor
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import mods.eln.client.GL11

class PowerSocketDescriptor(subID: Int, name: String, obj: Obj3D) :
    SixNodeDescriptor(name, PowerSocketElement::class.java, PowerSocketRender::class.java) {
    private var base: Obj3DPart? = null
    private var socket: Obj3DPart? = null
    // The Type G socket's own switch: a rocker, and the red flag it shows when on.
    private var socketSwitch: Obj3DPart? = null
    private var onMark: Obj3DPart? = null
    val hasSwitch get() = socketSwitch != null
    private var nominalVoltage = NominalVoltage.V240
    // The UK face is drawn white rather than in the paint colour, which defaults to a dark grey.
    private var tinted = true

    init {
        base = obj.getPart("SocketBase")
        when (subID) {
            1 -> {
                socket = obj.getPart("Socket50V") // Type J socket - 10 amps specification
                nominalVoltage = NominalVoltage.V240
                voltageLevelColor = VoltageLevelColor.Neutral
                setDefaultIcon("typejsocket")
            }

            2 -> {
                socket = obj.getPart("Socket200V") // Type E socket - 16 amps specification
                nominalVoltage = NominalVoltage.V240
                voltageLevelColor = VoltageLevelColor.Neutral
                setDefaultIcon("typeesocket")
            }

            3 -> {
                socket = obj.getPart("SocketTypeG") // Type G (UK, BS 1363) socket - 13 amps specification
                nominalVoltage = NominalVoltage.V240
                voltageLevelColor = VoltageLevelColor.Neutral
                setDefaultIcon("typegsocket")
                tinted = false
                socketSwitch = obj.getPart("SocketSwitch")
                onMark = obj.getPart("SocketOnMark")
            }
            else -> socket = null
        }
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
    }

    @JvmOverloads
    fun draw(color: Int = 0, switchOn: Float = 1f) {
        if (base != null) base!!.draw()
        if (socket != null) {
            if (tinted) setGlColorFromDye(color, 0.7f, 0.3f)
            socket!!.draw()
            GL11.glColor3f(1f, 1f, 1f)
        }
        socketSwitch?.let { rocker ->
            val off = rocker.getFloat("alphaOff")
            val angle = switchOn * (rocker.getFloat("alphaOn") - off) + off
            rocker.draw(angle, 0f, 0f, 1f)
            if (switchOn > 0.5f) onMark?.draw(angle, 0f, 0f, 1f)
        }
    }

    override fun addInformation(
        itemStack: ItemStack?,
        entityPlayer: Player?,
        list: MutableList<String>?,
        par4: Boolean
    ) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list?.addAll(tr("Supplies any device\nplugged in with energy.").split("\n".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray())
        list?.add(tr("Nominal mains class: %1\$V", Utils.plotValue(nominalVoltage)))
    }

    override fun addRealismContext(list: MutableList<String?>): RealisticEnum {
        super.addRealismContext(list)
        list.add(tr("Homes have power sockets. These are not them."))
        return RealisticEnum.UNREALISTIC
    }

    override fun getFrontFromPlace(side: Direction, player: Player): LRDU? {
        return LRDU.Down
    }
}
