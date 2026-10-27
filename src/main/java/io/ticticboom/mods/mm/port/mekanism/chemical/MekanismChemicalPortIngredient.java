package io.ticticboom.mods.mm.port.mekanism.chemical;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import io.ticticboom.mods.mm.Ref;

public class MekanismChemicalPortIngredient implements IPortIngredient {

    protected final ResourceLocation id;
    protected final long amount;

    protected final ChemicalStack stack;
    protected final Chemical chemical;

    /** All four chemical kinds share one stack type and one registry on 1.21.1. */
    public ChemicalStack createStack(Chemical id, long amount) {
        return new ChemicalStack(id, amount);
    }

    public Chemical findChemical(ResourceLocation id) {
        return MekanismAPI.CHEMICAL_REGISTRY.get(id);
    }

    public Class<? extends MekanismChemicalPortStorage> getStorageClass() {
        return MekanismChemicalPortStorage.class;
    }

    public ResourceLocation getTypeId() {
        return Ref.Ports.MEK_CHEMICAL;
    }

    public MekanismChemicalPortIngredient(ResourceLocation chemical, long amount) {
        this.id = chemical;
        this.amount = amount;
        this.chemical = findChemical(id);
        if (this.chemical.isEmptyType()) {
            Ref.LOG.error("Unknown Mekanism chemical '{}' in a recipe. Check the id, or that the mod adding it is installed.", id);
        }
        stack = createStack(this.chemical, amount);
    }

    /**
     * Returns the ResourceLocation id of the chemical this ingredient refers to.
     * Used by controllers to perform lightweight pre-checks by id.
     */
    public ResourceLocation getChemicalId() {
        return this.id;
    }

    @Override
    public PortContent display() {
        return PortContent.chemical(chemical.getTextComponent(), chemical.getIcon(), chemical.getTint(), amount, 0);
    }

    public Chemical getChemical() {
        return chemical;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public boolean canProcess(Level level, RecipeStorages storages, RecipeStateModel state) {
        if (storages == null) return false;
        var inputStorages = storages.getInputStorages(getStorageClass());
        long remaining = amount;
        for (MekanismChemicalPortStorage storage : inputStorages) {
            var stored = storage.chemicalTank.getStack();
            if (stored.isEmpty()) continue;
            if (!stored.getChemical().equals(this.chemical)) continue;
            long available = stored.getAmount();
            long toTake = Math.min(available, remaining);
            remaining -= toTake;
            if (remaining <= 0) break;
        }
        return remaining <= 0;
    }

    @Override
    public void process(Level level, RecipeStorages storages, RecipeStateModel state) {
        var inputStorages = storages.getInputStorages(getStorageClass());
        long remaining = amount;
        for (MekanismChemicalPortStorage storage : inputStorages) {
            var stored = storage.chemicalTank.getStack();
            if (stored.isEmpty()) continue;
            if (!stored.getChemical().equals(this.chemical)) continue;
            long toExtract = Math.min(remaining, stored.getAmount());
            var extracted = storage.extract(toExtract, Action.EXECUTE);
            remaining -= extracted.getAmount();
            if (remaining <= 0) break;
        }
    }

    @Override
    public boolean canSimulateOutput() {
        return true;
    }

    @Override
    public boolean canOutput(Level level, RecipeStorages storages, RecipeStateModel state) {
        var outputStorages = storages.getOutputStoragesByPriority(getStorageClass());
        long remaining = amount;
        for (MekanismChemicalPortStorage storage : outputStorages) {
            var inserted = storage.insert(createStack(this.chemical, remaining), Action.SIMULATE);
            remaining -= inserted.getAmount();
        }
        return remaining <= 0;
    }

    @Override
    public void output(Level level, RecipeStorages storages, RecipeStateModel state) {
        var outputStorages = storages.getOutputStoragesByPriority(getStorageClass());
        long remaining = amount;
        for (MekanismChemicalPortStorage storage : outputStorages) {
            var inserted = storage.insert(createStack(this.chemical, remaining), Action.EXECUTE);
            remaining -= inserted.getAmount();
        }
    }

    @Override
    public JsonObject debugInput(Level level, RecipeStorages storages, JsonObject json) {
        var inputStorages = storages.getInputStorages(getStorageClass());
        var searchedStoragesJson = new JsonArray();

        json.addProperty("ingredientType", getTypeId().toString());
        json.addProperty("amountToInsert", amount);

        long remaining = amount;
        for (MekanismChemicalPortStorage storage : inputStorages) {
            var stored = storage.chemicalTank.getStack();
            if (!stored.isEmpty() && stored.getChemical().equals(this.chemical)) {
                long avail = stored.getAmount();
                long toTake = Math.min(avail, remaining);
                remaining -= toTake;
            }
            searchedStoragesJson.add(storage.getStorageUid().toString());
        }
        json.addProperty("canRun", remaining <= 0);
        json.add("searchedStorages", searchedStoragesJson);
        return json;
    }

    @Override
    public JsonObject debugOutput(Level level, RecipeStorages storages, JsonObject json) {
        var outputStorages = storages.getOutputStoragesByPriority(getStorageClass());
        var searchedStoragesJson = new JsonArray();

        json.addProperty("ingredientType", getTypeId().toString());
        json.addProperty("amountToInsert", amount);

        long remaining = amount;
        for (MekanismChemicalPortStorage storage : outputStorages) {
            var stored = storage.chemicalTank.getStack();
            if (!stored.isEmpty() && stored.getChemical().equals(this.chemical)) {
                long avail = stored.getAmount();
                long toTake = Math.min(avail, remaining);
                remaining -= toTake;
            }
            searchedStoragesJson.add(storage.getStorageUid().toString());
        }
        json.addProperty("canRun", remaining <= 0);
        json.add("searchedStorages", searchedStoragesJson);
        return json;
    }

    @SuppressWarnings("removal")
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeModel model, IFocusGroup focus, IJeiHelpers helpers, SlotGrid grid, IRecipeSlotBuilder recipeSlot) {
        if (!stack.isEmpty()) {
            recipeSlot.addIngredient(MekanismJEI.TYPE_CHEMICAL, stack);
        }
        recipeSlot.addTooltipCallback((a, c) -> c.add(1, Component.translatable("jei.mm.ingredient.mekanism_chemical.amount", amount)));
    }
}
