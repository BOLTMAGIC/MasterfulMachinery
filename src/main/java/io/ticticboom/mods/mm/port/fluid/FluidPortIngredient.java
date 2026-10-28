package io.ticticboom.mods.mm.port.fluid;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.AmountRange;
import lombok.Getter;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.core.registries.BuiltInRegistries;

public class FluidPortIngredient implements IPortIngredient {

    private final AmountRange amount;
    private final Fluid fluid;
    @Getter
    private final Identifier fluidId;

    public FluidPortIngredient(Identifier fluidId, AmountRange amount) {
        this.amount = amount;
        this.fluidId = fluidId;
        fluid = BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == null) {
            throw new RuntimeException(String.format("Could not find fluid [%s] which is required by an MM recipe", fluidId));
        }
    }

    @Override
    public IPortIngredient scaled(double factor) {
        return new FluidPortIngredient(fluidId, amount.scaled(factor));
    }

    @Override
    public PortContent display() {
        return PortContent.fluid(new FluidStack(fluid, amount.max()), 0);
    }

    @Override
    public AmountRange getAmountRange() {
        return amount;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        int required = amount.resolve(state);
        if (required <= 0) return true; // nothing to drain
        if (storages == null) return false;
        var fluidStorages = storages.getInputStorages(FluidPortStorage.class);
        if (fluidStorages.isEmpty()) return false;
        int remaining = required;
        for (FluidPortStorage storage : fluidStorages) {
            if (remaining <= 0) break; // early exit when we've satisfied the amount
            var handler = storage.getHandler();
            if (handler == null) continue;
            var drained = handler.drain(new FluidStack(fluid, remaining), IFluidHandler.FluidAction.SIMULATE);
            remaining -= drained.getAmount();
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        int required = amount.resolve(state);
        if (required <= 0) return;
        if (storages == null) return;
        var fluidStorages = storages.getInputStorages(FluidPortStorage.class);
        if (fluidStorages.isEmpty()) return;
        int remaining = required;
        for (FluidPortStorage storage : fluidStorages) {
            if (remaining <= 0) break;
            var handler = storage.getHandler();
            if (handler == null) continue;
            var drained = handler.drain(new FluidStack(fluid, remaining), IFluidHandler.FluidAction.EXECUTE);
            remaining -= drained.getAmount();
        }
    }

    @Override
    public io.ticticboom.mods.mm.pattern.PatternIngredient asPatternIngredient() {
        return amount.isRanged() ? null : new io.ticticboom.mods.mm.pattern.PatternIngredient(
                io.ticticboom.mods.mm.pattern.PatternIngredient.Kind.FLUID, fluidId, amount.min());
    }

    @Override
    public boolean canSimulateOutput() {
        return true;
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        int required = amount.resolve(state);
        if (required <= 0) return true; // nothing to output
        if (storages == null) return false;
        var fluidStorages = storages.getOutputStoragesByPriority(FluidPortStorage.class);
        if (fluidStorages.isEmpty()) return false;
        int remaining = required;
        for (FluidPortStorage storage : fluidStorages) {
            if (remaining <= 0) break;
            var handler = storage.getHandler();
            if (handler == null) continue;
            var filled = handler.fill(new FluidStack(fluid, remaining), IFluidHandler.FluidAction.SIMULATE);
            remaining -= filled;
        }
        return remaining <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        int required = amount.resolve(state);
        if (required <= 0) return;
        if (storages == null) return;
        var fluidStorages = storages.getOutputStoragesByPriority(FluidPortStorage.class);
        if (fluidStorages.isEmpty()) return;
        int remaining = required;
        for (FluidPortStorage storage : fluidStorages) {
            if (remaining <= 0) break;
            var handler = storage.getHandler();
            if (handler == null) continue;
            var filled = handler.fill(new FluidStack(fluid, remaining), IFluidHandler.FluidAction.EXECUTE);
            remaining -= filled;
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.FLUID, new FluidStack(fluid, amount.max()));
        recipeSlot.addRichTooltipCallback((a, b) -> b.add(Component.translatable("jei.mm.ingredient.fluid.amount", amount.toString())));
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        var fluidStorages = storages.getInputStorages(FluidPortStorage.class);
        var searchedStorages = new JsonArray();
        var searchIterations = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.FLUID.toString());
        amount.addToDebug(json, "amountToDrain", null);

        int remaining = amount.max();
        for (FluidPortStorage storage : fluidStorages) {
            var iterJson = new JsonObject();

            var drained = storage.getHandler().drain(new FluidStack(fluid, remaining), IFluidHandler.FluidAction.SIMULATE);
            remaining -= drained.getAmount();

            var drainedRes = JsonOps.INSTANCE.withEncoder(FluidStack.OPTIONAL_CODEC).apply(drained);

            if (drainedRes.result().isPresent()) {
                iterJson.add("drainedFluidStack", drainedRes.result().get());
            } else {
                iterJson.addProperty("failedToSerializeFluidStack", true);
            }
            iterJson.addProperty("drainedAmount", drained.getAmount());
            iterJson.addProperty("remainingToDrain", remaining);
            iterJson.addProperty("storageUid", storage.getStorageUid().toString());

            searchIterations.add(iterJson);
            searchedStorages.add(storage.getStorageUid().toString());
        }
        json.add("drainIterations", searchIterations);
        json.addProperty("canRun", remaining <= 0);
        json.add("searchedStorages", searchedStorages);
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        var fluidStorages = storages.getOutputStoragesByPriority(FluidPortStorage.class);
        var searchedStorages = new JsonArray();
        var searchIterations = new JsonArray();
        json.addProperty("ingredientType", Ref.Ports.FLUID.toString());
        amount.addToDebug(json, "amountToFill", null);

        int remaining = amount.max();
        for (FluidPortStorage storage : fluidStorages) {
            var iterJson = new JsonObject();

            var filled = storage.getHandler().fill(new FluidStack(fluid, remaining), IFluidHandler.FluidAction.SIMULATE);
            remaining -= filled;

            iterJson.addProperty("filledAmount", filled);
            iterJson.addProperty("remainingToFill", remaining);
            iterJson.addProperty("storageUid", storage.getStorageUid().toString());

            searchIterations.add(iterJson);
            searchedStorages.add(storage.getStorageUid().toString());
        }
        json.add("fillIterations", searchIterations);
        json.addProperty("canRun", remaining <= 0);
        json.add("searchedStorages", searchedStorages);
        return json;
    }

    @Override
    public net.minecraft.network.chat.Component displayName() {
        return new net.neoforged.neoforge.fluids.FluidStack(
                net.minecraft.core.registries.BuiltInRegistries.FLUID.get(getFluidId()), 1).getHoverName();
    }
}
