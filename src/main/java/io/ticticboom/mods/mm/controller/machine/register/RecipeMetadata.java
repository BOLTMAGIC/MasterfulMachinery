package io.ticticboom.mods.mm.controller.machine.register;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

/**
 * Cached recipe requirement metadata for fast pre-checks during recipe scanning.
 * This record stores all the resource and port requirements extracted from a recipe,
 * avoiding expensive re-computation on every tick.
 * All fields are exposed as public via record accessors.
 */
public record RecipeMetadata(
        Set<ResourceLocation> requiredPortTypes,
        Set<ResourceLocation> requiredItemIds,
        Set<ResourceLocation> requiredFluidIds,
        Set<ResourceLocation> requiredMekanismIds,
        boolean needsEnergy,
        boolean needsMana,
        boolean needsPneumatic,
        boolean needsKinetic,
        boolean needsMekanismChemical
) {
    // Record automatically provides public accessors for all fields
    // e.g., metadata.requiredPortTypes(), metadata.needsEnergy(), etc.
}

