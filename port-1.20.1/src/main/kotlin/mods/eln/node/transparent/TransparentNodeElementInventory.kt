package mods.eln.node.transparent

import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils.readFromNBT
import mods.eln.misc.Utils.writeToNBT
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * Inventory of a transparent node element. 1.7.10 implemented ISidedInventory by hand; 1.20.1's SimpleContainer
 * covers the slot handling. The side access methods stay so machines keep overriding them.
 */
open class TransparentNodeElementInventory : SimpleContainer, INBTTReady {
    @JvmField
    protected var transparentNodeRender: TransparentNodeElementRender? = null
    @JvmField
    protected var transparentNodeElement: TransparentNodeElement? = null
    var stackLimit: Int

    // PORT: this was IUtilityCableInventory.requiredCableLength; the interface returns with the utility cables.
    var requiredCableLength = DEFAULT_REQUIRED_LENGTH

    constructor(size: Int, stackLimit: Int, transparentNodeRender: TransparentNodeElementRender?) : super(size) {
        this.stackLimit = stackLimit
        this.transparentNodeRender = transparentNodeRender
    }

    constructor(size: Int, stackLimit: Int, transparentNodeElement: TransparentNodeElement?) : super(size) {
        this.stackLimit = stackLimit
        this.transparentNodeElement = transparentNodeElement
    }

    constructor(size: Int, stackLimit: Int, transparentNodeElement: TransparentNodeElement?, requiredCableLength: Double) : super(size) {
        this.stackLimit = stackLimit
        this.transparentNodeElement = transparentNodeElement
        this.requiredCableLength = requiredCableLength
    }

    override fun getMaxStackSize(): Int {
        return stackLimit
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun setChanged() {
        super.setChanged()
        if (transparentNodeElement != null && !transparentNodeElement!!.node!!.isDestructing) {
            transparentNodeElement!!.inventoryChange(this)
        }
    }

    override fun readFromNBT(nbt: CompoundTag, str: String) {
        readFromNBT(nbt, str, this)
    }

    override fun writeToNBT(nbt: CompoundTag, str: String) {
        writeToNBT(nbt, str, this)
    }

    // 1.7.10 isItemValidForSlot: valid if some side may insert it.
    override fun canPlaceItem(i: Int, itemstack: ItemStack): Boolean {
        for (idx in 0..5) {
            for (slot in getAccessibleSlotsFromSide(idx)) {
                if (slot == i && canInsertItem(i, itemstack, idx)) {
                    return true
                }
            }
        }
        return false
    }

    // PORT: the side access below feeds hoppers and pipes again once the block entity exposes an item handler.
    open fun getAccessibleSlotsFromSide(side: Int): IntArray {
        return intArrayOf()
    }

    open fun canInsertItem(slot: Int, stack: ItemStack?, side: Int): Boolean {
        return false
    }

    open fun canExtractItem(slot: Int, stack: ItemStack?, side: Int): Boolean {
        return false
    }

    companion object {
        const val DEFAULT_REQUIRED_LENGTH = 1.0
    }
}
