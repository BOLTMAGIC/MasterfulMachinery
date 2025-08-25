package io.ticticboom.mods.mm.port.fluid;

import io.ticticboom.mods.mm.port.common.ISlottedPortStorageModel;

import java.util.function.Supplier;

public record FluidPortStorageModel(
    int rows,
    int columns,
    int slotCapacity,
    Supplier<Boolean> autoPush
) implements ISlottedPortStorageModel {
}
