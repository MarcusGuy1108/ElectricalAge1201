package mods.eln.item.electricalitem

import mods.eln.compat.*
import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import net.minecraft.world.level.block.Block
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

open class ElectricalTool(name: String, var strengthOn: Float, var strengthOff: Float,
                          var energyStorage: Double, var energyPerBlock: Double, var chargePower: Double) : GenericItemUsingDamageDescriptor(name), IItemEnergyBattery {
    var light = 0
    var range = 0
    var rIcon: ResourceLocation
    override fun onEntitySwing(entityLiving: LivingEntity?, stack: ItemStack?): Boolean {
        if (entityLiving!!.worldObj.isRemote) return false
        Eln.itemEnergyInventoryProcess.addExclusion(this, 2.0)
        return super.onEntitySwing(entityLiving, stack)
    }

    override fun onBlockDestroyed(stack: ItemStack, w: Level, block: Block, x: Int, y: Int, z: Int, entity: LivingEntity): Boolean {
        subtractEnergyForBlockBreak(stack, block)
        Utils.println("destroy")
        return true
    }

    fun subtractEnergyForBlockBreak(stack: ItemStack, block: Block) {
        if (getStrVsBlock(stack, block) == strengthOn) {
            var e = getEnergy(stack) - energyPerBlock
            if (e < 0) e = 0.0
            setEnergy(stack, e)
        }
    }

    fun getStrength(stack: ItemStack): Float {
        return if (getEnergy(stack) >= energyPerBlock) strengthOn else strengthOff
    }

    override fun getDefaultNBT(): CompoundTag? {
        val nbt = CompoundTag()
        nbt.putDouble("energy", 0.0)
        nbt.putBoolean("powerOn", false)
        nbt.putInt("rand", (Math.random() * 0xFFFFFFF).toInt())
        return nbt
    }

    fun getPowerOn(stack: ItemStack?): Boolean {
        return getNbt(stack!!).getBoolean("powerOn")
    }

    fun setPowerOn(stack: ItemStack?, value: Boolean) {
        getNbt(stack!!).putBoolean("powerOn", value)
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (itemStack != null) list.add(tr("Stored energy: %1\$J (%2$%)", Utils.plotValue(getEnergy(itemStack)),
            (getEnergy(itemStack) / energyStorage * 100).toInt()))
    }

    override fun getEnergy(stack: ItemStack): Double {
        return getNbt(stack).getDouble("energy")
    }

    override fun setEnergy(stack: ItemStack, value: Double) {
        getNbt(stack).putDouble("energy", value)
    }

    override fun getEnergyMax(stack: ItemStack): Double {
        return energyStorage
    }

    override fun getChargePower(stack: ItemStack): Double {
        return chargePower
    }

    override fun getDischagePower(stack: ItemStack): Double {
        return 0.0
    }

    override fun getPriority(stack: ItemStack): Int {
        return 0
    }

    override fun electricalItemUpdate(stack: ItemStack, time: Double) {}

    init {
        rIcon = ResourceLocation("eln", "textures/items/" + name.replace(" ", "").lowercase() + ".png")
    }
}
