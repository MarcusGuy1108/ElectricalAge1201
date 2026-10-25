package mods.eln.gui

import mods.eln.gui.ISlotSkin.SlotSkin
import net.minecraft.world.Container
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

open class SlotWithSkin(
    inventory: Container,
    slotIndex: Int,
    xDisplayPosition: Int,
    yDisplayPosition: Int,
    var skin: SlotSkin
): Slot(
    inventory,
    slotIndex,
    xDisplayPosition,
    yDisplayPosition
), ISlotSkin {
    override fun getSlotSkin(): SlotSkin {
        return skin
    }

    // 1.7.10 names, kept so the ported slot filters override them unchanged.
    open fun isItemValid(itemStack: ItemStack): Boolean = true
    open fun getSlotStackLimit(): Int = container.maxStackSize
    final override fun mayPlace(stack: ItemStack): Boolean = isItemValid(stack)
    final override fun getMaxStackSize(): Int = getSlotStackLimit()
}
