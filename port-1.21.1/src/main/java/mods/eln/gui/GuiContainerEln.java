package mods.eln.gui;

import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
import mods.eln.gui.IGuiObject.IGuiObjectObserver;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public abstract class GuiContainerEln extends AbstractContainerScreen<AbstractContainerMenu> implements IGuiObjectObserver, GuiTextFieldElnObserver {

    public GuiHelperContainer helper;

    protected abstract GuiHelperContainer newHelper();

    static final ResourceLocation slotSkin = ResourceLocation.parse("textures/gui/container/furnace.png");

    public GuiContainerEln(AbstractContainerMenu par1Container) {
        super(par1Container, Minecraft.getInstance().player.getInventory(), Component.empty());
    }

    public void add(IGuiObject object) {
        helper.add(object);
    }

    // 1.20.1 init() is the 1.7.10 initGui(); subclasses keep overriding initGui.
    @Override
    protected final void init() {
        initGui();
    }

    public void initGui() {
        helper = newHelper();
        imageWidth = helper.xSize;
        imageHeight = helper.ySize;
        super.init();

        if (helper instanceof GuiHelperContainer) {
            apply((GuiHelperContainer) helper);
        }
    }

    void apply(GuiHelperContainer helper) {
        int size = menu.slots.size();
        for (int idx = size - 36; idx < size; idx++) {
            Slot s = menu.slots.get(idx);
            s.x = (idx - (size - 36)) % 9 * 18;
            s.y = (idx - (size - 36)) / 9 * 18;
            if (idx >= size - 9) {
                s.y = 58;
                s.x = (idx - (size - 9)) * 18;
            }
            s.x += helper.xInv;
            s.y += helper.yInv;
        }
    }

    public GuiTextFieldEln newGuiTextField(int x, int y, int width) {
        GuiTextFieldEln o = helper.newGuiTextField(x, y, width);
        o.setObserver(this);
        return o;
    }

    public GuiTextFieldEln newGuiTextField(int x, int y, int width, int maxLength) {
        GuiTextFieldEln o = helper.newGuiTextField(x, y, width, maxLength);
        o.setObserver(this);
        return o;
    }

    public GuiButtonEln newGuiButton(int x, int y, int width, String name) {
        GuiButtonEln o = helper.newGuiButton(x, y, width, name);
        o.setObserver(this);
        return o;
    }

    public GuiHorizontalTrackBar newGuiHorizontalTrackBar(int x, int y, int width, int height) {
        GuiHorizontalTrackBar o = helper.newGuiHorizontalTrackBar(x, y, width, height);
        o.setObserver(this);
        return o;
    }

    public GuiVerticalTrackBar newGuiVerticalTrackBar(int x, int y, int width, int height) {
        GuiVerticalTrackBar o = helper.newGuiVerticalTrackBar(x, y, width, height);
        o.setObserver(this);
        return o;
    }

    public GuiVerticalTrackBarHeat newGuiVerticalTrackBarHeat(int x, int y, int width, int height) {
        GuiVerticalTrackBarHeat o = helper.newGuiVerticalTrackBarHeat(x, y, width, height);
        o.setObserver(this);
        return o;
    }

    public GuiVerticalProgressBar newGuiVerticalProgressBar(int x, int y, int width, int height) {
        GuiVerticalProgressBar o = helper.newGuiVerticalProgressBar(x, y, width, height);

        return o;
    }

    public void drawTexturedModalRectEln(int x, int y, int u, int v, int width, int height) {
        helper.drawTexturedModalRect(x, y, u, v, width, height);
    }

    // 1.7.10 only let escape through to the container screen, so typing in a text field never closes the GUI.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (helper.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return helper.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int code) {
        helper.mouseClicked((int) x, (int) y, code);
        return super.mouseClicked(x, y, code);
    }

    @Override
    public boolean mouseReleased(double x, double y, int code) {
        helper.mouseMovedOrUp((int) x, (int) y, code);
        return super.mouseReleased(x, y, code);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float f) {
        GuiDraw.graphics = graphics;
        try {
            super.render(graphics, x, y, f);
            renderTooltip(graphics, x, y);
        } finally {
            GuiDraw.graphics = null;
        }
    }

    @Override
    public void textFieldNewValue(GuiTextFieldEln textField, String value) {
        guiObjectEvent(textField);
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float f, int mx, int my) {
        preDraw(f, mx, my);
        helper.mouseMove(mx, my);
        helper.draw(mx, my, f);
        UtilsClient.bindTexture(slotSkin);

        for (Slot slot : menu.slots) {
            SlotSkin skin = SlotSkin.none;

            if (slot instanceof ISlotSkin) skin = ((ISlotSkin) slot).getSlotSkin();

            switch (skin) {
                case medium:
                    drawTexturedModalRectEln(slot.x - 1, slot.y - 1, 55, 16, 73 - 55, 34 - 16);
                    break;
                case big:
                    drawTexturedModalRectEln(slot.x - 5, slot.y - 5, 111, 30, 137 - 111, 56 - 30);
                    break;
                default:
                    break;
            }
        }
        postDraw(f, mx, my);
    }

    // 1.7.10 drawGuiContainerForegroundLayer: the pose is translated to the GUI corner. No title labels, as before.
    @Override
    protected void renderLabels(GuiGraphics graphics, int mx, int my) {
        helper.draw2(mx, my);
        ArrayList<String> list = new ArrayList<String>();

        for (Slot slot : menu.slots) {
            if (!slot.hasItem()
                && mx - leftPos >= slot.x && my - topPos >= slot.y
                && mx - leftPos < slot.x + 17 && my - topPos < slot.y + 17) {
                list.clear();

                if (slot instanceof ISlotWithComment) {
                    ((ISlotWithComment) slot).getComment(list);
                    int strWidth = 0;
                    for (String str : list) {
                        int size = font.width(str);
                        if (size > strWidth) strWidth = size;
                    }

                    int xOffset = 0;
                    if (leftPos + slot.x + strWidth + 30 > this.width) {
                        xOffset -= strWidth + 20;
                    }
                    if (!list.isEmpty())
                        GuiDraw.drawTooltip(list, mx - leftPos + xOffset, my - topPos);
                }
            }
        }
    }

    protected void preDraw(float f, int x, int y) {
    }

    protected void postDraw(float f, int x, int y) {
        if (helper.background != null)
            UtilsClient.bindTexture(helper.background);
    }

    protected void drawString(int x, int y, String str) {
        drawString(x, y, 4210752, str);
    }

    protected void drawString(int x, int y, int color, String str) {
        helper.drawString(x, y, color, str);
    }
}
