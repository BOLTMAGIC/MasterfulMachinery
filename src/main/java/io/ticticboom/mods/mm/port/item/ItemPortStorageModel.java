package io.ticticboom.mods.mm.port.item;

import io.ticticboom.mods.mm.port.common.ISlottedPortStorageModel;

import java.util.function.Supplier;

public record ItemPortStorageModel(
        int rows,
        int columns,
        Supplier<Boolean> autoPush,
        int slotCapacity // 0 = use default per-item max stack size, >0 = override (capped at 1024)
) implements ISlottedPortStorageModel {
}
