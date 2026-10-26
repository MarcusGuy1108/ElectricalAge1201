package mods.eln.client

import mods.eln.sixnode.tutorialsign.TutorialSignOverlay
import net.minecraftforge.client.event.RenderGuiEvent
import mods.eln.node.transparent.TransparentNodeRender
import mods.eln.node.transparent.TransparentNodeBlock
import mods.eln.gui.ElnMenus
import mods.eln.node.NodeBlockEntity
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
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
        modBus.addListener(::clientSetup)
        MinecraftForge.EVENT_BUS.addListener(::onRenderTick)
        val tutorialSignOverlay = TutorialSignOverlay()
        MinecraftForge.EVENT_BUS.addListener { event: RenderGuiEvent.Post -> tutorialSignOverlay.render(event) }
    }

    private fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(SixNodeBlockRegistration.ENTITY.get()) { SixNodeRender() }
        event.registerBlockEntityRenderer(TransparentNodeBlock.ENTITY.get()) { TransparentNodeRender() }
    }

    private fun clientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            MenuScreens.register(ElnMenus.NODE.get(), MenuScreens.ScreenConstructor<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>> { menu, inventory, _ ->
                ElnMenus.createScreen(menu, inventory)
            })
        }
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
