package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortIngredient;
import io.ticticboom.mods.mm.port.energy.EnergyPortIngredient;
import io.ticticboom.mods.mm.port.fluid.FluidPortIngredient;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.port.kinetic.CreateKineticPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortIngredient;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * What a recipe needs from the machine's input ports, read once from its inputs.
 * Only a quick filter: a recipe that passes can still lack amounts or fail its conditions.
 */
public record RecipeRequirements(
        Set<ResourceLocation> portTypes,
        Set<ResourceLocation> itemIds,
        Set<ResourceLocation> fluidIds,
        Set<ResourceLocation> mekanismIds,
        boolean needsEnergy,
        boolean needsMana,
        boolean needsPneumatic,
        boolean needsKinetic,
        boolean needsMekanismChemical
) {
    public static RecipeRequirements of(RecipeModel recipe) {
        Set<ResourceLocation> portTypes = new HashSet<>();
        Set<ResourceLocation> itemIds = new HashSet<>();
        Set<ResourceLocation> fluidIds = new HashSet<>();
        Set<ResourceLocation> mekanismIds = new HashSet<>();
        boolean needsEnergy = false;
        boolean needsMana = false;
        boolean needsPneumatic = false;
        boolean needsKinetic = false;
        boolean needsMekanismChemical = false;
        for (var input : recipe.inputs().inputs()) {
            if (!(input instanceof ConsumeRecipeIngredientEntry cre)) continue;
            var ingr = cre.getIngredient();
            if (ingr instanceof BaseItemPortIngredient) {
                portTypes.add(Ref.Ports.ITEM);
                if (ingr instanceof SingleItemPortIngredient single) {
                    try { var id = single.getItemId(); if (id != null) itemIds.add(id); } catch (Throwable ignored) { }
                }
            } else if (ingr instanceof FluidPortIngredient fp) {
                portTypes.add(Ref.Ports.FLUID);
                try { var id = fp.getFluidId(); if (id != null) fluidIds.add(id); } catch (Throwable ignored) { }
            } else if (ingr instanceof EnergyPortIngredient) {
                portTypes.add(Ref.Ports.ENERGY);
                needsEnergy = true;
            } else if (ingr instanceof BotaniaManaPortIngredient) {
                portTypes.add(Ref.Ports.BOTANIA_MANA);
                needsMana = true;
            } else if (ingr instanceof PneumaticAirPortIngredient) {
                portTypes.add(Ref.Ports.PNEUMATIC_AIR);
                needsPneumatic = true;
            } else if (ingr instanceof CreateKineticPortIngredient) {
                portTypes.add(Ref.Ports.CREATE_KINETIC);
                needsKinetic = true;
            } else //noinspection rawtypes
                if (ingr instanceof MekanismChemicalPortIngredient mech) {
                try { var typeId = mech.getTypeId(); if (typeId != null) portTypes.add(typeId); } catch (Throwable ignored) { }
                try {
                    var chemId = mech.getChemicalId();
                    if (chemId != null) mekanismIds.add(chemId);
                    needsMekanismChemical = true;
                } catch (Throwable ignored) { }
            }
        }
        return new RecipeRequirements(portTypes, itemIds, fluidIds, mekanismIds,
                needsEnergy, needsMana, needsPneumatic, needsKinetic, needsMekanismChemical);
    }

    /**
     * @param availablePortTypes the controller's port types, null when unknown (then not checked)
     */
    public boolean hasPortTypes(@Nullable Set<ResourceLocation> availablePortTypes) {
        return portTypes.isEmpty() || availablePortTypes == null || availablePortTypes.containsAll(portTypes);
    }

    /**
     * @return false if the ports surely lack something this recipe consumes
     */
    public boolean isMetBy(StorageCacheManager.StorageCache cache) {
        if (!itemIds.isEmpty() && !cache.availableItemIds.containsAll(itemIds)) return false;
        if (!fluidIds.isEmpty() && !cache.availableFluidIds.containsAll(fluidIds)) return false;
        if (needsEnergy && !cache.hasEnergyAvailable) return false;
        if (needsMana && !cache.hasManaAvailable) return false;
        if (needsPneumatic && !cache.hasPneumaticAir) return false;
        if (needsKinetic && !cache.hasKinetic) return false;
        if (needsMekanismChemical && !cache.hasMekanismChemical) return false;
        return mekanismIds.isEmpty() || cache.availableMekanismIds.containsAll(mekanismIds);
    }
}
