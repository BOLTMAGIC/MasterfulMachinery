package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.port.fluid.FluidPortIngredient;
import io.ticticboom.mods.mm.port.energy.EnergyPortIngredient;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortIngredient;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortIngredient;
import io.ticticboom.mods.mm.port.kinetic.CreateKineticPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortIngredient;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import net.minecraft.resources.ResourceLocation;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to compute recipe metadata from recipe definitions.
 * Extracted for code organization and reusability.
 */
public class RecipeMetadataCompute {

    /**
     * Compute recipe requirements once and cache them
     */
    public static RecipeMetadata computeRecipeMetadata(RecipeModel recipe) {
        Set<ResourceLocation> requiredTypes = new HashSet<>();
        Set<ResourceLocation> requiredItemIds = new HashSet<>();
        Set<ResourceLocation> requiredFluidIds = new HashSet<>();
        Set<ResourceLocation> requiredMekanismIds = new HashSet<>();
        boolean needsEnergy = false;
        boolean needsMana = false;
        boolean needsPneumatic = false;
        boolean needsKinetic = false;
        boolean needsMekanismChemical = false;

        // Extract port types
        for (var input : recipe.inputs().inputs()) {
            if (input instanceof ConsumeRecipeIngredientEntry cre) {
                var ingr = cre.getIngredient();
                if (ingr instanceof BaseItemPortIngredient) requiredTypes.add(Ref.Ports.ITEM);
                else if (ingr instanceof FluidPortIngredient) requiredTypes.add(Ref.Ports.FLUID);
                else if (ingr instanceof EnergyPortIngredient) requiredTypes.add(Ref.Ports.ENERGY);
                else if (ingr instanceof BotaniaManaPortIngredient) requiredTypes.add(Ref.Ports.BOTANIA_MANA);
                else if (ingr instanceof PneumaticAirPortIngredient) requiredTypes.add(Ref.Ports.PNEUMATIC_AIR);
                else if (ingr instanceof CreateKineticPortIngredient) requiredTypes.add(Ref.Ports.CREATE_KINETIC);
                else //noinspection rawtypes
                    if (ingr instanceof MekanismChemicalPortIngredient mech) {
                    try { var typeId = mech.getTypeId(); if (typeId != null) requiredTypes.add(typeId); }
                    catch (Throwable ignored) { }
                }
            }
        }

        // Extract specific resource IDs
        for (var input : recipe.inputs().inputs()) {
            if (input instanceof ConsumeRecipeIngredientEntry cre) {
                var ingr = cre.getIngredient();
                if (ingr instanceof BaseItemPortIngredient) {
                    if (ingr instanceof SingleItemPortIngredient single) {
                        try { var id = single.getItemId(); if (id != null) requiredItemIds.add(id); } catch (Throwable ignored) {}
                    }
                } else if (ingr instanceof FluidPortIngredient fp) {
                    try { var id = fp.getFluidId(); if (id != null) requiredFluidIds.add(id); } catch (Throwable ignored) {}
                } else if (ingr instanceof EnergyPortIngredient) needsEnergy = true;
                else if (ingr instanceof BotaniaManaPortIngredient) needsMana = true;
                else if (ingr instanceof PneumaticAirPortIngredient) needsPneumatic = true;
                else if (ingr instanceof CreateKineticPortIngredient) needsKinetic = true;
                else //noinspection rawtypes
                    if (ingr instanceof MekanismChemicalPortIngredient mech) {
                    try {
                        var chemId = mech.getChemicalId();
                        if (chemId != null) requiredMekanismIds.add(chemId);
                        needsMekanismChemical = true;
                    } catch (Throwable ignored) { }
                }
            }
        }

        return new RecipeMetadata(requiredTypes, requiredItemIds, requiredFluidIds, requiredMekanismIds,
                                  needsEnergy, needsMana, needsPneumatic, needsKinetic, needsMekanismChemical);
    }
}

