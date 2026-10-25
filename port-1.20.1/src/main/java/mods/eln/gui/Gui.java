package mods.eln.gui;

import net.minecraft.client.gui.Font;

/**
 * Stand-in for the 1.7.10 net.minecraft.client.gui.Gui base class that Eln widgets extend for its draw helpers.
 */
public class Gui {
    protected float zLevel;

    public static void drawRect(int x0, int y0, int x1, int y1, int color) {
        GuiDraw.fill(x0, y0, x1, y1, color);
    }

    public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height) {
        GuiDraw.blit(x, y, u, v, width, height);
    }

    protected void drawGradientRect(int x0, int y0, int x1, int y1, int colorTop, int colorBottom) {
        GuiDraw.fillGradient(x0, y0, x1, y1, colorTop, colorBottom);
    }

    public void drawString(Font font, String text, int x, int y, int color) {
        GuiDraw.drawString(font, text, x, y, color, true);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        GuiDraw.drawString(font, text, x - font.width(text) / 2, y, color, true);
    }
}
