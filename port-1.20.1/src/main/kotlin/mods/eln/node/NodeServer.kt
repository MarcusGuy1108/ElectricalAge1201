package mods.eln.node

import mods.eln.environment.RoomThermalManager
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.TickEvent.ServerTickEvent
import net.minecraftforge.server.ServerLifecycleHooks

class NodeServer {
    fun init() {
        //	NodeBlockEntity.nodeAddedList.clear();
    }

    fun stop() {
        //	NodeBlockEntity.nodeAddedList.clear();
        RoomThermalManager.clear()
    }

    var counter = 0

    // Registered on the Forge event bus by ElnMod.
    fun tick(event: ServerTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        val server = ServerLifecycleHooks.getCurrentServer()
        if (server != null) {
            RoomThermalManager.tick(server)
            for (node in NodeManager.instance!!.nodeList) {
                if (node.needPublish) {
                    node.publishToAllPlayer()
                }
            }
            for (player in server.playerList.players) {
                var openContainerNode: NodeBase? = null
                var container: INodeContainer? = null
                if (player.containerMenu is INodeContainer) {
                    container = player.containerMenu as INodeContainer
                    openContainerNode = container.node
                }
                for (node in NodeManager.instance!!.nodeList) {
                    if (node === openContainerNode) {
                        if (counter % (1 + container!!.refreshRateDivider) == 0) node.publishToPlayer(player)
                    }
                }
            }
            counter++
        }
    }
}
