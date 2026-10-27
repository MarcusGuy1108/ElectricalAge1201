package mods.eln.generic

import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

/**
 * An item descriptor drawn by code in hand, on the ground and in item frames, as 1.7.10 IItemRenderers were; its item
 * model is builtin/entity there and the icon in the GUI. Runs inside the GL stand-in, in the item's model space.
 */
interface ICustomItemRender {
    fun renderInHand(stack: ItemStack, context: ItemDisplayContext)
}
