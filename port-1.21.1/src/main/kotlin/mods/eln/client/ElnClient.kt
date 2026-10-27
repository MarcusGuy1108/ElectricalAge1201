package mods.eln.client

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.fml.loading.FMLEnvironment
import mods.eln.debug.ClientSmokeCommands
import mods.eln.sixnode.tutorialsign.TutorialSignOverlay
import net.neoforged.neoforge.client.event.RenderGuiEvent
import mods.eln.node.transparent.TransparentNodeRender
import mods.eln.node.transparent.TransparentNodeBlock
import mods.eln.gui.ElnMenus
import mods.eln.railroad.EntityElectricMinecart
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.MinecartRenderer
import mods.eln.node.NodeBlockEntity
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import mods.eln.node.six.SixNodeBlockRegistration
import mods.eln.node.six.SixNodeRender
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.client.event.RenderFrameEvent
import net.neoforged.bus.api.IEventBus

/**
 * Client-only setup, called from ElnMod on the client distribution.
 */
object ElnClient {
    private var lastFrameNanos = 0L

    fun init(modBus: IEventBus) {
        modBus.addListener(::registerRenderers)
        modBus.addListener(::registerScreens)
        NeoForge.EVENT_BUS.addListener(::onRenderTick)
        val tutorialSignOverlay = TutorialSignOverlay()
        NeoForge.EVENT_BUS.addListener { event: RenderGuiEvent.Post -> tutorialSignOverlay.render(event) }
        if (!FMLEnvironment.production) {
            NeoForge.EVENT_BUS.addListener(ClientSmokeCommands::registerClient)
            NeoForge.EVENT_BUS.addListener(ClientSmokeCommands::onClientTick)
        }
    }

    private fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(SixNodeBlockRegistration.ENTITY.get()) { SixNodeRender() }
        event.registerBlockEntityRenderer(TransparentNodeBlock.ENTITY.get()) { TransparentNodeRender() }
        event.registerEntityRenderer(EntityElectricMinecart.TYPE.get()) { MinecartRenderer(it, ModelLayers.MINECART) }
    }

    private fun registerScreens(event: RegisterMenuScreensEvent) {
        event.register(ElnMenus.NODE.get(), MenuScreens.ScreenConstructor<AbstractContainerMenu, AbstractContainerScreen<AbstractContainerMenu>> { menu, inventory, _ ->
            ElnMenus.createScreen(menu, inventory)
        })
    }

    // 1.7.10 FrameTime: let client node entities animate once per frame with the frame duration.
    private fun onRenderTick(@Suppress("UNUSED_PARAMETER") event: RenderFrameEvent.Pre) {
        val now = System.nanoTime()
        val deltaT = if (lastFrameNanos == 0L) 0f else ((now - lastFrameNanos) / 1e9f).coerceAtMost(0.1f)
        lastFrameNanos = now
        FrameTime.deltaT = deltaT
        for (entity in NodeBlockEntity.clientList) {
            entity.clientRefresh(deltaT)
        }
    }
}
