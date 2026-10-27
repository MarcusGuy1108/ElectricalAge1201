package mods.eln.misc

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * Contains utilities for dealing with player entities.
 */

fun Player.totalItemsCarried(stack: ItemStack): Int {
    return inventory.items
        .filter { ItemStack.isSameItem(it, stack) }
        .sumOf { it.count }
}

// The server syncs the changed slots to the client on its own; 1.7.10 sent S2FPacketSetSlot by hand.
fun Player.removeMultipleItems(stack: ItemStack, count: Int) {
    if (this is ServerPlayer && Utils.isCreative(this)) return
    assert(count <= totalItemsCarried(stack))
    var left = count
    try {
        for (i in inventory.items.indices.reversed()) {
            val invStack = inventory.items[i]
            if (ItemStack.isSameItem(invStack, stack)) {
                left -= invStack.split(invStack.count.coerceAtMost(left)).count
                if (left == 0) return
            }
        }
    } finally {
        inventory.setChanged()
    }
}
