package io.ticticboom.mods.mm.compat.jei.ingredient.emc;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class EmcIngredientHelper implements IIngredientHelper<EmcStack> {
    @Override
    public IIngredientType<EmcStack> getIngredientType() {
        return MMJeiIngredients.PROJECTE_EMC;
    }

    @Override
    public String getDisplayName(EmcStack stack) {
        return "EMC";
    }

    @Override
    public String getUniqueId(EmcStack stack, UidContext uidContext) {
        return "projecte/emc";
    }

    @Override
    public Identifier getResourceLocation(EmcStack stack) {
        return Ref.id("projecte/emc");
    }

    @Override
    public EmcStack copyIngredient(EmcStack stack) {
        return new EmcStack(stack.min(), stack.max());
    }

    @Override
    public String getErrorInfo(@Nullable EmcStack stack) {
        return "ERR";
    }
}
