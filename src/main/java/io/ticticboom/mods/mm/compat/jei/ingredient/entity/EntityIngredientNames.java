package io.ticticboom.mods.mm.compat.jei.ingredient.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public class EntityIngredientNames {

    public static Component displayName(EntityIngredient ingredient) {
        if (ingredient == null || ingredient.id() == null) {
            return Component.literal("?");
        }
        if (ingredient.tag()) {
            return Component.literal("#" + ingredient.id());
        }
        var type = type(ingredient);
        return type == null ? Component.literal(ingredient.id().toString()) : type.getDescription().copy();
    }

    public static EntityType<?> type(EntityIngredient ingredient) {
        if (ingredient == null || ingredient.id() == null) {
            return null;
        }
        if (!ingredient.tag()) {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(ingredient.id()).orElse(null);
        }
        var tag = BuiltInRegistries.ENTITY_TYPE.getTag(TagKey.create(Registries.ENTITY_TYPE, ingredient.id()));
        return tag.flatMap(holders -> holders.stream().findFirst().map(holder -> (EntityType<?>) holder.value())).orElse(null);
    }

    public static ItemStack icon(EntityIngredient ingredient) {
        var egg = SpawnEggItem.byId(type(ingredient));
        return egg == null ? ItemStack.EMPTY : new ItemStack(egg);
    }
}
