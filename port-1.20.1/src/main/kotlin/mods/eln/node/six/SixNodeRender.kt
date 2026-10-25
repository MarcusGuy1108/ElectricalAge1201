package mods.eln.node.six

import com.mojang.blaze3d.vertex.PoseStack
import mods.eln.client.GL11
import mods.eln.misc.Direction.Companion.fromInt
import mods.eln.misc.UtilsClient.glDefaultColor
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer

/**
 * Draws every element of a six-node through its [SixNodeElementRender], with the GL stand-in mapped onto the
 * 1.20.1 pose stack and buffers. The block entity pose already sits at the block corner, so 1.7.10's camera-relative
 * x/y/z translation is gone.
 */
class SixNodeRender : BlockEntityRenderer<SixNodeEntity> {
    override fun render(tileEntity: SixNodeEntity, partialTick: Float, poseStack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        GL11.beginRender(poseStack, buffers, light, overlay)
        try {
            GL11.glPushMatrix()
            GL11.glTranslatef(.5f, .5f, .5f)
            for ((idx, render) in tileEntity.elementRenderList.withIndex()) {
                if (render != null) {
                    glDefaultColor()
                    GL11.glPushMatrix()
                    fromInt(idx)!!.glRotateXnRef()
                    GL11.glTranslatef(-0.5f, 0f, 0f)
                    render.draw()
                    GL11.glPopMatrix()
                }
            }
            GL11.glPopMatrix()
        } finally {
            GL11.endRender()
        }
    }

    override fun shouldRenderOffScreen(tileEntity: SixNodeEntity): Boolean = !tileEntity.cameraDrawOptimisation()

    // 1.7.10 getMaxRenderDistanceSquared was 4096 * 4 * 4, i.e. 256 blocks.
    override fun getViewDistance(): Int = 256
}
