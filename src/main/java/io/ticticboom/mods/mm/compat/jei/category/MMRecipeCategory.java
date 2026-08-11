package io.ticticboom.mods.mm.compat.jei.category;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.WidgetUtils;
import lombok.Getter;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class MMRecipeCategory implements IRecipeCategory<RecipeModel> {

    public static final RecipeType<RecipeModel> RECIPE_TYPE = RecipeType.create(Ref.ID, "recipes", RecipeModel.class);
    private final IJeiHelpers helpers;
    private final IDrawable bgProgressBar;
    @Getter
    private final StructureModel structureModel;
    private final IDrawable fgProgressBar;
    private final RecipeType<RecipeModel> recipeType;
    private final int height;

    @Override
    public ResourceLocation getRegistryName(RecipeModel recipe) {
        return recipe.id();
    }

    public MMRecipeCategory(IJeiHelpers helpers, StructureModel parent, int height) {
        this.helpers = helpers;
        bgProgressBar = helpers.getGuiHelper().createDrawable(Ref.UiTextures.SLOT_PARTS, 26, 0, 24, 17);
        this.structureModel = parent;
        var staticProgressBar = helpers.getGuiHelper().createDrawable(Ref.UiTextures.SLOT_PARTS, 26, 17, 24, 17);
        fgProgressBar = helpers.getGuiHelper().createAnimatedDrawable(staticProgressBar, 16, IDrawableAnimated.StartDirection.LEFT, false);
        if (structureModel != null) {
            recipeType = RecipeType.create("mm", parent.id().getPath() + "_recipe", RecipeModel.class);
        } else {
            recipeType = RECIPE_TYPE;
        }
        this.height = height;
    }

    @Override
    public @NotNull RecipeType<RecipeModel> getRecipeType() {
        return recipeType;
    }

    @Override
    public @NotNull Component getTitle() {
        if (structureModel != null) {
            return Component.literal(this.structureModel.name()).append(Component.literal(" (Recipes)"));
        } else {
            return Component.literal("MM Recipes");
        }
    }

    @SuppressWarnings("removal")
    @Override
    public IDrawable getBackground() {
        return helpers.getGuiHelper().createBlankDrawable(162, height);
    }

    @Override
    public IDrawable getIcon() {
        return helpers.getGuiHelper().createDrawableItemStack(MMRegisters.BLUEPRINT.get().getDefaultInstance());
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, RecipeModel recipe, @NotNull IFocusGroup focuses) {
        int inputRows = (int) Math.ceil(recipe.inputs().inputs().size() / 3.0);
        int outputRows = (int) Math.ceil(recipe.outputs().outputs().size() / 3.0);
        var inGrid = new SlotGrid(20, 20, 3, inputRows, 0, 0);
        var outGrid = new SlotGrid(20, 20, 3, outputRows, 100, 0);
        for (IRecipeIngredientEntry input : recipe.inputs().inputs()) {
            input.setRecipe(builder, recipe, focuses, helpers, inGrid);
        }
        for (IRecipeOutputEntry output : recipe.outputs().outputs()) {
            output.setRecipe(builder, recipe, focuses, helpers, outGrid);
        }

        recipe.inputSlots().addAll(inGrid.getSlots());
        recipe.inputSlots().addAll(outGrid.getSlots());
    }

    @Override
    public void draw(RecipeModel recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics gfx, double mouseX, double mouseY) {
        bgProgressBar.draw(gfx, 70, 12);
        fgProgressBar.draw(gfx, 70, 12);
        var seconds = (double) recipe.ticks() / 20;
        var fmt = String.format("%.2f", seconds) + "s";

        if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, 70, 12, 24, 17)) {
            gfx.renderTooltip(Minecraft.getInstance().font, Component.literal(fmt), (int) mouseX, (int) mouseY);
        }

        if (structureModel == null) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, 75, 28, 19, 26, 7, 9);
            if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, 75, 28, 7, 9)) {
                gfx.renderTooltip(Minecraft.getInstance().font, Component.literal("Structure: " + recipe.structureId().toString()), (int) mouseX, (int) mouseY);
            }
        }

        for (SlotGridEntry inputSlot : recipe.inputSlots()) {
            if (inputSlot.used()) {
                gfx.blit(Ref.UiTextures.SLOT_PARTS, inputSlot.x, inputSlot.y, 0, 26, 18, 18);
                // draw small 'x' badge bottom-right if slot is marked not used
                if (inputSlot.hasBadgeNotUsed()) {
                    String badge = "x";
                    int badgeX = inputSlot.x + 12;
                    int badgeY = inputSlot.y + 12;
                    gfx.drawString(Minecraft.getInstance().font, badge, badgeX, badgeY, 0xFF5555, true);
                }
                // draw AE2-style count badge (K/M) in the slot
                int count = inputSlot.getBadgeCount();
                if (count > 1) {
                    drawSlotCount(gfx, inputSlot.x, inputSlot.y, count);
                }
            }
        }
    }

    /** Renders an AE2-style count label at the bottom-right of the given slot. */
    private void drawSlotCount(GuiGraphics gfx, int slotX, int slotY, int count) {
        String text = formatCount(count);
        var font = Minecraft.getInstance().font;
        // Use half-size font when the string is longer than 3 chars to fit inside the slot
        float scale = text.length() > 3 ? 0.5f : 1.0f;
        var pose = gfx.pose();
        pose.pushPose();
        // Anchor to the bottom-right corner of the slot inner area; z=200 draws above the item texture
        pose.translate(slotX + 17, slotY + 17, 200);
        pose.scale(scale, scale, 1.0f);
        gfx.drawString(font, text, -font.width(text), -font.lineHeight, 0xFFFFFF, true);
        pose.popPose();
    }

    /** Formats a count into a compact AE2-style string (1K / 1M / 1B). */
    public static String formatCount(int count) {
        if (count >= 1_000_000_000) {
            long tenthsOfB = Math.round(count / 100_000_000.0);
            long whole = tenthsOfB / 10;
            long tenths = tenthsOfB % 10;
            return tenths == 0 ? whole + "B" : whole + "." + tenths + "B";
        } else if (count >= 1_000_000) {
            long tenthsOfM = Math.round(count / 100_000.0);
            // Rounding can push 999.95M → 1B
            if (tenthsOfM >= 10_000) {
                return (tenthsOfM / 10_000) + "B";
            }
            long whole = tenthsOfM / 10;
            long tenths = tenthsOfM % 10;
            return tenths == 0 ? whole + "M" : whole + "." + tenths + "M";
        } else if (count >= 1_000) {
            long tenthsOfK = Math.round(count / 100.0);
            // Rounding can push 999.95K → 1M
            if (tenthsOfK >= 10_000) {
                return (tenthsOfK / 10_000) + "M";
            }
            long whole = tenthsOfK / 10;
            long tenths = tenthsOfK % 10;
            return tenths == 0 ? whole + "K" : whole + "." + tenths + "K";
        }
        return String.valueOf(count);
    }
}