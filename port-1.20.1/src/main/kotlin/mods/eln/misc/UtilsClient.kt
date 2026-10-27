@file:Suppress("NAME_SHADOWING")
package mods.eln.misc

import net.minecraft.world.item.ItemStack
import mods.eln.compat.*
import mods.eln.client.GL11
import mods.eln.compat.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.network.ElnNetwork
import mods.eln.node.six.SixNodeEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.sqrt

object UtilsClient {
    // PORT: guiLastOpen and the item renderer return with the GUI layer.
    @JvmStatic
    var uuid = Int.MIN_VALUE
        get() {
            if (field > -1) field = Int.MIN_VALUE
            return field++
        }
        private set
    val whiteTexture = ResourceLocation("eln", "sprites/cable.png")
    val portableBatteryOverlayResource = ResourceLocation("eln", "sprites/portablebatteryoverlay.png")
    fun distanceFromClientPlayer(@Suppress("UNUSED_PARAMETER") world: Level?, xCoord: Int, yCoord: Int, zCoord: Int): Float {
        val player = Minecraft.getInstance().player ?: return 0f
        return Math.sqrt((xCoord - player.posX) * (xCoord - player.posX) + (yCoord - player.posY) * (yCoord - player.posY) + (zCoord - player.posZ) * (zCoord - player.posZ)).toFloat()
    }

    @JvmStatic
    fun distanceFromClientPlayer(tileEntity: SixNodeEntity): Float {
        return distanceFromClientPlayer(tileEntity.worldObj, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord)
    }

    val clientPlayer: LocalPlayer
        get() = Minecraft.getInstance().player!!

    fun drawHaloNoLightSetup(halo: Obj3DPart?, r: Float, g: Float, b: Float, w: Level, x: Int, y: Int, z: Int, bilinear: Boolean) {
        if (halo == null) return
        withBilinearFilters(halo, bilinear) {
            val light = getLight(w, x, y, z) * 19 / 15 - 4
            val e: Entity = clientPlayer
            val d = (Math.abs(x - e.posX) + Math.abs(y - e.posY) + Math.abs(z - e.posZ)).toFloat()
            GL11.glColor4f(r, g, b, 1f - light / 15f)
            halo.draw(d * 20, 1f, 0f, 0f)
            GL11.glColor4f(1f, 1f, 1f, 1f)
        }
    }

    // PORT: clientOpenGui returns with the GUI layer.

    @JvmStatic
    fun drawHalo(halo: Obj3DPart?, r: Float, g: Float, b: Float, w: Level, x: Int, y: Int, z: Int, bilinear: Boolean) {
        disableLight()
        enableBlend()
        drawHaloNoLightSetup(halo, r, g, b, w, x, y, z, bilinear)
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun drawHaloNoLightSetup(halo: Obj3DPart?, r: Float, g: Float, b: Float, e: BlockEntity, bilinear: Boolean) {
        drawHaloNoLightSetup(halo, r, g, b, e.worldObj, e.xCoord, e.yCoord, e.zCoord, bilinear)
    }

    @JvmStatic
    fun drawHalo(halo: Obj3DPart?, r: Float, g: Float, b: Float, e: BlockEntity, bilinear: Boolean) {
        drawHalo(halo, r, g, b, e.worldObj, e.xCoord, e.yCoord, e.zCoord, bilinear)
    }

    @JvmStatic
    fun drawHaloNoLightSetup(halo: Obj3DPart?, @Suppress("UNUSED_PARAMETER") distance: Float) {
        if (halo == null) return
        // This overload deliberately draws without rebinding, using the first face group's texture.
        // Preserve that behavior while restoring the texture's original filters afterward.
        val faceGroup = halo.faceGroup.firstOrNull()
        if (faceGroup?.textureResource == null) {
            halo.drawNoBind()
            return
        }
        faceGroup.bindTexture()
        val filterState = saveTextureFilter()
        setBilinearFilter()
        try {
            halo.drawNoBind()
        } finally {
            faceGroup.bindTexture()
            restoreTextureFilter(filterState)
        }
    }

    @JvmStatic
    fun drawHalo(halo: Obj3DPart?, distance: Float) {
        disableLight()
        enableBlend()
        drawHaloNoLightSetup(halo, distance)
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun drawHaloNoLightSetup(halo: Obj3DPart?, r: Float, g: Float, b: Float, e: Entity, bilinear: Boolean) {
        if (halo == null) return
        withBilinearFilters(halo, bilinear) {
            val light = getLight(e.worldObj, Mth.floor(e.posX), Mth.floor(e.posY), Mth.floor(e.posZ))
            GL11.glColor4f(r, g, b, 1f - light / 15f)
            halo.draw()
            GL11.glColor4f(1f, 1f, 1f, 1f)
        }
    }

    @JvmStatic
    fun drawHalo(halo: Obj3DPart?, r: Float, g: Float, b: Float, e: Entity, bilinear: Boolean) {
        disableLight()
        enableBlend()
        drawHaloNoLightSetup(halo, r, g, b, e, bilinear)
        enableLight()
        disableBlend()
    }

    data class TextureFilterState(val minFilter: Int, val magFilter: Int)

    /** Saves the filters on the currently bound texture. */
    @JvmStatic
    fun saveTextureFilter(): TextureFilterState = TextureFilterState(
        GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER),
        GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER)
    )

    @JvmStatic
    fun setBilinearFilter() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
    }

    @JvmStatic
    fun setNearestFilter() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    }

    @JvmStatic
    fun restoreTextureFilter(state: TextureFilterState) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, state.minFilter)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, state.magFilter)
    }

    private fun withBilinearFilters(halo: Obj3DPart, enabled: Boolean, draw: () -> Unit) {
        if (!enabled) {
            draw()
            return
        }

        // Texture filters belong to texture objects, not OpenGL's transient draw state.
        // Bind each texture this model can draw before changing it, then restore it afterward.
        val filterStates = halo.faceGroup
            .filter { it.textureResource != null }
            .distinctBy { it.textureResource }
            .map { faceGroup ->
                faceGroup.bindTexture()
                faceGroup to saveTextureFilter().also { setBilinearFilter() }
            }
        try {
            draw()
        } finally {
            filterStates.forEach { (faceGroup, filterState) ->
                faceGroup.bindTexture()
                restoreTextureFilter(filterState)
            }
        }
    }

    @JvmStatic
    fun disableCulling() {
        GL11.glDisable(GL11.GL_CULL_FACE)
    }

    @JvmStatic
    fun enableCulling() {
        GL11.glEnable(GL11.GL_CULL_FACE)
    }

    @JvmStatic
    fun disableTexture() {
        bindTexture(whiteTexture)
        //GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    @JvmStatic
    fun enableTexture() {
        //GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    // 1.7.10 turned off the lightmap texture so parts glowed; the GL stand-in uses full-bright light instead.
    @JvmStatic
    fun disableLight() {
        GL11.lightOverride = LightTexture.FULL_BRIGHT
        GL11.glDisable(GL11.GL_LIGHTING)
    }

    @JvmStatic
    fun enableLight() {
        GL11.lightOverride = null
        GL11.glEnable(GL11.GL_LIGHTING)
    }

    @JvmStatic
    fun enableBlend() {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        // GL11.glDepthMask(true);
        // GL11.glDisable(GL11.GL_ALPHA_TEST);
        /*
         * Utils.println(GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB) + " " + GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA) + " " + GL11.glGetInteger(GL14.GL_BLEND_DST_RGB) + " " + GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA) + " " + GL11.glIsEnabled(GL11.GL_BLEND));
		 */

        // Utils.println(GL11.glGetInteger(GL11.GL_BLEND_SRC) + " " + GL11.glGetInteger(GL11.GL_BLEND_DST) + " " + GL11.glIsEnabled(GL11.GL_BLEND));
        /*
		 * GL11.glEnable(2977); GL11.glEnable(3042);
		 */
        // OpenGlHelper.glBlendFunc(770, 770, 771, 771);
    }

    @JvmStatic
    fun disableBlend() {
        GL11.glDisable(GL11.GL_BLEND)

        // GL11.glDepthMask(true);
        // GL11.glEnable(GL11.GL_ALPHA_TEST);
        // GL11.glDisable(GL11.GL_BLEND);
        // GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // Utils.println(GL11.glGetInteger(GL11.GL_BLEND_SRC) + " " + GL11.glGetInteger(GL11.GL_BLEND_DST) + " " + GL11.glIsEnabled(GL11.GL_BLEND));
        // GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
        // GL11.glBlendFunc(1, 1);
        // GL11.glDisable(3042);

        // OpenGlHelper.glBlendFunc(1, 1, 1, 1);
    }

    // 1.7.10 drew machine contents through a fake EntityItem; 1.20.1 renders the stack with the ground transform.
    @JvmStatic
    fun drawEntityItem(stack: ItemStack?, x: Double, y: Double, z: Double, roty: Float, scale: Float) {
        if (stack == null || stack.isEmpty) return
        GL11.glPushMatrix()
        GL11.glTranslatef(x.toFloat(), y.toFloat(), z.toFloat())
        GL11.glRotatef(roty, 0f, 1f, 0f)
        GL11.glScalef(scale, scale, scale)
        GL11.renderItem(stack)
        GL11.glPopMatrix()
    }

    // PORT: drawIcon/drawEnergyBare (IItemRenderer item drawing) are replaced by generated item models.





    @JvmStatic
    fun bindTexture(resource: ResourceLocation?) {
        GL11.boundTexture = resource
    }

    @JvmStatic
    fun ledOnOffColor(on: Boolean) {
        if (!on) GL11.glColor3f(0.7f, 0f, 0f) // Red
        else GL11.glColor3f(0f, 0.7f, 0f) // Green
    }

    @JvmStatic
    fun ledOnOffColorC(on: Boolean): Color {
        return if (!on) Color(0.7f, 0f, 0f) // Red
        else Color(0f, 0.7f, 0f) // Green
    }

    @JvmStatic
    fun drawLight(part: Obj3DPart?) {
        if (part == null) return
        disableLight()
        enableBlend()
        part.draw()
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun drawLightNoBind(part: Obj3DPart?) {
        if (part == null) return
        disableLight()
        enableBlend()
        part.drawNoBind()
        enableLight()
        disableBlend()
    }

    // PORT: drawGuiBackground returns with the GUI layer.

    fun drawLight(part: Obj3DPart?, angle: Float, x: Float, y: Float, z: Float) {
        if (part == null) return
        disableLight()
        enableBlend()
        part.draw(angle, x, y, z)
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun glDefaultColor() {
        GL11.glColor4f(1f, 1f, 1f, 1f)
    }


    fun drawConnectionPinSixNode(d: Float, w: Float, h: Float) {
        var d = d
        var w = w
        var h = h
        d += 0.1f
        d *= 0.0625f
        w *= 0.0625f
        h *= 0.0625f
        val w2 = w * 0.5f
        disableTexture()
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex3f(-w2, d, 0f)
        GL11.glVertex3f(w2, d, 0f)
        GL11.glVertex3f(w2, d, h)
        GL11.glVertex3f(-w2, d, h)
        GL11.glEnd()
        enableTexture()
    }

    @JvmStatic
    fun drawConnectionPinSixNode(front: LRDU, dList: FloatArray, w: Float, h: Float) {
        // front.glRotateOnX();
        // drawConnectionPinSixNode(d[front.toInt()], w, h);
        var w = w
        var h = h
        var d = dList[front.toInt()]
        d += 0.04f
        d *= 0.0625f
        w *= 0.0625f
        h *= 0.0625f
        val w2 = w * 0.5f
        disableTexture()
        GL11.glBegin(GL11.GL_QUADS)
        when (front) {
            LRDU.Left -> {
                GL11.glVertex3f(0f, -w2, -d)
                GL11.glVertex3f(0f, w2, -d)
                GL11.glVertex3f(h, w2, -d)
                GL11.glVertex3f(h, -w2, -d)
            }
            LRDU.Right -> {
                GL11.glVertex3f(h, -w2, d)
                GL11.glVertex3f(h, w2, d)
                GL11.glVertex3f(0f, w2, d)
                GL11.glVertex3f(0f, -w2, d)
            }
            LRDU.Down -> {
                GL11.glVertex3f(h, -d, -w2)
                GL11.glVertex3f(h, -d, w2)
                GL11.glVertex3f(0f, -d, w2)
                GL11.glVertex3f(0f, -d, -w2)
            }
            LRDU.Up -> {
                GL11.glVertex3f(0f, d, -w2)
                GL11.glVertex3f(0f, d, w2)
                GL11.glVertex3f(h, d, w2)
                GL11.glVertex3f(h, d, -w2)
            }
        }
        GL11.glEnd()
        enableTexture()
    }


    fun mc(): Minecraft {
        return Minecraft.getInstance()
    }

    fun guiScale() {
        GL11.glScalef(16f, 16f, 1f)
    }

    // PORT: drawItemStack returns with the GUI layer.

    fun clientDistanceTo(e: Entity?): Double {
        if (e == null) return 100000000.0
        val c: Entity = Minecraft.getInstance().player ?: return 100000000.0
        val x = c.posX - e.posX
        val y = c.posY - e.posY
        val z = c.posZ - e.posZ
        return sqrt(x * x + y * y + z * z)
    }

    @JvmStatic
    fun clientDistanceTo(t: BlockEntity?): Double {
        if (t == null) return 100000000.0
        val c: Entity = Minecraft.getInstance().player ?: return 100000000.0
        return sqrt(c.position().distanceToSqr(t.blockPos.x.toDouble(), t.blockPos.y.toDouble(), t.blockPos.z.toDouble()))
    }

    fun getLight(w: Level, x: Int, y: Int, z: Int): Int {
        // Combined block and sky light with the current sky darkening, as 1.7.10 computed by hand.
        return w.getMaxLocalRawBrightness(BlockPos(x, y, z))
    }

    @JvmStatic
    fun disableDepthTest() {
        GL11.glDisable(GL11.GL_DEPTH_TEST)
    }

    @JvmStatic
    fun enableDepthTest() {
        GL11.glEnable(GL11.GL_DEPTH_TEST)
    }

    @JvmStatic
    fun sendPacketToServer(bos: ByteArrayOutputStream) {
        ElnNetwork.sendToServer(bos.toByteArray())
    }

    val glListsAllocated = HashSet<Int>()
    @JvmStatic
    fun glGenListsSafe(): Int {
        val id = GL11.glGenLists(1)
        glListsAllocated.add(id)
        return id
    }

    @JvmStatic
    fun glDeleteListsSafe(id: Int) {
        glListsAllocated.remove(id)
        GL11.glDeleteLists(id, 1)
    }

    @JvmStatic
    fun glDeleteListsAllSafe() {
        try {
            for (id in glListsAllocated) {
                GL11.glDeleteLists(id, 1)
            }
            glListsAllocated.clear()
        } catch (e: Exception) {
            //nic
        }
    }

    @JvmStatic
    fun showItemTooltip(details: List<String>, realismDetails: List<String>, realisticEnum: RealisticEnum?, dst: MutableList<String>) {
        if (realisticEnum != null)
            dst.add("§r${realisticEnum.color}${realisticEnum.name}§r")
        if (details.isNotEmpty()) {
            if (isShiftHeld()) {
                dst.addAll(details)
            } else {
                dst.add("§F§o${tr("Hold [shift] for details")}")
            }
        }
        if (realismDetails.isNotEmpty()) {
            if (isControlHeld()) {
                dst.addAll(realismDetails)
            } else {
                if (realisticEnum != null) {
                    if (realismDetails.isNotEmpty()) {
                        dst.add("§F§o${tr("Hold [ctrl] for realism details")}")
                    }
                }
            }
        }
        dst.listLengthFormatter(24)
    }

    private fun List<String>.listLengthFormatter(@Suppress("UNUSED_PARAMETER") length: Int) {}

    private fun isShiftHeld(): Boolean {
        return Screen.hasShiftDown()
    }

    private fun isControlHeld(): Boolean {
        return Screen.hasControlDown()
    }

    @JvmStatic
    fun getWeather(world: Level): Double {
        if (world.isThundering) return 1.0
        return if (world.isRaining) 0.5 else 0.0
    }
}
