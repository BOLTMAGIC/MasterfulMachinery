package io.ticticboom.mods.mm.compat.jei.ingredient.matter;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class MatterIngredientNames {

    public static Component displayName(Identifier matter) {
        if (matter == null) {
            return Component.literal("?");
        }
        return Component.translatableWithFallback("replication.matter_type." + matter.getPath(), matter.getPath());
    }
}
