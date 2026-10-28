package io.ticticboom.mods.mm.compat.jei.ingredient.source;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class ArsSourceIngredientHelper implements IIngredientHelper<ArsSourceStack> {
    @Override
    public IIngredientType<ArsSourceStack> getIngredientType() {
        return MMJeiIngredients.ARS_SOURCE;
    }

    @Override
    public String getDisplayName(ArsSourceStack stack) {
        return "Source";
    }

    @Override
    public String getUniqueId(ArsSourceStack stack, UidContext uidContext) {
        return "ars_nouveau/source";
    }

    @Override
    public Identifier getResourceLocation(ArsSourceStack stack) {
        return Ref.id("ars_nouveau/source");
    }

    @Override
    public ArsSourceStack copyIngredient(ArsSourceStack stack) {
        return new ArsSourceStack(stack.min(), stack.max());
    }

    @Override
    public String getErrorInfo(@Nullable ArsSourceStack stack) {
        return "ERR";
    }
}
