package mods.eln.debug

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.node.six.SixNodeItem
import mods.eln.node.transparent.TransparentNodeItem
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.event.RegisterCommandsEvent

/**
 * Dev-only commands for checking every node in game; registered only outside production. /elnsmoke lays every machine
 * and six-node out in a grid next to the player (renders), /elnsmokegui (ClientSmokeCommands) then opens each loaded
 * node's GUI in turn.
 */
object SmokeCommands {
    private const val COLUMNS = 20
    private const val SPACING = 8

    fun registerServer(event: RegisterCommandsEvent) {
        event.dispatcher.register(Commands.literal("elnsmoke").requires { it.hasPermission(2) }.executes { context ->
            place(context.source.playerOrException)
            1
        })
    }

    private fun place(player: ServerPlayer) {
        val level = player.serverLevel()
        val descriptors = Eln.transparentNodeItem.descriptors + Eln.sixNodeItem.descriptors
        val origin = player.blockPosition().offset(SPACING, 0, SPACING)
        var placed = 0
        for ((i, descriptor) in descriptors.withIndex()) {
            val pos = origin.offset((i % COLUMNS) * SPACING, 0, (i / COLUMNS) * SPACING)
            for (x in -3..3) for (z in -3..3) level.setBlockAndUpdate(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState())
            val stack = descriptor.newItemStack()
            val ok = when (val item = stack.item) {
                is TransparentNodeItem -> item.placeBlockAt(stack, player, level, pos, Direction.YP)
                is SixNodeItem -> item.placeBlockAt(stack, player, level, pos, Direction.YN)
                else -> false
            }
            if (ok) placed++
        }
        player.sendSystemMessage(Component.literal("Placed $placed of ${descriptors.size} nodes"))
    }
}
