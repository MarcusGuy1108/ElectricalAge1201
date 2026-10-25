package mods.eln.generic

import mods.eln.misc.RealisticEnum
import mods.eln.misc.VoltageLevelColor
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

open class GenericItemUsingDamageDescriptor {
    @JvmOverloads
    constructor(name: String, iconName: String = name) {
        setDefaultIcon(iconName)
        this.name = name
        byName[name] = this
    }

    // "eln:<icon>" texture under assets/eln/textures/items/, used to generate the item model.
    var IconName: String? = null
    @JvmField
    var name: String
    @JvmField
    var voltageLevelColor = VoltageLevelColor.None
    // In 1.20.1 every descriptor has its own item; parentItemDamage stays as the stable numeric id saves use.
    @JvmField
    var parentItem: Item? = null
    @JvmField
    var parentItemDamage = 0
    open var creativeTab: GenericCreativeTab? = null
    var hidden: Boolean = false

    fun setDefaultIcon(name: String) {
        IconName = "eln:" + name.replace(" ".toRegex(), "").lowercase()
    }

    open fun getDefaultNBT(): CompoundTag? = null
    open fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {}
    open fun addRealismContext(list: List<*>?): RealisticEnum? = null

    open fun onItemRightClick(s: ItemStack, w: Level, p: Player): ItemStack {
        return s
    }

    // PORT: updateIcons and the IItemRenderer methods are replaced by generated item models.

    open fun getName(stack: ItemStack): String? {
        return name
    }

    open fun setParent(item: Item?, damage: Int) {
        parentItem = item
        parentItemDamage = damage
    }

    open fun newItemStack(size: Int): ItemStack {
        val stack = ItemStack(parentItem!!, size)
        getDefaultNBT()?.let { stack.tag = it }
        return stack
    }

    fun newItemStack(): ItemStack {
        return newItemStack(1)
    }

    open fun hideFromCreative(): GenericItemUsingDamageDescriptor {
        hidden = true
        return this
    }

    fun isHidden(): Boolean = hidden

    fun checkSameItemStack(stack: ItemStack?): Boolean {
        if (stack == null || stack.isEmpty) return false
        return stack.item === this.parentItem
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    open fun onItemUse(stack: ItemStack?, player: Player?, world: Level?, x: Int, y: Int, z: Int, side: Int, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    open fun onUpdate(stack: ItemStack, world: Level, entity: Entity, par4: Int, par5: Boolean) {}

    protected fun getNbt(stack: ItemStack): CompoundTag {
        var nbt = stack.tag
        if (nbt == null) {
            nbt = getDefaultNBT() ?: CompoundTag()
            stack.tag = nbt
        }
        return nbt
    }

    open fun getStrVsBlock(stack: ItemStack, block: Block?): Float {
        return 0.2f
    }

    open fun onBlockDestroyed(stack: ItemStack, w: Level, block: Block, x: Int, y: Int, z: Int, entity: LivingEntity): Boolean {
        return false
    }

    open fun onDroppedByPlayer(item: ItemStack, player: Player?): Boolean {
        return true
    }

    open fun onEntitySwing(entityLiving: LivingEntity?, stack: ItemStack?): Boolean {
        return false
    }

    open fun onBlockStartBreak(itemstack: ItemStack?, x: Int, y: Int, z: Int, player: Player?): Boolean {
        return false
    }

    companion object {
        var byName = HashMap<String, GenericItemUsingDamageDescriptor>()
        @JvmField
        var INVALID_NAME = "\$NO_DESCRIPTOR"

        @JvmStatic
        fun getByName(name: String?): GenericItemUsingDamageDescriptor? {
            return byName[name]
        }

        @JvmStatic
        fun getDescriptor(stack: ItemStack?): GenericItemUsingDamageDescriptor? {
            if (stack == null || stack.isEmpty) return null
            return (stack.item as? GenericItem)?.descriptor
        }

        @JvmStatic
        fun getDescriptor(stack: ItemStack?, extendClass: Class<*>): GenericItemUsingDamageDescriptor? {
            val desc = getDescriptor(stack) ?: return null
            return if (!extendClass.isAssignableFrom(desc.javaClass)) null else desc
        }
    }
}
