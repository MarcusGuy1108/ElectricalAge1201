package mods.eln.gui;

public interface IGuiObject {

    public void idraw(int x, int y, float f);

    public void idraw2(int x, int y);

    public boolean ikeyTyped(char key, int code);

    // 1.20.1 splits 1.7.10 keyTyped into key presses (GLFW key codes) and typed characters.
    default boolean ikeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean icharTyped(char c, int modifiers) {
        return ikeyTyped(c, 0);
    }

    public void imouseClicked(int x, int y, int code);

    public void imouseMove(int x, int y);

    public void imouseMovedOrUp(int x, int y, int witch);

    public interface IGuiObjectObserver {
        public void guiObjectEvent(IGuiObject object);
    }

    public void translate(int x, int y);

    public int getYMax();
}
