package mods.eln.sixnode.lampsocket

import mods.eln.i18n.I18N
import mods.eln.item.lampitem.BoilerplateLampData
import mods.eln.misc.RealisticEnum
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.sixnode.lampsocket.objrender.ILampSocketObjRender
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.Collections
import kotlin.text.split

class LampSocketDescriptor(itemName: String, val renderType: ILampSocketObjRender, val range: Int, val acceptedLampTypes: Array<BoilerplateLampData>) :
    SixNodeDescriptor(itemName, LampSocketElement::class.java, LampSocketRender::class.java) {

    var paintable: Boolean = false
    var enableProjectionRotation: Boolean = false
    var initialRenderAngleOffset = 0.0
    var renderSideCables = true
    var extendedRenderBounds = false

    private var acceptedLampTypesString: String = ""

    init {
        voltageLevelColor = VoltageLevelColor.Neutral

        for (lampData in acceptedLampTypes) {
            acceptedLampTypesString += lampData.translatedLampType
            if (acceptedLampTypes.indexOf(lampData) < (acceptedLampTypes.size - 1)) acceptedLampTypesString += "/"
        }
    }

    // PORT: setParent registers with the in-game wiki (Data.addLight) once mods.eln.wiki is ported. The 1.7.10
    // IItemRenderer drew the socket model in hand; 1.20.1 uses the generated icon model everywhere.

    override fun hasVolume(): Boolean {
        return hasGhostGroup()
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        if (range != 0) list.add(I18N.tr("Spot range: %1$ blocks", range))
        if (enableProjectionRotation) list.add(I18N.tr("Angle: %1$\u00B0 to %2$\u00B0", LampSocketGui.MIN_ROTATION_ANGLE.toInt(), LampSocketGui.MAX_ROTATION_ANGLE.toInt()))
        list.add(I18N.tr("Accepted lamp types: %1$", acceptedLampTypesString))
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        super.addRealismContext(list)

        Collections.addAll(list!!, *I18N.tr("Wireless mode of lights is intended \nto pretend wires are in the walls.")!!
            .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

        return RealisticEnum.REALISTIC
    }

}