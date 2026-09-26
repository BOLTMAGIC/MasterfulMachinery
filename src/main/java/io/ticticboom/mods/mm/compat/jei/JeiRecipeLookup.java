package io.ticticboom.mods.mm.compat.jei;

import io.ticticboom.mods.mm.port.PortContent;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import org.jetbrains.annotations.Nullable;

/**
 * Opens JEI's recipe screen from MM screens. Only touch this class when JEI is loaded.
 */
public final class JeiRecipeLookup {
    @Nullable
    private static IJeiRuntime runtime;

    private JeiRecipeLookup() {
    }

    static void setRuntime(@Nullable IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    /**
     * @return whether JEI can show recipes for it (items and fluids)
     */
    public static boolean canShow(PortContent content) {
        return switch (content.kind()) {
            case ITEM -> !content.item().isEmpty();
            case FLUID -> !content.fluid().isEmpty();
            default -> false;
        };
    }

    /**
     * @param uses true for the recipes that use it, false for the recipes that make it
     * @return false when JEI couldn't show anything
     */
    public static boolean show(PortContent content, boolean uses) {
        if (runtime == null || !canShow(content)) {
            return false;
        }
        var role = uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        var factory = runtime.getJeiHelpers().getFocusFactory();
        IFocus<?> focus = content.kind() == PortContent.Kind.ITEM
                ? factory.createFocus(role, VanillaTypes.ITEM_STACK, content.item().copyWithCount(1))
                : factory.createFocus(role, ForgeTypes.FLUID_STACK, content.fluid());
        runtime.getRecipesGui().show(focus);
        return true;
    }
}
