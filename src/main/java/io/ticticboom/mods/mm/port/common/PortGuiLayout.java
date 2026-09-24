package io.ticticboom.mods.mm.port.common;

/**
 * Positions shared by port menus (server) and screens (client), relative to the GUI's top-left corner.
 */
public final class PortGuiLayout {
    /** Width of the port GUI background. */
    public static final int WIDTH = 174;
    /** First row below the title line. */
    public static final int CONTENT_TOP = 18;
    /** Last row above the player inventory. */
    public static final int CONTENT_BOTTOM = 136;
    /** Big tank gauge (fluid / chemical ports), same frame as the energy port's bar. */
    public static final int TANK_X = 7;
    public static final int TANK_Y = 24;
    public static final int TANK_W = 162;
    public static final int TANK_H = 80;

    private PortGuiLayout() {
    }

    /** X of the first slot so a grid of the given width is centred. */
    public static int slotGridX(int columns) {
        return (162 - columns * 18) / 2 + 8;
    }

    /** Y of the first slot so a grid of the given height sits between the title and the inventory. */
    public static int slotGridY(int rows) {
        return CONTENT_TOP + (CONTENT_BOTTOM - CONTENT_TOP - rows * 18) / 2 + 1;
    }
}
