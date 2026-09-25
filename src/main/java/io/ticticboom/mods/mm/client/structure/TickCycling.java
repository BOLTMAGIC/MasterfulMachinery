package io.ticticboom.mods.mm.client.structure;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;

import java.util.List;

public class TickCycling<T> {
    private static final long MILLIS_PER_TICK = 50;

    @Getter
    private final List<T> part;
    // cycle interval in game ticks; driven by wall-clock time so the speed doesn't depend on FPS
    @Setter
    private int interval = 1;
    private long startMillis = Util.getMillis();
    private int index = 0;

    public TickCycling(List<T> part) {
        this.part = part;
    }

    public void tick() {
        if (part.size() <= 1) {
            return;
        }
        long elapsedTicks = (Util.getMillis() - startMillis) / MILLIS_PER_TICK;
        index = (int) ((elapsedTicks / Math.max(1, interval)) % part.size());
    }

    public T next() {
        return part.get(index);
    }

    public void reset() {
        index = 0;
        startMillis = Util.getMillis();
    }
}
