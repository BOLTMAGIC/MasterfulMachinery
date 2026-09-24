package io.ticticboom.mods.mm.port.common;

/**
 * Positions shared by port menus (server) and screens (client), relative to the GUI's top-left corner.
 * The window keeps its standard size for grids up to 9x6; bigger grids widen and heighten it so the
 * slots never overlap the title or the player inventory.
 */
public final class PortGuiLayout {
    /** Width of the port GUI background. */
    public static final int WIDTH = 174;
    /** Height of the port GUI background. */
    public static final int HEIGHT = 222;
    /** First row below the title line. */
    public static final int CONTENT_TOP = 18;
    /** Last row above the player inventory. */
    public static final int CONTENT_BOTTOM = 136;
    /** Big tank gauge (fluid / chemical ports), same frame as the energy port's bar. */
    public static final int TANK_X = 7;
    public static final int TANK_Y = 24;
    public static final int TANK_W = 162;
    public static final int TANK_H = 80;

    private static final int CONTENT_W = 162;

    private PortGuiLayout() {
    }

    /** Window width for a slot grid with the given number of columns. */
    public static int width(int columns) {
        return Math.max(CONTENT_W, columns * 18) + WIDTH - CONTENT_W;
    }

    /** Height added below the title so a grid with the given number of rows fits. */
    public static int extraHeight(int rows) {
        return Math.max(0, rows * 18 + 4 - (CONTENT_BOTTOM - CONTENT_TOP));
    }

    /** Horizontal shift of the player inventory so it stays centred in a widened window. */
    public static int inventoryOffsetX(int columns) {
        return (width(columns) - WIDTH) / 2;
    }

    /** X of the first slot so a grid of the given width is centred. */
    public static int slotGridX(int columns) {
        return (Math.max(CONTENT_W, columns * 18) - columns * 18) / 2 + 8;
    }

    /** Y of the first slot so a grid of the given height sits between the title and the inventory. */
    public static int slotGridY(int rows) {
        return CONTENT_TOP + (CONTENT_BOTTOM + extraHeight(rows) - CONTENT_TOP - rows * 18) / 2 + 1;
    }
}
