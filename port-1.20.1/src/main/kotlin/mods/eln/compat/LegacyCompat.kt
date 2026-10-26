@file:JvmName("LegacyCompat")
package mods.eln.compat

import mods.eln.generic.GenericItem
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import mods.eln.generic.IDescriptorItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3

/*
 * 1.7.10 names for 1.20.1 APIs, so ported code keeps its shape. 1.7.10 used null for "no item"; 1.20.1 uses
 * ItemStack.EMPTY, so the getters below return null for empty stacks and the setters accept null.
 */

var ItemStack.stackSize: Int
    get() = count
    set(value) { count = value }

// Descriptor items have no damage subtypes in 1.20.1; their stable descriptor id stands in for the old damage value.
val ItemStack.itemDamage: Int
    get() = (item as? IDescriptorItem)?.descriptor?.parentItemDamage
        ?: (item as? GenericItem)?.descriptor?.parentItemDamage
        ?: damageValue

var ItemStack.tagCompound: CompoundTag?
    get() = tag
    set(value) { tag = value }

fun ItemStack.hasTagCompound(): Boolean = hasTag()

fun ItemStack.copyOrNull(): ItemStack? = if (isEmpty) null else copy()

val Player.currentEquippedItem: ItemStack?
    get() = mainHandItem.takeUnless { it.isEmpty }

val Entity.worldObj: Level
    get() = level()

val BlockEntity.worldObj: Level
    get() = level!!

val Level.isRemote: Boolean
    get() = isClientSide

val Container.sizeInventory: Int
    get() = containerSize

fun Container.getStackInSlot(slot: Int): ItemStack? = getItem(slot).takeUnless { it.isEmpty }

fun Container.setInventorySlotContents(slot: Int, stack: ItemStack?) = setItem(slot, stack ?: ItemStack.EMPTY)

// 1.7.10 code also grew stacks with a negative amount; removeItem ignores those.
fun Container.decrStackSize(slot: Int, amount: Int): ItemStack? {
    if (amount < 0) {
        val stack = getItem(slot)
        if (!stack.isEmpty) {
            stack.grow(-amount)
            setChanged()
        }
        return null
    }
    return removeItem(slot, amount).takeUnless { it.isEmpty }
}

fun ItemStack?.orEmpty(): ItemStack = this ?: ItemStack.EMPTY

val BlockEntity.xCoord: Int
    get() = blockPos.x

val BlockEntity.yCoord: Int
    get() = blockPos.y

val BlockEntity.zCoord: Int
    get() = blockPos.z

val Entity.posX: Double
    get() = x

val Entity.posY: Double
    get() = y

val Entity.posZ: Double
    get() = z

val Entity.rotationYaw: Float
    get() = yRot

val Entity.rotationPitch: Float
    get() = xRot

fun BlockGetter.getBlock(x: Int, y: Int, z: Int): Block = getBlockState(BlockPos(x, y, z)).block

fun BlockGetter.getTileEntity(x: Int, y: Int, z: Int): BlockEntity? = getBlockEntity(BlockPos(x, y, z))

fun Level.blockExists(x: Int, y: Int, z: Int): Boolean = isLoaded(BlockPos(x, y, z))

val Vec3.xCoord: Double
    get() = x

val Vec3.yCoord: Double
    get() = y

val Vec3.zCoord: Double
    get() = z
