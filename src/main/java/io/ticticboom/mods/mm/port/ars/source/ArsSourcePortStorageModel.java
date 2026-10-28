package io.ticticboom.mods.mm.port.ars.source;

import io.ticticboom.mods.mm.port.IPortStorageModel;

public record ArsSourcePortStorageModel(
        int capacity,
        int range
) implements IPortStorageModel {
}
