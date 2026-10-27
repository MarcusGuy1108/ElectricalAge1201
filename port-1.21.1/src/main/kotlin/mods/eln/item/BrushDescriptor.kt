package mods.eln.item

import mods.eln.compat.*
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import mods.eln.client.GL11

class BrushDescriptor(name: String): GenericItemUsingDamageDescriptor(name) {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    private val ricon = ResourceLocation.fromNamespaceAndPath("eln", "textures/items/" + name.lowercase().replace(" ", "") + ".png")

    override fun getName(stack: ItemStack): String {
        val creative = Minecraft.getInstance().player?.abilities?.instabuild == true
        val color = getColor(stack)
        val life = getLife(stack)
        return if (!creative && color == 15 && life == 0) "Empty " + super.getName(stack) else super.getName(stack)?: ""
    }

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
    }

    fun getColor(stack: ItemStack) = stack.itemDamage and 0xF

    private fun getLife(stack: ItemStack?) = if (stack == null || stack.tag == null)
        32
    else
        stack.orCreateTag.getInt("life")

    fun setLife(stack: ItemStack, life: Int) {
        stack.editTag { putInt("life", life) }
    }

    override fun getDefaultNBT(): CompoundTag? {
        val nbt = CompoundTag()
        nbt.putInt("life", 32)
        return nbt
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        if (itemStack != null) {
            val creative = Minecraft.getInstance().player?.abilities?.instabuild == true
            list.add(tr("Can paint %1$ blocks", if (creative) "infinite" else itemStack.orCreateTag.getInt("life")))
        }
    }

    fun use(stack: ItemStack, entityPlayer: Player): Boolean {

        val creative = entityPlayer.abilities.instabuild
        var life = stack.orCreateTag.getInt("life")
        return if (creative || life != 0) {
            if (!creative) {
                --life
                stack.editTag { putInt("life", life) }
            }
            true
        } else {
            Utils.addChatMessage(entityPlayer, tr("Brush is dry"))
            false
        }
    }

    companion object {
        private val dryOverlay = ResourceLocation.fromNamespaceAndPath("eln", "textures/items/brushdryoverlay.png")
    }
}
