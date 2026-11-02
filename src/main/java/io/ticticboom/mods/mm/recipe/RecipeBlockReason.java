package io.ticticboom.mods.mm.recipe;

import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.count.CountRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Why a recipe of a machine can't start (or a running one can't go on), for the controller screen and Jade.
 * Only the recipe, the kind and the entry's place in the recipe are kept: clients know the recipes, so they
 * look up what the entry needs themselves.
 *
 * @param index the failing condition, input or output entry of the recipe; -1 when no single entry is to blame
 *              (outputs that each fit alone but not together)
 */
public record RecipeBlockReason(ResourceLocation recipeId, Kind kind, int index) {

    public enum Kind {
        /** A recipe condition (time, weather, biome, redstone...) doesn't hold. */
        CONDITION,
        /** An input isn't in the input ports, or not enough of it. */
        INPUT,
        /** An output doesn't fit in the output ports: they are full or too small. */
        OUTPUT
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putString("Recipe", recipeId.toString());
        tag.putString("Kind", kind.name().toLowerCase(Locale.ROOT));
        tag.putInt("Index", index);
        return tag;
    }

    @Nullable
    public static RecipeBlockReason load(CompoundTag tag) {
        var id = ResourceLocation.tryParse(tag.getString("Recipe"));
        if (id == null) {
            return null;
        }
        Kind kind;
        try {
            kind = Kind.valueOf(tag.getString("Kind").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new RecipeBlockReason(id, kind, tag.getInt("Index"));
    }

    @Nullable
    public RecipeModel recipe() {
        return MachineRecipeManager.RECIPES.get(recipeId);
    }

    /**
     * @return what the failing entry needs or makes, with its amount, or null when it has nothing to show
     * (conditions, flex inputs, outputs that don't fit together)
     */
    @Nullable
    public PortContent content() {
        RecipeModel recipe = recipe();
        if (recipe == null || index < 0) {
            return null;
        }
        if (kind == Kind.INPUT && index < recipe.inputs().inputs().size()) {
            var entry = recipe.inputs().inputs().get(index);
            if (entry instanceof ConsumeRecipeIngredientEntry consume) {
                return consume.getIngredient().display();
            }
            if (entry instanceof CountRecipeIngredientEntry count) {
                return count.getIngredient().display();
            }
        }
        if (kind == Kind.OUTPUT && index < recipe.outputs().outputs().size()) {
            var outputs = recipe.outputs().outputs().get(index).displayedOutputs();
            if (!outputs.isEmpty()) {
                DisplayedOutput first = outputs.get(0);
                return first.ingredient().display();
            }
        }
        return null;
    }

    /**
     * The headline (client side): "Missing input", "Not enough energy", "Output doesn't fit", "Condition not met",
     * followed by what the recipe makes ("Missing input for 256 Diamatine Crystal Block") when it isn't the output
     * that is in the way.
     */
    public Component title() {
        String key = "gui.mm.controller.blocked." + kind.name().toLowerCase(Locale.ROOT);
        if (kind == Kind.INPUT) {
            PortContent content = content();
            if (content != null && content.kind() == PortContent.Kind.ENERGY) {
                key = "gui.mm.controller.blocked.energy";
            }
        }
        PortContent product = kind == Kind.OUTPUT ? null : product();
        return product == null ? Component.translatable(key) : Component.translatable(key + ".for", amountText(product));
    }

    /** The recipe's first output, or null when it shows none. */
    @Nullable
    private PortContent product() {
        RecipeModel recipe = recipe();
        if (recipe == null) {
            return null;
        }
        for (var entry : recipe.outputs().outputs()) {
            for (DisplayedOutput output : entry.displayedOutputs()) {
                PortContent content = output.ingredient().display();
                if (content != null) {
                    return content;
                }
            }
        }
        return null;
    }

    /** "256 Block of Diamond", "64 000 FE" */
    private static Component amountText(PortContent content) {
        Component name = content.kind() == PortContent.Kind.ENERGY ? Component.translatable("gui.mm.port.energy")
                : content.name() != null ? content.name() : Component.empty();
        String unit = content.unit().isEmpty() ? "" : " " + content.unit();
        return Component.literal(CountFormat.grouped(content.amount()) + unit + " ").append(name);
    }

    /**
     * The second line (client side): what the entry needs or makes with its amount ("16 Iron Ingot"), the condition
     * that doesn't hold, or else the recipe's id.
     */
    public Component detail() {
        RecipeModel recipe = recipe();
        if (kind == Kind.CONDITION && recipe != null) {
            var lines = recipe.conditions().describe();
            if (index >= 0 && index < lines.size()) {
                return lines.get(index);
            }
        }
        PortContent content = content();
        if (content != null) {
            return amountText(content);
        }
        return Component.literal(recipeId.toString());
    }
}
