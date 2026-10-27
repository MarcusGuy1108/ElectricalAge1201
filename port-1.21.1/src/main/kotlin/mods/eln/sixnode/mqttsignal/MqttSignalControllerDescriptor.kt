package mods.eln.sixnode.mqttsignal

import mods.eln.compat.*
import mods.eln.misc.Obj3D
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.client.GL11

class MqttSignalControllerDescriptor(name: String?, private val obj: Obj3D?) :
    SixNodeDescriptor(name, MqttSignalControllerElement::class.java, MqttSignalControllerRender::class.java) {

    private val main: Obj3DPart?
    private val door: Obj3DPart?
    private val leds: Array<Obj3DPart?>
    private val alphaOff: Float
    val pinDistance: FloatArray?

    init {
        var doorAlpha = 0f
        val ledParts = Array(8) { obj?.getPart("led$it") }
        if (obj != null) {
            main = obj.getPart("main")
            door = obj.getPart("door")
            if (door != null) {
                doorAlpha = door.getFloat("alphaOff")
            }
        } else {
            main = null
            door = null
        }
        leds = ledParts
        alphaOff = doorAlpha
        pinDistance = main?.let { Utils.getSixNodePinDistance(it) }
        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }

    // PORT: setParent registers with the in-game wiki (Data.addSignal) once mods.eln.wiki is ported.

    fun draw(open: Float, ledState: BooleanArray) {
        main?.draw()
        door?.draw((1f - open) * alphaOff, 0f, 1f, 0f)
        leds.forEachIndexed { index, led ->
            led ?: return@forEachIndexed
            if (index < ledState.size && ledState[index]) {
                if ((index and 3) == 0) {
                    GL11.glColor3f(0.8f, 0f, 0f)
                } else {
                    GL11.glColor3f(0f, 0.8f, 0f)
                }
                UtilsClient.drawLight(led)
            } else {
                GL11.glColor3f(0.3f, 0.3f, 0.3f)
                led.draw()
            }
        }
        GL11.glColor3f(1f, 1f, 1f)
    }

    // PORT: IItemRenderer drawing is replaced by the generated item model.

}
