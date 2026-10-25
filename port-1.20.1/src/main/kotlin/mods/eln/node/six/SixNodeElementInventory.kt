package mods.eln.node.six

import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils.readFromNBT
import mods.eln.misc.Utils.writeToNBT
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * Inventory of a six-node element. 1.7.10 implemented IInventory by hand; 1.20.1's SimpleContainer covers the slot
 * handling, and the legacy getStackInSlot/setInventorySlotContents names come from mods.eln.compat.
 */
class SixNodeElementInventory : SimpleContainer, INBTTReady {
    var sixNodeRender: SixNodeElementRender? = null
    var sixNodeElement: SixNodeElement? = null
    var stackLimit: Int

    // PORT: this was IUtilityCableInventory.requiredCableLength; the interface returns with the utility cables.
    var requiredCableLength = DEFAULT_REQUIRED_LENGTH

    constructor(size: Int, stackLimit: Int, sixNodeRender: SixNodeElementRender?) : super(size) {
        this.stackLimit = stackLimit
        this.sixNodeRender = sixNodeRender
    }

    constructor(size: Int, stackLimit: Int, sixNodeElement: SixNodeElement?) : super(size) {
        this.stackLimit = stackLimit
        this.sixNodeElement = sixNodeElement
    }

    /**
     * Constructor for inventories which require a different length of utility cable than the default.
     */
    constructor(size: Int, stackLimit: Int, sixNodeElement: SixNodeElement?, requiredCableLength: Double) : super(size) {
        this.stackLimit = stackLimit
        this.sixNodeElement = sixNodeElement
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
        if (sixNodeElement != null && !sixNodeElement!!.sixNode!!.isDestructing) {
            sixNodeElement!!.inventoryChanged()
        }
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        return false
    }

    override fun readFromNBT(nbt: CompoundTag, str: String) {
        readFromNBT(nbt, str, this)
    }

    override fun writeToNBT(nbt: CompoundTag, str: String) {
        writeToNBT(nbt, str, this)
    }

    companion object {
        const val DEFAULT_REQUIRED_LENGTH = 1.0
    }
}
