package io.ticticboom.mods.mm.compat.jei.ingredient.create;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.ingredient.MMJeiIngredients;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CreateRotationIngredientHelper implements IIngredientHelper<CreateRotationStack> {
    private static final ResourceLocation ingredientId = Ref.id("create-rotation");

    @Override
    public @NotNull IIngredientType<CreateRotationStack> getIngredientType() {
        return MMJeiIngredients.CREATE_ROTATION;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull CreateRotationStack createRotationStack) {
        return "Rotation (Create Mod)";
    }

    @Override
    public @NotNull String getUniqueId(CreateRotationStack createRotationStack, @NotNull UidContext uidContext) {
        return "create-rotation-" + createRotationStack.speed();
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation(@NotNull CreateRotationStack createRotationStack) {
        return ingredientId;
    }

    @Override
    public @NotNull CreateRotationStack copyIngredient(CreateRotationStack createRotationStack) {
        return new CreateRotationStack(createRotationStack.speed());
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable CreateRotationStack createRotationStack) {
        return "Error";
    }
}
