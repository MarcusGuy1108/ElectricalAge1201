package mods.eln.item

import mods.eln.Eln
import mods.eln.misc.Obj3D
import mods.eln.misc.VoltageLevelColor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import mods.eln.client.GL11

class ElectricalFuseDescriptor(
    name: String,
    val cableDescriptor: ElectricalCableDescriptor?,
    obj: Obj3D?,
    val currentLimit: Double? = null
) :
    GenericItemUsingDamageDescriptorUpgrade(name) {

    companion object {
        var BlownFuse: ElectricalFuseDescriptor? = null
    }

    private val fuseType = obj?.getPart("FuseType")
    private val fuseOk = obj?.getPart("FuseOk")
    private val fuse = obj?.getPart("Fuse")

    init {
        if (isUsable()) {
            setDefaultIcon("electricalfuse")
            voltageLevelColor = VoltageLevelColor.fromCable(cableDescriptor)
        } else {
            setDefaultIcon("blownelectricalfuse")
            voltageLevelColor = VoltageLevelColor.Neutral
        }
    }

    fun isUsable(): Boolean = cableDescriptor != null || currentLimit != null

    fun resistanceOhms(): Double = cableDescriptor?.electricalRs?.times(2.0) ?: Eln.smallRs * 2.0

    fun warmLimitCelsius(): Double = cableDescriptor?.thermalWarmLimit ?: Eln.cableWarmLimit

    fun thermalRp(): Double {
        val limit = currentLimit ?: return cableDescriptor?.thermalRp ?: Double.POSITIVE_INFINITY
        return warmLimitCelsius() / (limit * limit * resistanceOhms())
    }

    fun thermalC(): Double {
        val limit = currentLimit ?: return cableDescriptor?.thermalC ?: Double.POSITIVE_INFINITY
        return limit * limit * resistanceOhms() * Eln.cableHeatingTime / warmLimitCelsius()
    }

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
    }
}
