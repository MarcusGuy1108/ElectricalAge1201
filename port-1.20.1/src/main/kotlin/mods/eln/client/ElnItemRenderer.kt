package mods.eln.client

import com.mojang.blaze3d.vertex.PoseStack
import mods.eln.generic.GenericItem
import mods.eln.generic.ICustomItemRender
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

/** Draws [ICustomItemRender] descriptors through the GL stand-in. */
object ElnItemRenderer : BlockEntityWithoutLevelRenderer(Minecraft.getInstance().blockEntityRenderDispatcher, Minecraft.getInstance().entityModels) {
    override fun renderByItem(stack: ItemStack, context: ItemDisplayContext, poseStack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val render = (stack.item as? GenericItem)?.descriptor as? ICustomItemRender ?: return
        GL11.beginRender(poseStack, buffers, light, overlay)
        try {
            render.renderInHand(stack, context)
        } finally {
            GL11.endRender()
        }
    }
}
