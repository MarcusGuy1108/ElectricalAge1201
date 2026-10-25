package mods.eln.gui

import mods.eln.client.GL11
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

/**
 * 1.7.10 GUIs drew through static Gui helpers and the bound GL texture. 1.20.1 draws through a [GuiGraphics]; Eln
 * screens publish theirs here for the duration of a render call so the ported widgets keep their draw methods.
 * Outside a render call every method does nothing.
 */
object GuiDraw {
    @JvmField
    var graphics: GuiGraphics? = null

    @JvmStatic
    val font: Font
        get() = Minecraft.getInstance().font

    @JvmStatic
    fun fill(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        graphics?.fill(x0, y0, x1, y1, color)
    }

    @JvmStatic
    fun fillGradient(x0: Int, y0: Int, x1: Int, y1: Int, colorTop: Int, colorBottom: Int) {
        graphics?.fillGradient(x0, y0, x1, y1, colorTop, colorBottom)
    }

    // Draws from the texture last bound with UtilsClient.bindTexture, assuming a 256x256 sheet as 1.7.10 did.
    @JvmStatic
    fun blit(x: Int, y: Int, u: Int, v: Int, width: Int, height: Int) {
        val texture = GL11.boundTexture ?: return
        graphics?.blit(texture, x, y, u, v, width, height)
    }

    @JvmStatic
    fun drawString(font: Font, text: String?, x: Int, y: Int, color: Int, shadow: Boolean) {
        if (text == null) return
        graphics?.drawString(font, text, x, y, color, shadow)
    }

    @JvmStatic
    fun drawTooltip(lines: List<String>, x: Int, y: Int) {
        if (lines.isEmpty()) return
        graphics?.renderComponentTooltip(font, lines.map { Component.literal(it) }, x, y)
    }
}
