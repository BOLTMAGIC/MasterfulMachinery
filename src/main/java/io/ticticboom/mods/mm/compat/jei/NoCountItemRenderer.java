package io.ticticboom.mods.mm.compat.jei;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * JEI item renderer that draws the item sprite WITHOUT the vanilla count label.
 * The count is instead rendered by {@link io.ticticboom.mods.mm.compat.jei.category.MMRecipeCategory#draw}
 * using AE2-style K/M suffixes so that the ItemStack retains its real count for
 * AE2 pattern encoding and other integrations.
 */
public class NoCountItemRenderer implements IIngredientRenderer<ItemStack> {

    private final IIngredientRenderer<ItemStack> delegate;

    public NoCountItemRenderer(IIngredientRenderer<ItemStack> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void render(GuiGraphics guiGraphics, ItemStack ingredient) {
        // Render the item model only; skip renderItemDecorations so the vanilla
        // count string is never drawn (we draw our own in MMRecipeCategory.draw).
        guiGraphics.renderItem(ingredient, 0, 0);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ItemStack ingredient, TooltipFlag tooltipFlag) {
        delegate.getTooltip(tooltip, ingredient, tooltipFlag);
    }

    @SuppressWarnings({"deprecation", "removal"})
    @Override
    public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
        return delegate.getTooltip(ingredient, tooltipFlag);
    }

    @Override
    public Font getFontRenderer(Minecraft minecraft, ItemStack ingredient) {
        return delegate.getFontRenderer(minecraft, ingredient);
    }
}
