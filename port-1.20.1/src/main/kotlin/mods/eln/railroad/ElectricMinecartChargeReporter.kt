package mods.eln.railroad

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.misc.Utils
import net.minecraft.network.chat.Component
import net.minecraftforge.event.entity.player.PlayerInteractEvent

/** A multimeter or all meter used while riding an electric minecart reports the cart's stored energy. */
object ElectricMinecartChargeReporter {
    fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
        val player = event.entity
        if (player.level().isClientSide) return
        val minecart = player.vehicle as? EntityElectricMinecart ?: return
        val heldItem = event.itemStack
        val holdingMeter = Eln.multiMeterElement?.checkSameItemStack(heldItem) == true ||
            Eln.allMeterElement?.checkSameItemStack(heldItem) == true
        if (!holdingMeter) return
        player.sendSystemMessage(Component.literal(I18N.tr("Cart Energy: ") + Utils.plotEnergy(minecart.energyBufferJoules)))
    }
}
