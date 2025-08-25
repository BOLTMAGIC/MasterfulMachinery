package io.ticticboom.mods.mm.port.item;

import io.ticticboom.mods.mm.port.common.ISlottedPortStorageModel;

import java.util.function.Supplier;

public record ItemPortStorageModel(
        int rows,
        int columns,
        Supplier<Boolean> autoPush
) implements ISlottedPortStorageModel {
}
