package io.ticticboom.mods.mm.compat.jei;

public class SlotGridEntry {
    public final int x;
    public final int y;

    private boolean used;
    // Badge flag set when the ingredient/slot should show a small 'x' in JEI (e.g. 0% consumption)
    private boolean badgeNotUsed = false;
    public SlotGridEntry(int x, int y) {

        this.x = x;
        this.y = y;
    }

    public int getInnerX() {
        return x + 1;
    }

    public int getInnerY() {
        return y + 1;
    }

    public void setUsed() {
        used = true;
    }

    public boolean used() {
        return used;
    }

    // Badge accessors
    public void setBadgeNotUsed() {
        this.badgeNotUsed = true;
    }

    public boolean hasBadgeNotUsed() {
        return this.badgeNotUsed;
    }
}
