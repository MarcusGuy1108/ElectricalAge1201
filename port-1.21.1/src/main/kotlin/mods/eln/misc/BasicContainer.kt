package mods.eln.misc

import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.SlotWithSkin
import mods.eln.gui.ElnMenus
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * Base of Eln container menus: the element slots followed by the player inventory, whose positions the GUI sets.
 * 1.20.1 menus need the window id at construction while Eln containers are built by elements and renders that do
 * not know it, so the code opening the menu passes it through [withContainerId].
 */
open class BasicContainer(player: Player, protected var inventory: Container, slot: Array<Slot>) :
    AbstractContainerMenu(ElnMenus.NODE.get(), pendingContainerId.get() ?: 0) {
    init {
        for (i in slot.indices) {
            addSlot(slot[i])
        }
        bindPlayerInventory(player.inventory)
    }

    override fun stillValid(player: Player): Boolean {
        return inventory.stillValid(player)
    }

    private fun bindPlayerInventory(inventoryPlayer: Inventory) {
        for (i in 0..2) {
            for (j in 0..8) {
                addSlot(SlotWithSkin(inventoryPlayer, j + i * 9 + 9, j * 18, i * 18, SlotSkin.medium))
            }
        }
        for (i in 0..8) {
            addSlot(SlotWithSkin(inventoryPlayer, i, i * 18, 58, SlotSkin.medium))
        }
    }

    // 1.7.10 transferStackInSlot; vanilla moveItemStackTo honours the same slot filters and stack limits.
    override fun quickMoveStack(player: Player, slotId: Int): ItemStack {
        val slot = slots.getOrNull(slotId)
        if (slot != null && slot.hasItem()) {
            val itemstack1 = slot.item
            val invSize = inventory.containerSize
            if (slotId < invSize) {
                moveItemStackTo(itemstack1, invSize, slots.size, true)
            } else {
                if (!moveItemStackTo(itemstack1, 0, invSize, true)) {
                    if (slotId < invSize + 27) {
                        moveItemStackTo(itemstack1, invSize + 27, slots.size, false)
                    } else {
                        moveItemStackTo(itemstack1, invSize, invSize + 27, false)
                    }
                }
            }

            if (itemstack1.isEmpty) {
                slot.set(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }

        return ItemStack.EMPTY
    }

    companion object {
        private val pendingContainerId = ThreadLocal<Int>()

        @JvmStatic
        fun <T> withContainerId(id: Int, block: () -> T): T {
            pendingContainerId.set(id)
            try {
                return block()
            } finally {
                pendingContainerId.remove()
            }
        }
    }
}
