@file:Suppress("NAME_SHADOWING")
package mods.eln.node.six

import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.Eln
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.IDescriptorItem
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.addChatMessage
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level

/**
 * Item for one [SixNodeDescriptor]. 1.7.10 used one SixNodeItem for every descriptor via the damage value.
 */
class SixNodeItem(override val descriptor: SixNodeDescriptor) : Item(Properties()), IDescriptorItem {

    // Legacy lang key, e.g. "Low_Voltage_Cable.name", so the 1.7.10 translations apply.
    override fun getDescriptionId(stack: ItemStack): String = descriptor.name.replace(Regex("\\s+"), "_") + ".name"

    override fun getDescriptionId(): String = descriptor.name.replace(Regex("\\s+"), "_") + ".name"
    // Items new in the port have no 1.7.10 lang entry; they show their descriptor name.
    override fun getName(stack: ItemStack): Component = Component.translatableWithFallback(getDescriptionId(stack), descriptor.name)

    override fun getMaxStackSize(stack: ItemStack): Int = descriptor.getItemStackLimit(stack)

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        val lines = ArrayList<String>()
        descriptor.addInformation(stack, null, lines, flag.isAdvanced)
        lines.forEach { tooltip.add(Component.literal(it)) }
    }

    private fun shouldConsumeUtilityCableLength(player: Player): Boolean {
        val creativeFreeLength = Eln.config.getBooleanOrElse("gameplay.cables.creativeFreeLength", true)
        return !(creativeFreeLength && player.abilities.instabuild)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        val stack = context.itemInHand
        if (stack.isEmpty) return InteractionResult.FAIL
        // BlockPlaceContext moves past non-replaceable blocks and replaces snow layers, grass and vines in place.
        val placeContext = BlockPlaceContext(context)
        val pos = placeContext.clickedPos
        val direction = Direction.from(context.clickedFace).inverse
        val descriptor = descriptor
        if (descriptor is UtilityCableDescriptor && !descriptor.hasLengthForPlacement(stack)) {
            if (!context.level.isClientSide) addChatMessage(player, "Not enough wire length remaining to place another segment")
            return InteractionResult.FAIL
        }
        if (!player.mayUseItemAt(pos, context.clickedFace, stack)) return InteractionResult.FAIL
        if (!descriptor.canBePlacedOnSide(player, Coordinate(pos.x, pos.y, pos.z, context.level), direction)) return InteractionResult.FAIL
        if (context.level.isClientSide) return InteractionResult.SUCCESS
        if (placeBlockAt(stack, player, context.level, pos, direction)) {
            val sound = Eln.sixNodeBlock.defaultBlockState().soundType
            context.level.playSound(null, pos, sound.placeSound, SoundSource.BLOCKS, (sound.volume + 1.0f) / 2.0f, sound.pitch * 0.8f)
            if (descriptor is UtilityCableDescriptor) {
                if (shouldConsumeUtilityCableLength(player)) {
                    descriptor.consumeLengthForPlacement(stack)
                    if (!descriptor.hasLengthForPlacement(stack)) stack.shrink(1)
                }
            } else if (!player.abilities.instabuild) {
                stack.shrink(1)
            }
        }
        return InteractionResult.CONSUME
    }

    fun placeBlockAt(stack: ItemStack, player: Player, world: Level, pos: BlockPos, direction: Direction): Boolean {
        if (world.isClientSide) return false
        val x = pos.x
        val y = pos.y
        val z = pos.z
        val blockOld = world.getBlockState(pos)
        val block = Eln.sixNodeBlock
        if (blockOld.isAir || blockOld.canBeReplaced()) {
            val coord = Coordinate(x, y, z, world)
            var error: String?
            if (descriptor.checkCanPlace(coord, direction, LRDU.Up).also { error = it } != null) {
                addChatMessage(player, error)
                return false
            }
            if (block.getIfOtherBlockIsSolid(world, x, y, z, direction)) {
                descriptor.getGhostGroup(direction, LRDU.Up)?.plot(coord, coord, descriptor.ghostGroupUuid)
                val sixNode = SixNode()
                sixNode.onBlockPlacedBy(Coordinate(x, y, z, world), direction, player, stack)
                sixNode.createSubBlock(stack, direction, player)
                world.setBlock(pos, block.defaultBlockState(), 3)
                return true
            }
        } else if (blockOld.block === block) {
            val sixNode = (world.getBlockEntity(pos) as? SixNodeEntity)?.node as SixNode?
            if (sixNode == null) {
                world.removeBlock(pos, false)
                return false
            }
            if (!sixNode.getSideEnable(direction) && block.getIfOtherBlockIsSolid(world, x, y, z, direction)) {
                sixNode.createSubBlock(stack, direction, player)
                return true
            }
        }
        return false
    }

    companion object {
        @JvmStatic
        fun descriptorOf(stack: ItemStack?): GenericItemBlockUsingDamageDescriptor? =
            GenericItemBlockUsingDamageDescriptor.getDescriptor(stack)
    }
}
