package io.ticticboom.mods.mm.compat.jei.ingredient.source;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.Ref;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArsSourceIngredientRenderer implements IIngredientRenderer<ArsSourceStack> {

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, @NotNull ArsSourceStack stack) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, 0, 0, 0, 160, 16, 16, 256, 256);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(ArsSourceStack stack, @NotNull TooltipFlag tooltipFlag) {
        var amount = stack.min() == stack.max()
                ? Component.translatable("jei.mm.ingredient.ars_source.amount", stack.max())
                : Component.translatable("jei.mm.ingredient.ars_source.range", stack.min(), stack.max());
        return List.of(Component.translatable("jei.mm.ingredient.ars_source.title"), amount);
    }
}
