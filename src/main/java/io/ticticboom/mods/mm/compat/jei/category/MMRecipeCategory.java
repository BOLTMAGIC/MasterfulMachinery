package io.ticticboom.mods.mm.compat.jei.category;

import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
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
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class MMRecipeCategory implements IRecipeCategory<RecipeModel> {

    public static final RecipeType<RecipeModel> RECIPE_TYPE = RecipeType.create(Ref.ID, "recipes", RecipeModel.class);
    public static final int VISIBLE_ROWS = 6;
    private static final int COLUMNS = 3;
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
            return Component.translatable("jei.mm.recipes.title_for", this.structureModel.name());
        } else {
            return Component.translatable("jei.mm.recipes.title");
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
        int inputRows = rowsFor(recipe.inputs().inputs().size());
        int outputCount = recipe.outputs().outputs().stream().mapToInt(o -> o.displayedOutputs().size()).sum();
        int outputRows = rowsFor(outputCount);
        var inGrid = new SlotGrid(20, 20, COLUMNS, inputRows, 0, 0);
        var outGrid = new SlotGrid(20, 20, COLUMNS, outputRows, 100, 0);
        for (IRecipeIngredientEntry input : recipe.inputs().inputs()) {
            input.setRecipe(builder, recipe, focuses, helpers, inGrid);
        }
        for (IRecipeOutputEntry output : recipe.outputs().outputs()) {
            output.setRecipe(builder, recipe, focuses, helpers, outGrid);
        }

        recipe.inputSlots().clear();
        recipe.inputSlots().addAll(inGrid.getSlots());
        recipe.inputSlots().addAll(outGrid.getSlots());
    }

    public static int rowsFor(int slotCount) {
        return (slotCount + COLUMNS - 1) / COLUMNS;
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeModel recipe, IFocusGroup focuses) {
        var inputSlots = builder.getRecipeSlots().getSlots(RecipeIngredientRole.INPUT);
        var outputSlots = builder.getRecipeSlots().getSlots(RecipeIngredientRole.OUTPUT);
        if (inputSlots.size() > COLUMNS * VISIBLE_ROWS) {
            builder.addScrollGridWidget(inputSlots, COLUMNS, VISIBLE_ROWS).setPosition(0, 0);
        }
        if (outputSlots.size() > COLUMNS * VISIBLE_ROWS) {
            builder.addScrollGridWidget(outputSlots, COLUMNS, VISIBLE_ROWS).setPosition(92, 0);
        }
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
                gfx.renderTooltip(Minecraft.getInstance().font, Component.translatable("jei.mm.recipes.structure", recipe.structureId().toString()), (int) mouseX, (int) mouseY);
            }
        }

        if (!recipe.conditions().isEmpty()) {
            // a clock under the arrow; its tooltip lists what the recipe needs besides its inputs
            int cx = structureModel == null ? 86 : 77;
            var pose = gfx.pose();
            pose.pushPose();
            pose.translate(cx, 29, 0);
            pose.scale(10f / 16f, 10f / 16f, 1);
            gfx.renderItem(new ItemStack(Items.CLOCK), 0, 0);
            pose.popPose();
            if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, cx, 29, 10, 10)) {
                var lines = new ArrayList<Component>();
                lines.add(Component.translatable("jei.mm.condition.title").withStyle(ChatFormatting.GOLD));
                lines.addAll(recipe.conditions().describe());
                gfx.renderComponentTooltip(Minecraft.getInstance().font, lines, (int) mouseX, (int) mouseY);
            }
        }

        boolean scrollInputs = recipe.inputs().inputs().size() > COLUMNS * VISIBLE_ROWS;
        boolean scrollOutputs = recipe.outputs().outputs().stream()
                .mapToInt(o -> o.displayedOutputs().size()).sum() > COLUMNS * VISIBLE_ROWS;
        for (SlotGridEntry inputSlot : recipe.inputSlots()) {
            boolean scrollSide = inputSlot.x < 100 ? scrollInputs : scrollOutputs;
            if (inputSlot.used() && !scrollSide) {
                gfx.blit(Ref.UiTextures.SLOT_PARTS, inputSlot.x, inputSlot.y, 0, 26, 18, 18);
            }
        }
    }
}
