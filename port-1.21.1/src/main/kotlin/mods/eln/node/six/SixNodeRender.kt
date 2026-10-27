package mods.eln.node.six

import net.minecraft.world.phys.AABB
import com.mojang.blaze3d.vertex.PoseStack
import mods.eln.client.GL11
import mods.eln.misc.Direction.Companion.fromInt
import mods.eln.misc.UtilsClient.glDefaultColor
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.client.RenderTypeHelper
import net.neoforged.neoforge.client.model.data.ModelData
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer

/**
 * Draws every element of a six-node through its [SixNodeElementRender], with the GL stand-in mapped onto the
 * 1.20.1 pose stack and buffers. The block entity pose already sits at the block corner, so 1.7.10's camera-relative
 * x/y/z translation is gone.
 */
class SixNodeRender : BlockEntityRenderer<SixNodeEntity> {
    override fun render(tileEntity: SixNodeEntity, partialTick: Float, poseStack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val cache = tileEntity.sixNodeCacheBlock
        if (!cache.isAir) {
            renderCacheBlock(tileEntity, cache, poseStack, buffers, overlay)
            // Elements inside an opaque cache block are hidden anyway; glass and the like still show them.
            if (cache.canOcclude()) return
        }
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

    // Drawn like a moving piston block: the block model, lit and face-culled from the world around it.
    private fun renderCacheBlock(tileEntity: SixNodeEntity, cache: BlockState, poseStack: PoseStack, buffers: MultiBufferSource, overlay: Int) {
        val level = tileEntity.level ?: return
        val pos = tileEntity.blockPos
        val dispatcher = Minecraft.getInstance().blockRenderer
        val model = dispatcher.getBlockModel(cache)
        val random = RandomSource.create()
        for (renderType in model.getRenderTypes(cache, random, ModelData.EMPTY)) {
            dispatcher.modelRenderer.tesselateBlock(
                level, model, cache, pos, poseStack, buffers.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)),
                true, random, cache.getSeed(pos), overlay, ModelData.EMPTY, renderType
            )
        }
    }

    override fun getRenderBoundingBox(tileEntity: SixNodeEntity): AABB = tileEntity.renderBoundingBox()

    override fun shouldRenderOffScreen(tileEntity: SixNodeEntity): Boolean = !tileEntity.cameraDrawOptimisation()

    // 1.7.10 getMaxRenderDistanceSquared was 4096 * 4 * 4, i.e. 256 blocks.
    override fun getViewDistance(): Int = 256
}
