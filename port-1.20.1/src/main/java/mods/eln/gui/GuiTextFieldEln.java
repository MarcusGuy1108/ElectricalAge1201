package mods.eln.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

/**
 * 1.7.10 GuiTextField based text box. Focus and input are routed by the owning GuiHelper; losing focus (click outside
 * or enter) reports the new value to the observer, as in 1.7.10.
 */
public class GuiTextFieldEln extends EditBox implements IGuiObject {

    GuiTextFieldElnObserver observer;
    GuiHelper helper;

    private boolean enabled = true;

    IGuiObjectObserver iGuiObjectObserver;

    public GuiTextFieldEln(Font par1FontRenderer, int x, int y, int w, int h, GuiHelper helper) {
        this(par1FontRenderer, x, y, w, h, helper, 150);
    }

    public GuiTextFieldEln(Font par1FontRenderer, int x, int y, int w, int h, GuiHelper helper, int maxLength) {
        super(par1FontRenderer, x, y, w, h, Component.empty());
        setTextColor(-1);
        setTextColorUneditable(-1);
        setBordered(true);
        setMaxLength(maxLength);
        this.helper = helper;
    }

    public GuiTextFieldEln(Font par1FontRenderer, int x, int y, int w, GuiHelper helper) {
        this(par1FontRenderer, x, y, w, 12, helper);
    }

    @Override
    public int getYMax() {
        return getY() + height;
    }

    public void setObserver(GuiTextFieldElnObserver observer) {
        this.observer = observer;
    }

    public interface GuiTextFieldElnObserver {
        void textFieldNewValue(GuiTextFieldEln textField, String value);
    }

    ArrayList<String> comment = new ArrayList<String>();

    public void setComment(String[] comment) {
        for (String str : comment) {
            this.comment.add(str);
        }
    }

    public void setComment(int line, String comment) {
        if (this.comment.size() < line + 1)
            this.comment.add(line, comment);
        else
            this.comment.set(line, comment);
    }

    public String getText() {
        return getValue();
    }

    public void setText(String value) {
        setValue(value);
    }

    public void setText(float value) {
        if (Math.abs(value) < 1000)
            setText(String.format("%3.2f", value));
        else
            setText(String.format("%3.0f", value));
    }

    public void setText(int value) {
        setText(String.format("%d", value));
    }

    public void setEnabled(boolean par1) {
        enabled = par1;
        setEditable(par1);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public boolean getVisible() {
        return isVisible();
    }

    @Override
    public void setFocused(boolean par1) {
        if (isFocused() && !par1) {
            if (observer != null)
                observer.textFieldNewValue(this, this.getText());
            if (iGuiObjectObserver != null)
                iGuiObjectObserver.guiObjectEvent(this);
        }
        super.setFocused(par1);
    }

    @Override
    public void idraw(int x, int y, float f) {
        if (GuiDraw.graphics != null) render(GuiDraw.graphics, x, y, f);
    }

    @Override
    public boolean ikeyTyped(char key, int code) {
        return false;
    }

    @Override
    public boolean ikeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;
        if (getEnabled() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            setFocused(false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return false;
        keyPressed(keyCode, scanCode, modifiers);
        // Swallow the rest so keys such as the inventory key do not reach the screen while typing.
        return true;
    }

    @Override
    public boolean icharTyped(char c, int modifiers) {
        return isFocused() && charTyped(c, modifiers);
    }

    @Override
    public void imouseClicked(int x, int y, int code) {
        boolean inside = isVisible() && x >= getX() && y >= getY() && x < getX() + width && y < getY() + height;
        setFocused(inside && getEnabled());
        if (inside) onClick(x, y);
    }

    @Override
    public void imouseMove(int x, int y) {
    }

    @Override
    public void imouseMovedOrUp(int x, int y, int witch) {
    }

    @Override
    public void idraw2(int x, int y) {
        if (!isFocused() && isVisible() && x >= getX() && y >= getY() && x < getX() + width && y < getY() + height)
            helper.drawHoveringText(comment, x, y, GuiDraw.getFont());

    }

    @Override
    public void translate(int x, int y) {
        setX(getX() + x);
        setY(getY() + y);
    }

    public void setGuiObserver(IGuiObjectObserver iGuiObjectObserver) {
        this.iGuiObjectObserver = iGuiObjectObserver;
    }
}
