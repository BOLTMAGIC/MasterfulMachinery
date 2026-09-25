package io.ticticboom.mods.mm.recipe.condition.tier;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import io.ticticboom.mods.mm.structure.StructureTier;
import net.minecraft.network.chat.Component;

/**
 * The formed structure's tier must be within a range: {"type": "mm:tier", "minTier": 2}; either end may be left out.
 * Useful for a recipe given to several structures (structureIds) that only the higher tiers may run.
 */
public class TierRecipeCondition implements IRecipeCondition {
    private final double minTier;
    private final double maxTier;

    public TierRecipeCondition(double minTier, double maxTier) {
        this.minTier = minTier;
        this.maxTier = maxTier;
    }

    public static IRecipeCondition parse(JsonObject json) {
        double min = json.has("minTier") ? json.get("minTier").getAsDouble() : Double.NEGATIVE_INFINITY;
        double max = json.has("maxTier") ? json.get("maxTier").getAsDouble() : Double.POSITIVE_INFINITY;
        return new TierRecipeCondition(min, max);
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        if (ctx.structure() == null) return true;
        double tier = StructureTier.of(ctx.structure());
        return tier >= minTier && tier <= maxTier;
    }

    @Override
    public Component describe() {
        if (maxTier == Double.POSITIVE_INFINITY) return Component.translatable("jei.mm.condition.tier.min", StructureTier.format(minTier));
        if (minTier == Double.NEGATIVE_INFINITY) return Component.translatable("jei.mm.condition.tier.max", StructureTier.format(maxTier));
        return Component.translatable("jei.mm.condition.tier.range", StructureTier.format(minTier), StructureTier.format(maxTier));
    }
}
