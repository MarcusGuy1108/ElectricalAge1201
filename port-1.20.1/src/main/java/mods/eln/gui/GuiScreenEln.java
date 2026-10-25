package mods.eln.gui;

import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
import mods.eln.gui.IGuiObject.IGuiObjectObserver;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class GuiScreenEln extends Screen implements GuiTextFieldElnObserver, IGuiObjectObserver {

    protected GuiHelper helper;

    protected abstract GuiHelper newHelper();

    protected GuiScreenEln() {
        super(Component.empty());
    }

    // 1.20.1 init() is the 1.7.10 initGui(); subclasses keep overriding initGui.
    @Override
    protected final void init() {
        initGui();
    }

    public void initGui() {
        helper = newHelper();
    }

    public GuiTextFieldEln newGuiTextField(int x, int y, int width) {
        GuiTextFieldEln o = helper.newGuiTextField(x, y, width);
        o.setObserver(this);
        return o;
    }

    public GuiButtonEln newGuiButton(int x, int y, int width, String name) {
        GuiButtonEln o = helper.newGuiButton(x, y, width, name);
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

    public GuiVerticalCustomValuesBar newGuiVerticalCustomValuesBar(int x, int y, int width, int height, Float[] values) {
        GuiVerticalCustomValuesBar o = helper.newGuiVerticalCustomValuesBar(x, y, width, height, values);
        o.setObserver(this);
        return o;
    }

    public GuiVerticalProgressBar newGuiVerticalProgressBar(int x, int y, int width, int height) {
        GuiVerticalProgressBar o = helper.newGuiVerticalProgressBar(x, y, width, height);

        return o;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (helper.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
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
            preDraw(f, x, y);
            helper.mouseMove(x, y);
            helper.draw(x, y, f);
            postDraw(f, x, y);
            helper.draw2(x, y);
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

    protected void preDraw(float f, int x, int y) {
    }

    protected void postDraw(float f, int x, int y) {
    }

    protected void drawString(int x, int y, String str) {
        drawString(x, y, 4210752, str);
    }

    protected void drawString(int x, int y, int color, String str) {
        helper.drawString(x, y, color, str);
    }

    protected void add(IGuiObject o) {
        helper.add(o);
    }

    protected void remove(IGuiObject o) {
        helper.remove(o);
    }
}
