package io.ticticboom.mods.mm.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.structure.GuiCountedItemStack;
import io.ticticboom.mods.mm.client.structure.GuiStructureRenderer;
import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import io.ticticboom.mods.mm.compat.jei.SlotGrid;
import io.ticticboom.mods.mm.compat.jei.SlotGridEntry;
import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.setup.loader.ControllerLoader;
import net.minecraft.client.Minecraft;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.joml.Vector4f;

public class MMStructureCategory implements IRecipeCategory<StructureModel> {

    public static final RecipeType<StructureModel> RECIPE_TYPE = RecipeType.create("mm", "structure", StructureModel.class);

    private final IGuiHelper helper;

    private static final Vector2i PANEL_SIZE = new Vector2i(162, 170);
    private static final Vector2i RENDER_SIZE = new Vector2i(160, 120);
    private final IDrawableStatic background;
    private final MutableComponent title = Component.literal("MM Structure");

    public MMStructureCategory(final IGuiHelper helper) {
        this.helper = helper;
        background = helper.createDrawable(Ref.UiTextures.GUI_LARGE_JEI, 0, 0, 162, 121);
    }

    @Override
    public @NotNull RecipeType<StructureModel> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public ResourceLocation getRegistryName(StructureModel recipe) {
        return recipe.id();
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return PANEL_SIZE.x;
    }

    @Override
    public int getHeight() {
        return PANEL_SIZE.y;
    }

    @Override
    public IDrawable getIcon() {
        return helper.createDrawableItemStack(MMRegisters.BLUEPRINT.get().getDefaultInstance());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StructureModel recipe, @NotNull IFocusGroup focuses) {
        var catalysts = builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST);
        for (ResourceLocation id : recipe.controllerIds().getIds()) {
            Item controller = MMControllerRegistry.getControllerItem(id);
            if (controller != null) {
                catalysts.addItemStack(controller.getDefaultInstance());
            }
        }
        GuiStructureRenderer guiRenderer = recipe.getGuiRenderer();
        guiRenderer.resetTransforms();
        guiRenderer.init();
        var countedItemStacks = recipe.getCountedItemStacks();
        var grid = new SlotGrid(20, 20, 8, 3, 1, 130);
        recipe.setGrid(grid);
        for (GuiCountedItemStack countedItemStack : countedItemStacks) {
            SlotGridEntry next = grid.next();
            next.setUsed();
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, next.x, next.y);
            var stacks = countedItemStack.getStacks().stream().map(s -> {
                var copy = s.copy();
                copy.setCount(countedItemStack.getCount());
                return copy;
            }).toList();
            slot.addItemStacks(stacks);
            slot.addRichTooltipCallback((a, b) -> b.add(countedItemStack.getDetail()));
        }

        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
                .addItemStack(MMRegisters.BLUEPRINT.get().getStructureInstance(recipe.id()));
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .addItemStack(MMRegisters.BLUEPRINT.get().getStructureInstance(recipe.id()));
    }

    @Override
    public void draw(@NotNull StructureModel recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics, 0, 0);

        renderStructure(recipe, guiGraphics, mouseX, mouseY);

        var slotDrawable = helper.getSlotDrawable();
        for (SlotGridEntry slot : recipe.getGrid().getSlots()) {
            slotDrawable.draw(guiGraphics, slot.x - 1, slot.y - 1);
        }

        // Render Max Parallel Processing as first entry in JEI (configurable)
        if (io.ticticboom.mods.mm.config.MMConfigSetup.COMMON.showJeiMaxParallel.get()) {
        int structVal = recipe.maxParallelRecipes();
        int displayInt = 1; // default when unspecified or zero/negative
        if (structVal > 0) {
            displayInt = structVal;
        } else {
            var ids = recipe.controllerIds().getIds();
            if (!ids.isEmpty()) {
                var controllerModel = ControllerLoader.CONTROLLER_MODELS.get(ids.get(0).getPath());
                if (controllerModel != null && controllerModel.maxParallelRecipes() > 0) {
                    displayInt = controllerModel.maxParallelRecipes();
                }
            }
        }
        String line = "Max Parallel Processing: " + displayInt;
            // draw structure name first, then the Max Parallel line smaller beneath it
            int nameY = 5;
            TextRenderUtil.renderWordWrapLimit(guiGraphics, recipe.name(), 5, nameY, RENDER_SIZE.x - 5, 2, 0xFFFFFFFF);
            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int subY = nameY + lineHeight;
            float scale = 0.65f; // smaller text for the max-parallel line
            guiGraphics.pose().pushPose();
            // translate to desired position then scale down
            guiGraphics.pose().translate(5f, (float) subY, 0f);
            guiGraphics.pose().scale(scale, scale, 1f);
            int wrapWidth = (int) ((RENDER_SIZE.x - 5) / scale);
            TextRenderUtil.renderWordWrapLimit(guiGraphics, line, 0, 0, wrapWidth, 1, 0xFFFFFFFF);
            guiGraphics.pose().popPose();
        } else {
            TextRenderUtil.renderWordWrapLimit(guiGraphics, recipe.name(), 5, 5, RENDER_SIZE.x - 5, 2, 0xFFFFFFFF);
        }
    }

    private void renderStructure(StructureModel recipe, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var pos = getGuiPosition(guiGraphics.pose());

        guiGraphics.pose().pushPose();
        guiGraphics.pose().setIdentity();

        var renderer = recipe.getGuiRenderer();
        renderer.setViewport(GuiPos.of(pos.x + 1, pos.y + 1, RENDER_SIZE.x, RENDER_SIZE.y));
        renderer.render(guiGraphics, (int) mouseX, (int) mouseY);

        guiGraphics.pose().popPose();
    }

    private Vector2i getGuiPosition(PoseStack poseStack) {
        var transformedPosition = new Vector4f(0, 0, 0, 1);
        transformedPosition.mul(poseStack.last().pose());
        return new Vector2i((int) transformedPosition.x, (int) transformedPosition.y);
    }
}
