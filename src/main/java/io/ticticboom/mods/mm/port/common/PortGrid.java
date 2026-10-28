package io.ticticboom.mods.mm.port.common;

import io.ticticboom.mods.mm.util.BlockUtils;

public record PortGrid(int total, int columns, int rows, int perPage, int pages) {

    public static final int MAX_COLUMNS = 9;
    private static final int MAX_ROWS = 6;
    private static final int PAGED_ROWS = 5;

    public static PortGrid of(ISlottedPortStorageModel model) {
        return of(model.rows(), model.columns());
    }

    public static PortGrid of(int rows, int columns) {
        int total = Math.max(0, rows) * Math.max(0, columns);
        int cols = Math.max(1, Math.min(MAX_COLUMNS, Math.max(columns, Math.ceilDiv(total, MAX_ROWS))));
        int needed = Math.ceilDiv(total, cols);
        if (needed <= MAX_ROWS) {
            return new PortGrid(total, cols, needed, Math.max(1, total), 1);
        }
        int perPage = cols * PAGED_ROWS;
        return new PortGrid(total, cols, PAGED_ROWS, perPage, Math.ceilDiv(total, perPage));
    }

    public int firstSlot(int page) {
        return page * perPage;
    }

    public int slotsOnPage(int page) {
        return Math.max(0, Math.min(perPage, total - firstSlot(page)));
    }

    public int pageOf(int slot) {
        return slot / perPage;
    }

    public int originX() {
        return BlockUtils.slotGridOriginX(columns);
    }

    public int originY() {
        return BlockUtils.slotGridOriginY(rows);
    }

    public int slotX(int indexOnPage) {
        return (indexOnPage % columns) * 18 + originX();
    }

    public int slotY(int indexOnPage) {
        return (indexOnPage / columns) * 18 + originY();
    }
}
