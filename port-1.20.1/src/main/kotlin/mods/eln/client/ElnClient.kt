package mods.eln.client

import mods.eln.node.NodeBlockEntity
import mods.eln.node.six.SixNodeBlockRegistration
import mods.eln.node.six.SixNodeRender
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.IEventBus

/**
 * Client-only setup, called from ElnMod on the client distribution.
 */
object ElnClient {
    private var lastFrameNanos = 0L

    fun init(modBus: IEventBus) {
        modBus.addListener(::registerRenderers)
        MinecraftForge.EVENT_BUS.addListener(::onRenderTick)
    }

    private fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(SixNodeBlockRegistration.ENTITY.get()) { SixNodeRender() }
    }

    // 1.7.10 FrameTime: let client node entities animate once per frame with the frame duration.
    private fun onRenderTick(event: TickEvent.RenderTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        val now = System.nanoTime()
        val deltaT = if (lastFrameNanos == 0L) 0f else ((now - lastFrameNanos) / 1e9f).coerceAtMost(0.1f)
        lastFrameNanos = now
        for (entity in NodeBlockEntity.clientList) {
            entity.clientRefresh(deltaT)
        }
    }
}
