package mods.eln.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import mods.eln.Eln
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import org.joml.Vector3f

/**
 * Stand-in for org.lwjgl.opengl.GL11 so Eln's fixed-function render code runs on 1.20.1 unchanged: matrix calls
 * drive the block entity renderer's PoseStack, immediate-mode primitives become quads in a MultiBufferSource, and
 * enable/disable flags pick the render type and lighting. Display lists are recorded calls replayed on glCallList.
 * Outside [beginRender]/[endRender] every call is ignored, so stray GL calls (for example from not yet ported GUI
 * code) cannot crash.
 */
@Suppress("FunctionName", "unused", "MemberVisibilityCanBePrivate")
object GL11 {
    const val GL_POINTS = 0
    const val GL_LINES = 1
    const val GL_LINE_LOOP = 2
    const val GL_LINE_STRIP = 3
    const val GL_TRIANGLES = 4
    const val GL_TRIANGLE_STRIP = 5
    const val GL_TRIANGLE_FAN = 6
    const val GL_QUADS = 7
    const val GL_QUAD_STRIP = 8
    const val GL_POLYGON = 9
    const val GL_ZERO = 0
    const val GL_ONE = 1
    const val GL_SRC_ALPHA = 770
    const val GL_ONE_MINUS_SRC_ALPHA = 771
    const val GL_LEQUAL = 515
    const val GL_LESS = 513
    const val GL_CULL_FACE = 2884
    const val GL_LIGHTING = 2896
    const val GL_DEPTH_TEST = 2929
    const val GL_NORMALIZE = 2977
    const val GL_ALPHA_TEST = 3008
    const val GL_BLEND = 3042
    const val GL_SCISSOR_BOX = 3088
    const val GL_SCISSOR_TEST = 3089
    const val GL_TEXTURE_2D = 3553
    const val GL_COMPILE = 4864
    const val GL_FLAT = 7424
    const val GL_SMOOTH = 7425
    const val GL_NEAREST = 9728
    const val GL_LINEAR = 9729
    const val GL_TEXTURE_MAG_FILTER = 10240
    const val GL_TEXTURE_MIN_FILTER = 10241
    const val GL_ENABLE_BIT = 8192
    const val GL_CURRENT_BIT = 1
    const val GL_LIGHTING_BIT = 64

    private val WHITE = ResourceLocation(Eln.MODID, "textures/white.png")

    private var pose: PoseStack? = null
    private var buffers: MultiBufferSource? = null
    private var packedLight = LightTexture.FULL_BRIGHT
    private var packedOverlay = 0
    private var pushDepth = 0

    /** Light used instead of the block light, set by UtilsClient's lightmap helpers (null = block light). */
    @JvmStatic var lightOverride: Int? = null
    @JvmStatic var boundTexture: ResourceLocation? = null

    private var red = 1f
    private var green = 1f
    private var blue = 1f
    private var alpha = 1f
    private var lighting = true
    private var texture2d = true
    private var blend = false

    private var mode = -1
    private val vertices = ArrayList<FloatArray>()
    private var u = 0f
    private var v = 0f
    private var normalX = 0f
    private var normalY = 1f
    private var normalZ = 0f

    private val lists = HashMap<Int, MutableList<() -> Unit>>()
    private var nextList = 1
    private var recording: MutableList<() -> Unit>? = null
    private val attribStack = ArrayDeque<FloatArray>()

    @JvmStatic
    fun beginRender(poseStack: PoseStack, bufferSource: MultiBufferSource, light: Int, overlay: Int) {
        pose = poseStack
        buffers = bufferSource
        packedLight = light
        packedOverlay = overlay
        pushDepth = 0
        resetState()
    }

    @JvmStatic
    fun endRender() {
        while (pushDepth > 0) glPopMatrix()
        pose = null
        buffers = null
        resetState()
    }

    private fun resetState() {
        red = 1f; green = 1f; blue = 1f; alpha = 1f
        lighting = true; texture2d = true; blend = false
        mode = -1
        vertices.clear()
        lightOverride = null
        boundTexture = null
        attribStack.clear()
    }

    // Records the call while a display list is being compiled; otherwise tells the caller to run it now.
    private inline fun live(crossinline call: () -> Unit): Boolean {
        val list = recording ?: return pose != null
        list.add { call() }
        return false
    }

    @JvmStatic fun glPushMatrix() { if (live { glPushMatrix() }) { pose!!.pushPose(); pushDepth++ } }

    @JvmStatic fun glPopMatrix() {
        if (live { glPopMatrix() } && pushDepth > 0) { pose!!.popPose(); pushDepth-- }
    }

    @JvmStatic fun glTranslatef(x: Float, y: Float, z: Float) { if (live { glTranslatef(x, y, z) }) pose!!.translate(x, y, z) }
    @JvmStatic fun glTranslated(x: Double, y: Double, z: Double) = glTranslatef(x.toFloat(), y.toFloat(), z.toFloat())

    @JvmStatic
    fun glRotatef(angle: Float, x: Float, y: Float, z: Float) {
        if (!live { glRotatef(angle, x, y, z) }) return
        val axis = Vector3f(x, y, z)
        if (axis.lengthSquared() == 0f) return
        pose!!.mulPose(Axis.of(axis.normalize()).rotationDegrees(angle))
    }

    @JvmStatic fun glRotated(angle: Double, x: Double, y: Double, z: Double) = glRotatef(angle.toFloat(), x.toFloat(), y.toFloat(), z.toFloat())
    @JvmStatic fun glScalef(x: Float, y: Float, z: Float) { if (live { glScalef(x, y, z) }) pose!!.scale(x, y, z) }
    @JvmStatic fun glScaled(x: Double, y: Double, z: Double) = glScalef(x.toFloat(), y.toFloat(), z.toFloat())

    @JvmStatic
    fun glColor4f(r: Float, g: Float, b: Float, a: Float) {
        if (live { glColor4f(r, g, b, a) }) { red = r; green = g; blue = b; alpha = a }
    }

    @JvmStatic fun glColor3f(r: Float, g: Float, b: Float) = glColor4f(r, g, b, 1f)
    @JvmStatic fun glColor4d(r: Double, g: Double, b: Double, a: Double) = glColor4f(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())
    @JvmStatic fun glColor3d(r: Double, g: Double, b: Double) = glColor4f(r.toFloat(), g.toFloat(), b.toFloat(), 1f)

    @JvmStatic
    fun glEnable(cap: Int) {
        if (!live { glEnable(cap) }) return
        when (cap) {
            GL_LIGHTING -> lighting = true
            GL_TEXTURE_2D -> texture2d = true
            GL_BLEND -> blend = true
        }
    }

    @JvmStatic
    fun glDisable(cap: Int) {
        if (!live { glDisable(cap) }) return
        when (cap) {
            GL_LIGHTING -> lighting = false
            GL_TEXTURE_2D -> texture2d = false
            GL_BLEND -> blend = false
        }
    }

    @JvmStatic
    fun glIsEnabled(cap: Int): Boolean = when (cap) {
        GL_LIGHTING -> lighting
        GL_TEXTURE_2D -> texture2d
        GL_BLEND -> blend
        else -> false
    }

    @JvmStatic
    fun glPushAttrib(@Suppress("UNUSED_PARAMETER") mask: Int) {
        if (live { glPushAttrib(mask) }) {
            attribStack.addLast(floatArrayOf(red, green, blue, alpha, if (lighting) 1f else 0f, if (texture2d) 1f else 0f, if (blend) 1f else 0f))
        }
    }

    @JvmStatic
    fun glPopAttrib() {
        if (!live { glPopAttrib() }) return
        val s = attribStack.removeLastOrNull() ?: return
        red = s[0]; green = s[1]; blue = s[2]; alpha = s[3]
        lighting = s[4] != 0f; texture2d = s[5] != 0f; blend = s[6] != 0f
    }

    @JvmStatic fun glBegin(primitive: Int) { if (live { glBegin(primitive) }) { mode = primitive; vertices.clear() } }
    @JvmStatic fun glTexCoord2f(s: Float, t: Float) { if (live { glTexCoord2f(s, t) }) { u = s; v = t } }
    @JvmStatic fun glTexCoord2d(s: Double, t: Double) = glTexCoord2f(s.toFloat(), t.toFloat())

    @JvmStatic
    fun glNormal3f(x: Float, y: Float, z: Float) {
        if (live { glNormal3f(x, y, z) }) { normalX = x; normalY = y; normalZ = z }
    }

    @JvmStatic
    fun glVertex3f(x: Float, y: Float, z: Float) {
        if (live { glVertex3f(x, y, z) } && mode >= 0) {
            vertices.add(floatArrayOf(x, y, z, u, v, normalX, normalY, normalZ, red, green, blue, alpha))
        }
    }

    @JvmStatic fun glVertex3d(x: Double, y: Double, z: Double) = glVertex3f(x.toFloat(), y.toFloat(), z.toFloat())
    @JvmStatic fun glVertex2f(x: Float, y: Float) = glVertex3f(x, y, 0f)

    @JvmStatic
    fun glEnd() {
        if (!live { glEnd() }) return
        val primitive = mode
        mode = -1
        if (vertices.isEmpty()) return
        val buffer = currentBuffer()
        val q = vertices
        when (primitive) {
            GL_QUADS -> for (i in 0 until q.size - 3 step 4) quad(buffer, q[i], q[i + 1], q[i + 2], q[i + 3])
            GL_TRIANGLES -> for (i in 0 until q.size - 2 step 3) quad(buffer, q[i], q[i + 1], q[i + 2], q[i + 2])
            GL_QUAD_STRIP -> for (i in 0 until q.size - 3 step 2) quad(buffer, q[i], q[i + 1], q[i + 3], q[i + 2])
            GL_TRIANGLE_STRIP -> for (i in 0 until q.size - 2) {
                if (i % 2 == 0) quad(buffer, q[i], q[i + 1], q[i + 2], q[i + 2]) else quad(buffer, q[i + 1], q[i], q[i + 2], q[i + 2])
            }
            GL_TRIANGLE_FAN, GL_POLYGON -> for (i in 1 until q.size - 1) quad(buffer, q[0], q[i], q[i + 1], q[i + 1])
            // shortcut: points and lines are not drawn; only Eln debug overlays use them.
        }
        vertices.clear()
    }

    private fun currentBuffer(): VertexConsumer {
        val texture = if (texture2d) boundTexture ?: WHITE else WHITE
        val type = if (blend) RenderType.entityTranslucent(texture) else RenderType.entityCutoutNoCull(texture)
        return buffers!!.getBuffer(type)
    }

    private fun quad(buffer: VertexConsumer, a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray) {
        val last = pose!!.last()
        val light = lightOverride ?: packedLight
        for (p in arrayOf(a, b, c, d)) {
            // Without GL lighting the 1.7.10 code expected flat shading; an upward normal gives uniform brightness.
            val nx = if (lighting) p[5] else 0f
            val ny = if (lighting) p[6] else 1f
            val nz = if (lighting) p[7] else 0f
            buffer.vertex(last.pose(), p[0], p[1], p[2])
                .color(p[8], p[9], p[10], p[11])
                .uv(p[3], p[4])
                .overlayCoords(packedOverlay)
                .uv2(light)
                .normal(last.normal(), nx, ny, nz)
                .endVertex()
        }
    }

    @JvmStatic
    fun glGenLists(range: Int): Int {
        val first = nextList
        nextList += range
        return first
    }

    @JvmStatic
    fun glNewList(list: Int, @Suppress("UNUSED_PARAMETER") mode: Int) {
        val calls = ArrayList<() -> Unit>()
        lists[list] = calls
        recording = calls
    }

    @JvmStatic fun glEndList() { recording = null }

    @JvmStatic
    fun glCallList(list: Int) {
        if (live { glCallList(list) }) lists[list]?.forEach { it() }
    }

    @JvmStatic fun glDeleteLists(list: Int, range: Int) { for (i in list until list + range) lists.remove(i) }
    @JvmStatic fun glIsList(list: Int): Boolean = lists.containsKey(list)

    // Fixed-function state with no 1.20.1 per-vertex equivalent; render types decide these now.
    @JvmStatic fun glBlendFunc(s: Int, d: Int) {}
    @JvmStatic fun glDepthMask(flag: Boolean) {}
    @JvmStatic fun glDepthFunc(func: Int) {}
    @JvmStatic fun glShadeModel(mode: Int) {}
    @JvmStatic fun glHint(target: Int, mode: Int) {}
    @JvmStatic fun glLineWidth(width: Float) {}
    @JvmStatic fun glPointSize(size: Float) {}
    @JvmStatic fun glTexParameteri(target: Int, name: Int, value: Int) {}
    @JvmStatic fun glGetTexParameteri(target: Int, name: Int): Int = 0
    @JvmStatic fun glScissor(x: Int, y: Int, width: Int, height: Int) {}
    @JvmStatic fun glGetInteger(name: Int): Int = 0
}
