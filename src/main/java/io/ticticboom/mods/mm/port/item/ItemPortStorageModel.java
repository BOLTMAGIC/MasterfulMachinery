package io.ticticboom.mods.mm.port.item;

import io.ticticboom.mods.mm.port.common.ISlottedPortStorageModel;

import java.util.function.Supplier;

public record ItemPortStorageModel(
        int rows,
        int columns,
        Supplier<Boolean> autoPush,
        int slotCapacity, // 0 = item default, >0 = per-slot limit (up to 16,384)
        int tierRank
) implements ISlottedPortStorageModel {
    private static final int HIGH_CAPACITY_MAX_ROWS = 6;
    private static final int HIGH_CAPACITY_MAX_COLUMNS = 8;

    /** Keep high-capacity ports at the Colossal-sized grid; each slot keeps its item capacity. */
    public ItemPortStorageModel {
        if (slotCapacity >= 512) {
            rows = Math.min(rows, HIGH_CAPACITY_MAX_ROWS);
            columns = Math.min(columns, HIGH_CAPACITY_MAX_COLUMNS);
        }
    }

    @Override
    public int getTierRank() {
        return tierRank;
    }
}
