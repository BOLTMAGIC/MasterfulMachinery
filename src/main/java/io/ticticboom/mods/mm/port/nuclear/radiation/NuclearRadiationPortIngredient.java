package io.ticticboom.mods.mm.port.nuclear.radiation;

import net.minecraft.network.chat.Component;
import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.radiation.RadiationIngredient;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.level.Level;
import io.ticticboom.mods.mm.util.RadiationText;

public class NuclearRadiationPortIngredient implements IPortIngredient {

    private final String isotope;
    private final double min;
    private final double max;
    private final String rollKey;

    public NuclearRadiationPortIngredient(String isotope, double min, double max, String rollKey) {
        this.isotope = isotope;
        this.min = min;
        this.max = max;
        this.rollKey = rollKey;
    }

    private double amount(RecipeStateModel state) {
        if (max <= min || rollKey == null) {
            return max;
        }
        if (state == null) {
            return max;
        }
        return min + state.getRollToken(rollKey) * (max - min);
    }

    private static boolean satisfied(double remaining, double wanted) {
        return remaining <= wanted * 1.0e-6;
    }

    private double extractAll(RecipeStorages storages, double wanted, boolean simulate) {
        double remaining = wanted;
        for (var storage : storages.getInputStorages(NuclearRadiationPortStorage.class)) {
            remaining -= storage.extract(isotope, remaining, simulate);
            if (satisfied(remaining, wanted)) {
                break;
            }
        }
        return remaining;
    }

    private double insertAll(RecipeStorages storages, double wanted, boolean simulate) {
        double remaining = wanted;
        for (var storage : storages.getOutputStorages(NuclearRadiationPortStorage.class)) {
            remaining -= storage.insert(isotope, remaining, simulate);
            if (satisfied(remaining, wanted)) {
                break;
            }
        }
        return remaining;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        double wanted = amount(state);
        return satisfied(extractAll(storages, wanted, true), wanted);
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        extractAll(storages, amount(state), false);
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        process(level, storages, state);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null || isotope == null) {
            return false;
        }
        double wanted = amount(state);
        return satisfied(insertAll(storages, wanted, true), wanted);
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        insertAll(storages, amount(state), false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.NUCLEAR_RADIATION, new RadiationIngredient(isotope, min, max));
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.NUCLEAR_RADIATION.toString());
        json.addProperty("isotope", isotope == null ? "any" : isotope);
        json.addProperty("min", min);
        json.addProperty("max", max);
        json.addProperty("canRun", canProcess(level, storages, null));
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.NUCLEAR_RADIATION.toString());
        json.addProperty("isotope", isotope == null ? "any" : isotope);
        json.addProperty("min", min);
        json.addProperty("max", max);
        json.addProperty("canRun", canOutput(level, storages, null));
        return json;
    }

    @Override
    public net.minecraft.network.chat.Component displayName() {
        return RadiationText.isotopeName(isotope);
    }

    @Override
    public PortContent display() {
        return PortContent.gauge(Component.translatable("port.mm.nuclear_radiation.name"), 0x5DBB3A, (long) max, 0, "Bq");
    }
}
