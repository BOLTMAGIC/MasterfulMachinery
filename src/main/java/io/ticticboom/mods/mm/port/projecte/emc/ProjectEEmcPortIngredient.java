package io.ticticboom.mods.mm.port.projecte.emc;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import io.ticticboom.mods.mm.compat.jei.ingredient.emc.EmcStack;
import io.ticticboom.mods.mm.port.ITickSpreadIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.util.LongAmountRange;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class ProjectEEmcPortIngredient implements ITickSpreadIngredient {

    private final LongAmountRange emc;

    public ProjectEEmcPortIngredient(LongAmountRange emc) {
        this.emc = emc;
    }

    @Override
    public long resolveAmount(RecipeStateModel state) {
        return emc.resolve(state);
    }

    @Override
    public long extractFromInputs(RecipeStorages storages, long amount, boolean simulate) {
        long extracted = 0;
        for (ProjectEEmcPortStorage input : storages.getInputStorages(ProjectEEmcPortStorage.class)) {
            extracted += input.extract(amount - extracted, simulate);
            if (extracted >= amount) {
                break;
            }
        }
        return extracted;
    }

    private long insertIntoOutputs(RecipeStorages storages, long amount, boolean simulate) {
        long inserted = 0;
        for (ProjectEEmcPortStorage output : storages.getOutputStorages(ProjectEEmcPortStorage.class)) {
            inserted += output.receive(amount - inserted, simulate);
            if (inserted >= amount) {
                break;
            }
        }
        return inserted;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        long amount = resolveAmount(state);
        return extractFromInputs(storages, amount, true) >= amount;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        extractFromInputs(storages, resolveAmount(state), false);
    }

    @Override
    public void processTick(Level level, RecipeStorages storages, RecipeStateModel state) {
        process(level, storages, state);
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) {
            return false;
        }
        long amount = resolveAmount(state);
        return insertIntoOutputs(storages, amount, true) >= amount;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        insertIntoOutputs(storages, resolveAmount(state), false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        recipeSlot.addIngredient(MMJeiIngredients.PROJECTE_EMC, new EmcStack(emc.min(), emc.max()));
    }

    @Override
    public Component displayName() {
        return Component.translatable("port.mm.projecte_emc.name");
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.PROJECTE_EMC.toString());
        json.addProperty("amountToExtract", emc.max());
        json.addProperty("canRun", canProcess(level, storages, null));
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        json.addProperty("ingredientType", Ref.Ports.PROJECTE_EMC.toString());
        json.addProperty("amountToInsert", emc.max());
        json.addProperty("canRun", canOutput(level, storages, null));
        return json;
    }

    @Override
    public PortContent display() {
        return PortContent.gauge(Component.translatable("port.mm.projecte_emc.name"), ProjectEEmcPortStorage.FILL_COLOR, emc.max(), 0, "EMC");
    }
}
