package mods.eln.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/**
 * 1.7.10 GuiButton based button; clicks are routed by the owning GuiHelper rather than the vanilla screen.
 */
public class GuiButtonEln extends AbstractButton implements IGuiObject {

    IGuiObjectObserver observer;
    private boolean playPressSound = true;
    private boolean pressedInside = false;

    public GuiButtonEln(int x, int y, int width, int height, String str) {
        super(x, y, width, height, Component.literal(str));
    }

    GuiHelper helper;

    public void setHelper(GuiHelper helper) {
        this.helper = helper;
    }

    public void setObserver(IGuiObjectObserver observer) {
        this.observer = observer;
    }

    public GuiButtonEln setPlayPressSound(boolean playPressSound) {
        this.playPressSound = playPressSound;
        return this;
    }

    // 1.7.10 field names, as accessors so Kotlin GUIs keep writing button.displayString / button.enabled.
    public String getDisplayString() {
        return getMessage().getString();
    }

    public void setDisplayString(String displayString) {
        setMessage(Component.literal(displayString));
    }

    public boolean getEnabled() {
        return active;
    }

    public void setEnabled(boolean enabled) {
        active = enabled;
    }

    @Override
    public void onPress() {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    public void idraw(int x, int y, float f) {
        if (GuiDraw.graphics != null) render(GuiDraw.graphics, x, y, f);
    }

    @Override
    public int getYMax() {
        return getY() + height;
    }

    @Override
    public boolean ikeyTyped(char key, int code) {
        return false;
    }

    public void onMouseClicked() {
    }

    @Override
    public void imouseClicked(int x, int y, int code) {
        if (code != 0) {
            return;
        }
        pressedInside = active && visible && clicked(x, y);
    }

    @Override
    public void imouseMovedOrUp(int x, int y, int witch) {
        if (witch != 0) {
            return;
        }
        boolean shouldActivate = pressedInside && active && visible
            && x >= getX() && y >= getY() && x < getX() + width && y < getY() + height;
        pressedInside = false;
        if (shouldActivate) {
            if (playPressSound) {
                playDownSound(Minecraft.getInstance().getSoundManager());
            }
            onMouseClicked();
            if (observer != null) {
                observer.guiObjectEvent(this);
            }
        }
    }

    @Override
    public void imouseMove(int x, int y) {
    }

    @Override
    public void idraw2(int x, int y) {
        if (helper != null && visible && x >= getX() && y >= getY() && x < getX() + width && y < getY() + height)
            helper.drawHoveringText(comment, x, y, GuiDraw.getFont());
    }

    @Override
    public void translate(int x, int y) {
        setX(getX() + x);
        setY(getY() + y);
    }

    ArrayList<String> comment = new ArrayList<String>();

    public void setComment(int line, String comment) {
        if (this.comment.size() < line + 1)
            this.comment.add(line, comment);
        else
            this.comment.set(line, comment);
    }

    public void clearComment() {
        comment.clear();
    }
}
