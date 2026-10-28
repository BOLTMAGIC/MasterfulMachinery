package io.ticticboom.mods.mm.port.projecte.emc;

import io.ticticboom.mods.mm.port.IPortStorageModel;

public record ProjectEEmcPortStorageModel(
        long capacity,
        boolean kleinSlot,
        long kleinRate
) implements IPortStorageModel {
}
