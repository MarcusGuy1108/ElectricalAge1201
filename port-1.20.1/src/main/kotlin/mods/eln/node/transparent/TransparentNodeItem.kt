package mods.eln.node.transparent

import mods.eln.generic.IDescriptorItem
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
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
 * Item for one [TransparentNodeDescriptor]. 1.7.10 used one TransparentNodeItem for every descriptor via the damage
 * value; the in-hand model renderer is replaced by the generated icon model.
 */
class TransparentNodeItem(override val descriptor: TransparentNodeDescriptor) : Item(Properties()), IDescriptorItem {

    // Legacy lang key, e.g. "50V_Battery.name", so the 1.7.10 translations apply.
    override fun getDescriptionId(stack: ItemStack): String = descriptor.name.replace(Regex("\\s+"), "_") + ".name"

    override fun getDescriptionId(): String = descriptor.name.replace(Regex("\\s+"), "_") + ".name"

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        val lines = ArrayList<String>()
        descriptor.addInformation(stack, null, lines, flag.isAdvanced)
        lines.forEach { tooltip.add(Component.literal(it)) }
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        val stack = context.itemInHand
        if (stack.isEmpty) return InteractionResult.FAIL
        val placeContext = BlockPlaceContext(context)
        if (!placeContext.canPlace()) return InteractionResult.FAIL
        if (!player.mayUseItemAt(placeContext.clickedPos, context.clickedFace, stack)) return InteractionResult.FAIL
        if (context.level.isClientSide) return InteractionResult.SUCCESS
        if (placeBlockAt(stack, player, context.level, placeContext.clickedPos, Direction.from(context.clickedFace))) {
            val sound = TransparentNodeBlock.BLOCK.get().defaultBlockState().soundType
            context.level.playSound(null, placeContext.clickedPos, sound.placeSound, SoundSource.BLOCKS, (sound.volume + 1.0f) / 2.0f, sound.pitch * 0.8f)
            if (!player.abilities.instabuild) stack.shrink(1)
        }
        return InteractionResult.CONSUME
    }

    // [side] is the clicked face, as 1.7.10's metadata-free placeBlockAt side argument.
    fun placeBlockAt(stack: ItemStack, player: Player, world: Level, pos: BlockPos, side: Direction): Boolean {
        if (world.isClientSide) return false
        val direction = side.inverse
        val front = descriptor.getFrontFromPlace(direction, player)
        val v = intArrayOf(descriptor.spawnDeltaX, descriptor.spawnDeltaY, descriptor.spawnDeltaZ)
        front!!.rotateFromXN(v)
        val placePos = pos.offset(v[0], v[1], v[2])
        val coord = Coordinate(placePos.x, placePos.y, placePos.z, world)
        val error = descriptor.checkCanPlace(coord, front)
        if (error != null) {
            addChatMessage(player, error)
            return false
        }
        descriptor.getGhostGroupFront(front)?.plot(coord, coord, descriptor.ghostGroupUuid)
        val node = TransparentNode()
        node.onBlockPlacedBy(coord, front, player, stack)
        world.setBlock(placePos, TransparentNodeBlock.BLOCK.get().defaultBlockState(), 3)
        node.checkCanStay(true)
        return true
    }
}
