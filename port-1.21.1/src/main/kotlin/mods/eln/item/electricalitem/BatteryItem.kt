package mods.eln.item.electricalitem

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation

class BatteryItem(name: String, var energyStorage: Double, var chargePower: Double, var dischargePower: Double, private val priority: Int) : GenericItemUsingDamageDescriptor(name), IItemEnergyBattery {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
    }

    override fun getDefaultNBT(): CompoundTag? {
        val nbt = CompoundTag()
        nbt.putDouble("energy", 0.0)
        return nbt
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Charge power: %1\$W", Utils.plotValue(chargePower)))
        list.add(tr("Discharge power: %1\$W", Utils.plotValue(dischargePower)))
        if (itemStack != null) {
            list.add(tr("Stored energy: %1\$J (%2$%)", Utils.plotValue(getEnergy(itemStack)),
                (getEnergy(itemStack) / energyStorage * 100).toInt()))
        }
    }

    override fun getEnergy(stack: ItemStack): Double {
        return getNbt(stack).getDouble("energy")
    }

    override fun setEnergy(stack: ItemStack, value: Double) {
        editNbt(stack) { putDouble("energy", Math.max(0.0, value)) }
    }

    override fun getEnergyMax(stack: ItemStack): Double {
        return energyStorage
    }

    override fun getChargePower(stack: ItemStack): Double {
        return chargePower
    }

    override fun getDischagePower(stack: ItemStack): Double {
        return dischargePower
    }

    override fun getPriority(stack: ItemStack): Int {
        return priority
    }

    override fun electricalItemUpdate(stack: ItemStack, time: Double) {}
}
