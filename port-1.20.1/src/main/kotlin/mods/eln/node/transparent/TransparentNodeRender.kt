package mods.eln.node.transparent

import com.mojang.blaze3d.vertex.PoseStack
import mods.eln.client.GL11
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer

class TransparentNodeRender : BlockEntityRenderer<TransparentNodeEntity> {
    override fun render(tileEntity: TransparentNodeEntity, partialTick: Float, poseStack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val render = tileEntity.elementRender ?: return
        GL11.beginRender(poseStack, buffers, light, overlay)
        try {
            GL11.glPushMatrix()
            GL11.glTranslatef(.5f, .5f, .5f)
            render.draw()
            GL11.glPopMatrix()
        } finally {
            GL11.endRender()
        }
    }

    override fun shouldRenderOffScreen(tileEntity: TransparentNodeEntity): Boolean = !tileEntity.cameraDrawOptimisation()

    override fun getViewDistance(): Int = 256
}
