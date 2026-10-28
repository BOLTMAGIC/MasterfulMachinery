package io.ticticboom.mods.mm.port.common;

public class PortPager {

    private final PortGrid grid;
    private int page = 0;

    public PortPager(PortGrid grid) {
        this.grid = grid;
    }

    public PortGrid grid() {
        return grid;
    }

    public int page() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, Math.min(grid.pages() - 1, page));
    }
}
