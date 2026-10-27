@file:JvmName("LegacyCompat")
package mods.eln.compat

import net.neoforged.neoforge.server.ServerLifecycleHooks
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.RegistryAccess
import net.minecraft.core.HolderLookup
import mods.eln.generic.GenericItem
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import mods.eln.generic.IDescriptorItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
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

// 1.21 keeps item NBT in the immutable minecraft:custom_data component. [tag] reads a copy; change it with [editTag]
// or by assigning [tag], never by mutating what the getter returned.
var ItemStack.tag: CompoundTag?
    get() = get(DataComponents.CUSTOM_DATA)?.copyTag()
    set(value) {
        if (value == null) remove(DataComponents.CUSTOM_DATA) else set(DataComponents.CUSTOM_DATA, CustomData.of(value))
    }

fun ItemStack.hasTag(): Boolean = has(DataComponents.CUSTOM_DATA)

// A copy, as [tag]; an empty tag when the stack has none.
val ItemStack.orCreateTag: CompoundTag
    get() = tag ?: CompoundTag()

fun ItemStack.editTag(edit: CompoundTag.() -> Unit) {
    CustomData.update(DataComponents.CUSTOM_DATA, this) { it.edit() }
}

var ItemStack.tagCompound: CompoundTag?
    get() = tag
    set(value) { tag = value }

fun ItemStack.hasTagCompound(): Boolean = hasTag()

// Registries for saving and loading item stacks: the running server's, else the built-in ones.
fun registries(): HolderLookup.Provider =
    ServerLifecycleHooks.getCurrentServer()?.registryAccess() ?: RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)

// 1.20.1 ItemStack.of(tag): EMPTY for a missing or unreadable stack.
fun itemStackOf(tag: CompoundTag): ItemStack = ItemStack.parseOptional(registries(), tag)

// 1.20.1 stack.save(tag): writes the stack into [tag]; an empty stack writes nothing (1.21 refuses to encode it).
fun ItemStack.save(tag: CompoundTag): CompoundTag {
    if (!isEmpty) tag.merge(save(registries()) as CompoundTag)
    return tag
}

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
