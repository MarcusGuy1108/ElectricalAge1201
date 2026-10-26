package mods.eln.sixnode.thermometersensor

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.six.SixNodeDescriptor
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import mods.eln.client.GL11

class ThermometerSensorDescriptor(name: String, private val obj: Obj3D?) : SixNodeDescriptor(
    name,
    ThermometerSensorElement::class.java,
    ThermometerSensorRender::class.java
) {
    private val main = obj?.getPart("main")
    val pinDistance: FloatArray? = main?.let { Utils.getSixNodePinDistance(it) }

    init {
        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }

    fun draw() {
        UtilsClient.disableCulling()
        main?.draw()
        UtilsClient.enableCulling()
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.addAll(tr("Provides an electrical signal\nfor biome temperature.").split("\n"))
        list.addAll(tr("Set min/max values in GUI\nto map temperature to output.").split("\n"))
        list.add(tr("Default range: -40C to 50C"))
        list.add(tr("0%% output: %1\$V", 0))
        list.add(tr("100%% output: %1\$V", Utils.plotValue(Eln.SVU)))
    }

    override fun getFrontFromPlace(side: Direction, player: Player): LRDU {
        return super.getFrontFromPlace(side, player)!!.right()
    }
}
