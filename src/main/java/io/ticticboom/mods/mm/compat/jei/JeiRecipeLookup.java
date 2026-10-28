package io.ticticboom.mods.mm.compat.jei;

import io.ticticboom.mods.mm.port.PortContent;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import org.jetbrains.annotations.Nullable;

public final class JeiRecipeLookup {
    @Nullable
    private static IJeiRuntime runtime;

    private JeiRecipeLookup() {
    }

    static void setRuntime(@Nullable IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static boolean canShow(PortContent content) {
        return switch (content.kind()) {
            case ITEM -> !content.item().isEmpty();
            case FLUID -> !content.fluid().isEmpty();
            default -> false;
        };
    }

    public static boolean show(PortContent content, boolean uses) {
        if (runtime == null || !canShow(content)) {
            return false;
        }
        var role = uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        var factory = runtime.getJeiHelpers().getFocusFactory();
        IFocus<?> focus = content.kind() == PortContent.Kind.ITEM
                ? factory.createFocus(role, VanillaTypes.ITEM_STACK, content.item().copyWithCount(1))
                : factory.createFocus(role, NeoForgeTypes.FLUID_STACK, content.fluid());
        runtime.getRecipesGui().show(focus);
        return true;
    }
}
