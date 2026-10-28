package io.ticticboom.mods.mm.port.fluid;

import io.ticticboom.mods.mm.port.common.ISlottedPortStorageModel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.function.Supplier;

public record FluidPortStorageModel(
    int rows,
    int columns,
    int slotCapacity,
    Supplier<Boolean> autoPush,
    int tierRank,
    List<String> fluids
) implements ISlottedPortStorageModel {

    public boolean accepts(Fluid fluid) {
        if (fluids.isEmpty()) {
            return true;
        }
        for (String entry : fluids) {
            if (entry.startsWith("#")) {
                var tag = TagKey.create(Registries.FLUID, Identifier.parse(entry.substring(1)));
                if (fluid.builtInRegistryHolder().is(tag)) {
                    return true;
                }
            } else if (Identifier.parse(entry).equals(BuiltInRegistries.FLUID.getKey(fluid))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getTierRank() {
        return tierRank;
    }
}
