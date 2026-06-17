package io.ticticboom.mods.mm.compat.jei.ingredient.create;

import io.ticticboom.mods.mm.client.texture.GuiTextures;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreateRotationIngredientRenderer implements IIngredientRenderer<CreateRotationStack> {

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull CreateRotationStack createRotationStack) {
        GuiTextures.CREATE_PORT_SLOT.blit(guiGraphics, 0, 0);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(CreateRotationStack ingredient, @NotNull TooltipFlag tooltipFlag) {
        return List.of(Component.literal("Create Rotation: " + ingredient.speed()));
    }
}
