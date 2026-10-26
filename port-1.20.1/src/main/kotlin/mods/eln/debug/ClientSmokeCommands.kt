package mods.eln.debug

import mods.eln.Eln
import mods.eln.misc.BasicContainer
import mods.eln.misc.Direction
import mods.eln.node.NodeBlockEntity
import mods.eln.node.six.SixNodeEntity
import net.minecraft.client.Minecraft
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraftforge.client.event.RegisterClientCommandsEvent
import net.minecraftforge.event.TickEvent

/** Client half of SmokeCommands: /elnsmokegui opens every loaded node's GUI in turn. Dev only. */
object ClientSmokeCommands {
    private val guiQueue = ArrayDeque<Pair<NodeBlockEntity, Direction>>()
    private var guiOpened = 0
    private var guiRunning = false
    private var guiWait = 0

    fun registerClient(event: RegisterClientCommandsEvent) {
        event.dispatcher.register(Commands.literal("elnsmokegui").executes {
            queueGuis()
            1
        })
    }

    private fun queueGuis() {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return
        val center = minecraft.player?.blockPosition() ?: return
        val radius = minecraft.options.renderDistance().get()
        for (cx in -radius..radius) for (cz in -radius..radius) {
            val chunk = level.getChunk((center.x shr 4) + cx, (center.z shr 4) + cz)
            for (entity in chunk.blockEntities.values) {
                if (entity !is NodeBlockEntity) continue
                // A six-node has one GUI per occupied side; the others answer for any side.
                if (entity is SixNodeEntity) Direction.values().forEach { guiQueue.add(entity to it) }
                else guiQueue.add(entity to Direction.YN)
            }
        }
        guiOpened = 0
        guiRunning = true
        Eln.logger.info("GUI smoke: ${guiQueue.size} node sides queued")
    }

    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || !guiRunning) return
        if (guiWait-- > 0) return
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        while (true) {
            val (entity, side) = guiQueue.removeFirstOrNull() ?: run {
                minecraft.setScreen(null)
                guiRunning = false
                player.sendSystemMessage(Component.literal("GUI smoke: opened $guiOpened screens"))
                Eln.logger.info("GUI smoke: opened $guiOpened screens")
                return
            }
            val screen = try {
                BasicContainer.withContainerId(0) { entity.newGuiDraw(side, player) }
            } catch (e: Exception) {
                Eln.logger.error("GUI smoke: building the GUI at ${entity.blockPos} side $side failed", e)
                null
            } ?: continue
            Eln.logger.info("GUI smoke: ${screen.javaClass.simpleName} at ${entity.blockPos}")
            minecraft.setScreen(screen)
            guiOpened++
            guiWait = 2
            return
        }
    }
}
