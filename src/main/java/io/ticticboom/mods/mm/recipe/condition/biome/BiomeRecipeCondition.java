package io.ticticboom.mods.mm.recipe.condition.biome;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.recipe.condition.IRecipeCondition;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * The machine must stand in a biome, or a biome tag with a leading '#': {"type": "mm:biome", "biome": "#minecraft:is_ocean"}.
 */
public class BiomeRecipeCondition implements IRecipeCondition {
    private final String biome;
    @Nullable
    private final TagKey<Biome> tag;
    @Nullable
    private final ResourceKey<Biome> key;

    public BiomeRecipeCondition(String biome) {
        this.biome = biome;
        boolean isTag = biome.startsWith("#");
        var id = ResourceLocation.tryParse(isTag ? biome.substring(1) : biome);
        if (id == null) {
            throw new IllegalArgumentException("Invalid biome or biome tag in recipe condition: " + biome);
        }
        this.tag = isTag ? TagKey.create(Registries.BIOME, id) : null;
        this.key = isTag ? null : ResourceKey.create(Registries.BIOME, id);
    }

    public static IRecipeCondition parse(JsonObject json) {
        return new BiomeRecipeCondition(json.get("biome").getAsString());
    }

    @Override
    public boolean canRun(RecipeConditionContext ctx) {
        if (ctx.pos() == null) return true;
        var holder = ctx.level().getBiome(ctx.pos());
        return tag != null ? holder.is(tag) : holder.is(key);
    }

    @Override
    public Component describe() {
        return Component.translatable("jei.mm.condition.biome", biome);
    }
}
