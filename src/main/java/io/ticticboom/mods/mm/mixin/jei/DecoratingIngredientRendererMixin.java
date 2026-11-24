package io.ticticboom.mods.mm.mixin.jei;

import io.ticticboom.mods.mm.compat.jei.NoCountItemRenderer;
import mezz.jei.api.ingredients.IIngredientRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * JEI 15.75.0 wraps every custom slot renderer in {@code DecoratingIngredientRenderer}, which runs the
 * registered ingredient decorators after it. The vanilla plugin registers {@code ItemStackDecorator}
 * ({@code renderItemDecorations}: the stack count), so the full vanilla count was drawn on top of our
 * compact K/M badge. For slots that use {@link NoCountItemRenderer} we drop that one decorator again;
 * decorators added by other mods still run.
 * <p>
 * {@link Pseudo}: the class only exists from JEI 15.75.0 on; with older JEI this mixin is skipped.
 */
@Pseudo
@Mixin(targets = "mezz.jei.common.ingredients.DecoratingIngredientRenderer", remap = false)
public abstract class DecoratingIngredientRendererMixin {
    private static final String VANILLA_ITEM_DECORATOR = "mezz.jei.library.render.ItemStackDecorator";

    @Shadow
    @Final
    private IIngredientRenderer<?> delegate;

    @Inject(method = "getDecorators", at = @At("RETURN"), cancellable = true, require = 0)
    private void mm$skipVanillaCount(CallbackInfoReturnable<List<?>> cir) {
        if (!(delegate instanceof NoCountItemRenderer)) {
            return;
        }
        List<?> decorators = cir.getReturnValue();
        if (decorators.isEmpty()) {
            return;
        }
        cir.setReturnValue(decorators.stream()
                .filter(d -> !d.getClass().getName().equals(VANILLA_ITEM_DECORATOR))
                .toList());
    }
}
