@file:JvmName("LegacyCompat")
package mods.eln.compat

import mods.eln.generic.IDescriptorItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

/*
 * 1.7.10 names for 1.20.1 APIs, so ported code keeps its shape. 1.7.10 used null for "no item"; 1.20.1 uses
 * ItemStack.EMPTY, so the getters below return null for empty stacks and the setters accept null.
 */

var ItemStack.stackSize: Int
    get() = count
    set(value) { count = value }

// Descriptor items have no damage subtypes in 1.20.1; their stable descriptor id stands in for the old damage value.
val ItemStack.itemDamage: Int
    get() = (item as? IDescriptorItem)?.descriptor?.parentItemDamage ?: damageValue

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

fun Container.decrStackSize(slot: Int, amount: Int): ItemStack? = removeItem(slot, amount).takeUnless { it.isEmpty }

fun ItemStack?.orEmpty(): ItemStack = this ?: ItemStack.EMPTY
