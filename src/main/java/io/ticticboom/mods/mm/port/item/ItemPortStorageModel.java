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
    @Override
    public int getTierRank() {
        return tierRank;
    }
}
