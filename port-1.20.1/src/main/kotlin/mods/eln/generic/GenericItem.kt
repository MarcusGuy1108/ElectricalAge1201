package mods.eln.generic

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/**
 * Item for one [GenericItemUsingDamageDescriptor], forwarding the 1.20.1 item callbacks to the descriptor the way
 * 1.7.10's GenericItemUsingDamage did for every damage value.
 */
open class GenericItem @JvmOverloads constructor(val descriptor: GenericItemUsingDamageDescriptor, properties: Properties = Properties()) : Item(properties) {
    private val legacyKey = descriptor.name.replace(Regex("\\s+"), "_") + ".name"

    override fun getDescriptionId(stack: ItemStack): String = legacyKey
    override fun getDescriptionId(): String = legacyKey

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        val lines = ArrayList<String>()
        descriptor.addInformation(stack, null, lines, flag.isAdvanced)
        lines.forEach { tooltip.add(Component.literal(it)) }
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        val result = descriptor.onItemRightClick(stack, level, player)
        return InteractionResultHolder.sidedSuccess(result, level.isClientSide)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val pos = context.clickedPos
        val hit = context.clickLocation.subtract(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        val used = descriptor.onItemUse(
            context.itemInHand, context.player, context.level, pos.x, pos.y, pos.z,
            context.clickedFace.get3DDataValue(), hit.x.toFloat(), hit.y.toFloat(), hit.z.toFloat()
        )
        return if (used) InteractionResult.sidedSuccess(context.level.isClientSide) else InteractionResult.PASS
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slot: Int, selected: Boolean) {
        descriptor.onUpdate(stack, level, entity, slot, selected)
    }

    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState): Boolean {
        return descriptor.isCorrectToolForDrops(stack, state)
    }

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float {
        return descriptor.getStrVsBlock(stack, state.block)
    }

    override fun mineBlock(stack: ItemStack, level: Level, state: BlockState, pos: BlockPos, entity: LivingEntity): Boolean {
        return descriptor.onBlockDestroyed(stack, level, state.block, pos.x, pos.y, pos.z, entity)
    }

    override fun onDroppedByPlayer(item: ItemStack, player: Player?): Boolean {
        return descriptor.onDroppedByPlayer(item, player)
    }

    override fun onEntitySwing(stack: ItemStack, entity: LivingEntity): Boolean {
        return descriptor.onEntitySwing(entity, stack)
    }

    override fun onBlockStartBreak(stack: ItemStack, pos: BlockPos, player: Player): Boolean {
        return descriptor.onBlockStartBreak(stack, pos.x, pos.y, pos.z, player)
    }
}
