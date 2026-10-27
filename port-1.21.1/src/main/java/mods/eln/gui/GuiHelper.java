package mods.eln.gui;

import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class GuiHelper {
    public Screen screen;
    public int xSize, ySize;
    ResourceLocation background;
    static final ResourceLocation helperTexture = ResourceLocation.fromNamespaceAndPath("eln", "sprites/gui/helpertexture.png");

    static final ResourceLocation slotSkin = ResourceLocation.parse("textures/gui/container/furnace.png");

    public GuiHelper(Screen screen, int xSize, int ySize, String backgroundName) {
        this.screen = screen;
        this.xSize = xSize;
        this.ySize = ySize;
        background = Utils.resourceLocation("eln:sprites/gui/" + backgroundName);
    }

    public GuiHelper(Screen screen, int xSize, int ySize) {
        this.screen = screen;
        this.xSize = xSize;
        this.ySize = ySize;
    }

    GuiTextFieldEln newGuiTextField(int x, int y, int width) {
        return newGuiTextField(x, y, width, 150);
    }

    GuiTextFieldEln newGuiTextField(int x, int y, int width, int maxLength) {
        GuiTextFieldEln o;
        o = new GuiTextFieldEln(GuiDraw.getFont(),
            screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, 12, this, maxLength);
        objectList.add(o);
        return o;
    }

    GuiButtonEln newGuiButton(int x, int y, int width, String name) {
        GuiButtonEln o;
        o = new GuiButtonEln(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, 20, name);
        o.setHelper(this);
        objectList.add(o);
        return o;
    }

    GuiHorizontalTrackBar newGuiHorizontalTrackBar(int x, int y, int width, int height) {
        GuiHorizontalTrackBar o;
        o = new GuiHorizontalTrackBar(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, height, this);
        objectList.add(o);
        return o;
    }

    GuiVerticalTrackBar newGuiVerticalTrackBar(int x, int y, int width, int height) {
        GuiVerticalTrackBar o;
        o = new GuiVerticalTrackBar(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, height, this);
        objectList.add(o);
        return o;
    }

    GuiVerticalTrackBarHeat newGuiVerticalTrackBarHeat(int x, int y, int width, int height) {
        GuiVerticalTrackBarHeat o;
        o = new GuiVerticalTrackBarHeat(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, height, this);
        objectList.add(o);
        return o;
    }

    public GuiVerticalProgressBar newGuiVerticalProgressBar(int x, int y, int width, int height) {
        GuiVerticalProgressBar o;
        o = new GuiVerticalProgressBar(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, height, this);
        objectList.add(o);
        return o;
    }

    public GuiVerticalCustomValuesBar newGuiVerticalCustomValuesBar(int x, int y, int width, int height, Float[] values) {
        GuiVerticalCustomValuesBar o;
        o = new GuiVerticalCustomValuesBar(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, height, this, values);
        objectList.add(o);
        return o;
    }

    public void add(IGuiObject o) {
        o.translate(screen.width / 2 - xSize / 2, screen.height / 2 - ySize / 2);
        objectList.add(o);
    }

    public void remove(IGuiObject o) {
        o.translate(-screen.width / 2 + xSize / 2, -screen.height / 2 + ySize / 2);
        objectList.remove(o);
    }

    ArrayList<IGuiObject> objectList = new ArrayList<IGuiObject>();

    void draw(int x, int y, float f) {
        // Screen.render already drew the background (1.21 blurs it, and only once per frame).
        int px = (screen.width - xSize) / 2, py = (screen.height - ySize) / 2;
        if (background != null) {
            UtilsClient.bindTexture(background);
            GuiDraw.blit(px, py, 0, 0, xSize, ySize);
        } else {
            UtilsClient.bindTexture(helperTexture);

            Gui.drawRect(px + 2, py + 2, px + xSize - 2, py + ySize - 2, 0xFFC6C6C6);

            Gui.drawRect(px + 4, py, px + xSize - 4, py + 1, 0xFF000000);
            Gui.drawRect(px + 4, py + 1, px + xSize - 4, py + 3, 0xFFFFFFFF);
            Gui.drawRect(px + 4, py + ySize - 1, px + xSize - 4, py + ySize - 0, 0xFF000000);
            Gui.drawRect(px + 4, py + ySize - 3, px + xSize - 4, py + ySize - 1, 0xFF555555);

            Gui.drawRect(px, py + 4, px + 1, py + ySize - 4, 0xFF000000);
            Gui.drawRect(px + 1, py + 4, px + 3, py + ySize - 4, 0xFFFFFFFF);
            Gui.drawRect(px + xSize - 1, py + 4, px + xSize - 0, py + ySize - 4, 0xFF000000);
            Gui.drawRect(px + xSize - 3, py + 4, px + xSize - 1, py + ySize - 4, 0xFF555555);

            GuiDraw.blit(px, py, 0, 0, 4, 4);
            GuiDraw.blit(px + xSize - 4, py, 4, 0, 4, 4);
            GuiDraw.blit(px, py + ySize - 4, 0, 4, 4, 4);
            GuiDraw.blit(px + xSize - 4, py + ySize - 4, 4, 4, 4, 4);
        }

        for (IGuiObject o : objectList) {
            o.idraw(x, y, f);
        }
    }

    public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height) {
        x += (screen.width - xSize) / 2;
        y += (screen.height - ySize) / 2;
        GuiDraw.blit(x, y, u, v, width, height);
    }

    public void drawRect(int x0, int y0, int x1, int y1, int color) {
        int dx = (screen.width - xSize) / 2;
        int dy = (screen.height - ySize) / 2;
        Gui.drawRect(x0 + dx, y0 + dy, x1 + dx, y1 + dy, color);
    }

    IGuiObject[] objectListCopy() {
        IGuiObject[] cpy = new IGuiObject[objectList.size()];
        for (int idx = 0; idx < cpy.length; idx++) {
            cpy[idx] = objectList.get(idx);
        }
        return cpy;
    }

    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean used = false;
        for (IGuiObject o : objectListCopy()) {
            used |= o.ikeyPressed(keyCode, scanCode, modifiers);
        }
        return used;
    }

    protected boolean charTyped(char c, int modifiers) {
        boolean used = false;
        for (IGuiObject o : objectListCopy()) {
            used |= o.icharTyped(c, modifiers);
        }
        return used;
    }

    protected void mouseClicked(int x, int y, int code) {
        for (IGuiObject o : objectListCopy()) {
            o.imouseClicked(x, y, code);
        }
    }

    protected void mouseMove(int x, int y) {
        for (IGuiObject o : objectList) {
            o.imouseMove(x, y);
        }
    }

    protected void mouseMovedOrUp(int x, int y, int witch) {
        for (IGuiObject o : objectList) {
            o.imouseMovedOrUp(x, y, witch);
        }
    }

    public void drawString(int x, int y, int color, String str) {
        GuiDraw.drawString(GuiDraw.getFont(), str, screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, color, false);
    }

    public void draw2(int x, int y) {
        for (IGuiObject o : objectList) {
            o.idraw2(x, y);
        }
    }

    // Uses the vanilla tooltip renderer; the 1.7.10 code drew the same frame by hand.
    public void drawHoveringText(List<String> list, int x, int y, Font font) {
        if (list.isEmpty()) return;
        if (screen instanceof AbstractContainerScreen) {
            // Container foreground drawing is translated to the GUI corner.
            x -= (screen.width - xSize) / 2;
            y -= (screen.height - ySize) / 2;
        }
        GuiDraw.drawTooltip(list, x, y);
    }

    public int getHoveringTextWidth(List<String> comment, Font fontRenderer) {
        int strWidth = 0;
        for (String str : comment) {
            int size = fontRenderer.width(str);
            if (size > strWidth) strWidth = size;
        }
        return strWidth + 5;
    }

    public int getHoveringTextHeight(List<String> comment, Font fontRenderer) {
        return comment.size() * 9 - 4;
    }

    public void drawProcess(int x, int y, float value) {
        UtilsClient.bindTexture(helperTexture);
        drawTexturedModalRect(x, y, 8, 0, (int) (22), 16);
        drawTexturedModalRect(x, y, 8 + 22, 0, (int) (22 * value), 16);
    }

    protected void drawSlot(int x, int y) {
        UtilsClient.bindTexture(slotSkin);

        drawTexturedModalRect(x - 1, y - 1, 55, 16, 73 - 55, 34 - 16);
    }
}
