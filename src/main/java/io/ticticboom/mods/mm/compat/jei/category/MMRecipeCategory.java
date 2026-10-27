package io.ticticboom.mods.mm.compat.jei.category;

import java.util.ArrayList;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.IRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
        if (recipe.config() != null && recipe.config().has(FlexRecipeIngredientEntry.JEI_PAGE)) {
            // JEI page copies of a flex recipe need their own names
            return ResourceLocation.fromNamespaceAndPath(recipe.id().getNamespace(),
                    recipe.id().getPath() + "/jei_page_" + recipe.config().get(FlexRecipeIngredientEntry.JEI_PAGE).getAsInt());
        }
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
            return Component.translatable("jei.mm.category.structure_recipes", this.structureModel.displayName());
        } else {
            return Component.translatable("jei.mm.category.recipes");
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
        int inputRows = rowsFor(inputSlotCount(recipe));
        int outputRows = rowsFor(outputSlotCount(recipe));
        var inGrid = new SlotGrid(20, 20, COLUMNS, inputRows, 0, 0);
        var outGrid = new SlotGrid(20, 20, COLUMNS, outputRows, 100, 0);
        for (IRecipeIngredientEntry input : recipe.inputs().inputs()) {
            if (input instanceof FlexRecipeIngredientEntry flex) {
                // before any entry adds its slots: flex_match entries may come before their flex input
                flex.clearJeiGroup();
            }
        }
        for (IRecipeIngredientEntry input : recipe.inputs().inputs()) {
            input.setRecipe(builder, recipe, focuses, helpers, inGrid);
        }
        for (IRecipeOutputEntry output : recipe.outputs().outputs()) {
            output.setRecipe(builder, recipe, focuses, helpers, outGrid);
        }
        // flex recipes: the input (and its max_types copies), its matching items and its outputs list one item per
        // kind in the same order
        for (IRecipeIngredientEntry input : recipe.inputs().inputs()) {
            if (input instanceof FlexRecipeIngredientEntry flex && flex.getJeiGroup().size() > 1) {
                builder.createFocusLink(flex.getJeiGroup().toArray(new IRecipeSlotBuilder[0]));
            }
        }

        recipe.inputSlots().clear();
        recipe.inputSlots().addAll(inGrid.getSlots());
        recipe.inputSlots().addAll(outGrid.getSlots());
    }

    /** "Tier 5" (and "1/2") above the arrow of a flex recipe's JEI page. */
    private static void drawFlexPageLabel(RecipeModel recipe, GuiGraphics gfx) {
        if (recipe.config() == null || !recipe.config().has(FlexRecipeIngredientEntry.JEI_PAGE)) {
            return;
        }
        int tier = recipe.config().get(FlexRecipeIngredientEntry.JEI_PAGE_TIER).getAsInt();
        String part = recipe.config().get(FlexRecipeIngredientEntry.JEI_PAGE_PART).getAsString();
        Component label = tier >= 0
                ? Component.translatable("jei.mm.recipe.flex_page_tier", tier)
                : Component.translatable("jei.mm.recipe.flex_page", recipe.config().get(FlexRecipeIngredientEntry.JEI_PAGE).getAsInt() + 1);
        if (!part.isEmpty()) {
            label = label.copy().append(" " + part);
        }
        var font = Minecraft.getInstance().font;
        var pose = gfx.pose();
        pose.pushPose();
        pose.translate(82, 2, 0);
        pose.scale(0.6f, 0.6f, 1);
        gfx.drawString(font, label, -font.width(label) / 2, 0, 0x404040, false);
        pose.popPose();
    }

    public static int rowsFor(int slotCount) {
        return (slotCount + COLUMNS - 1) / COLUMNS;
    }

    /** JEI input slots of the recipe without a focus (a flex input with "max_types" shows one per kind). */
    public static int inputSlotCount(RecipeModel recipe) {
        return recipe.inputs().inputs().stream().mapToInt(i -> i.jeiSlotCount(recipe)).sum();
    }

    /** JEI output slots of the recipe without a focus. */
    public static int outputSlotCount(RecipeModel recipe) {
        return recipe.outputs().outputs().stream().mapToInt(o -> o.jeiSlotCount(recipe)).sum();
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
        drawFlexPageLabel(recipe, gfx);
        var seconds = (double) recipe.ticks() / 20;
        var fmt = String.format("%.2f", seconds);

        if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, 70, 12, 24, 17)) {
            gfx.renderTooltip(Minecraft.getInstance().font, Component.translatable("jei.mm.recipe.duration", fmt), (int) mouseX, (int) mouseY);
        }

        if (structureModel == null) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, 75, 28, 19, 26, 7, 9);
            if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, 75, 28, 7, 9)) {
                gfx.renderTooltip(Minecraft.getInstance().font, Component.translatable("jei.mm.recipe.structure", recipe.structureId().toString()), (int) mouseX, (int) mouseY);
            }
        }

        if (!recipe.conditions().isEmpty()) {
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

        // the slots setRecipe laid out (createRecipeExtras puts more than COLUMNS * VISIBLE_ROWS in a scroll grid, which
        // draws its own slot backgrounds; slots inside it aren't in recipeSlotsView, so count the grid entries)
        int inputs = 0;
        int outputs = 0;
        for (SlotGridEntry slot : recipe.inputSlots()) {
            if (slot.used()) {
                if (slot.x < 100) inputs++;
                else outputs++;
            }
        }
        boolean scrollInputs = inputs > COLUMNS * VISIBLE_ROWS;
        boolean scrollOutputs = outputs > COLUMNS * VISIBLE_ROWS;
        for (SlotGridEntry inputSlot : recipe.inputSlots()) {
            boolean scrollSide = inputSlot.x < 100 ? scrollInputs : scrollOutputs;
            if (inputSlot.used() && !scrollSide) {
                gfx.blit(Ref.UiTextures.SLOT_PARTS, inputSlot.x, inputSlot.y, 0, 26, 18, 18);
            }
        }
    }
}
