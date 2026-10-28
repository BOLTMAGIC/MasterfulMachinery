package io.ticticboom.mods.mm.compat.jei.ingredient.pncr;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.Ref;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PneumaticAirIngredientRender  implements IIngredientRenderer<PneumaticAirStack> {

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, @NotNull PneumaticAirStack pneumaticAirStack) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, 0, 0, 1, 62, 16, 16, 256, 256);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(PneumaticAirStack pneumaticAirStack, @NotNull TooltipFlag tooltipFlag) {
        var result = new ArrayList<Component>();
        result.add(Component.translatable("jei.mm.ingredient.pneumaticcraft_air.title"));
        result.add(Component.translatable("jei.mm.ingredient.pneumaticcraft_air.amount", pneumaticAirStack.air()));
        result.add(Component.translatable("jei.mm.ingredient.pneumaticcraft_air.pressure", pneumaticAirStack.pressure()));
        return result;
    }
}
