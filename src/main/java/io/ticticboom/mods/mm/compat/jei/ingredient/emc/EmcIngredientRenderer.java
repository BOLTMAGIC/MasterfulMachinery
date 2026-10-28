package io.ticticboom.mods.mm.compat.jei.ingredient.emc;

import io.ticticboom.mods.mm.util.NumberText;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EmcIngredientRenderer implements IIngredientRenderer<EmcStack> {

    private static final Identifier ICON = Identifier.fromNamespaceAndPath("projecte", "klein_star_ein");

    private ItemStack icon;

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, @NotNull EmcStack stack) {
        if (icon == null) {
            icon = BuiltInRegistries.ITEM.getValue(ICON).getDefaultInstance();
        }
        guiGraphics.fakeItem(icon, 0, 0);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(EmcStack stack, @NotNull TooltipFlag tooltipFlag) {
        var amount = stack.min() == stack.max()
                ? Component.translatable("jei.mm.ingredient.projecte_emc.amount", NumberText.grouped(stack.max()))
                : Component.translatable("jei.mm.ingredient.projecte_emc.range", NumberText.grouped(stack.min()), NumberText.grouped(stack.max()));
        return List.of(Component.translatable("jei.mm.ingredient.projecte_emc.title"), amount);
    }
}
