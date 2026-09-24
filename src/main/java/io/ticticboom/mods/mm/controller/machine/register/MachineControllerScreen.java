package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.FluidRenderer;
import io.ticticboom.mods.mm.client.texture.GuiTextures;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import io.ticticboom.mods.mm.setup.loader.ControllerLoader;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller status: structure, the running recipe (inputs, MM's progress arrow, outputs, percentage),
 * parallel count, redstone mode (click to cycle) and recipe selection mode.
 */
public class MachineControllerScreen extends AbstractContainerScreen<MachineControllerMenu> {
    private static final int TEXT = 0xacacac;
    private static final int MAX_INPUTS = 3;
    private static final int MAX_OUTPUTS = 2;
    private static final int SLOT_STEP = 19;
    private static final int RECIPE_X = 12;
    private static final int RECIPE_Y = 40;
    private static final int PARALLEL_Y = 66;
    private static final int REDSTONE_Y = 82;
    private static final int MODE_Y = 100;
    private static final int ROW_X = 10;

    private final MachineControllerMenu menu;
    private final MachineControllerBlockEntity be;

    public MachineControllerScreen(MachineControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.menu = menu;
        this.be = (MachineControllerBlockEntity) menu.getBe();
        this.imageHeight = 222;
        this.imageWidth = 174;
    }

    private record Shown(int x, int y, IPortIngredient ingredient, boolean input) {
    }

    /**
     * @return the running recipe's inputs and outputs laid out on the recipe row, GUI-relative
     */
    private List<Shown> recipeSlots() {
        RecipeModel recipe = be.getCurrentRecipe();
        var shown = new ArrayList<Shown>();
        if (recipe == null) {
            return shown;
        }
        var inputs = new ArrayList<IPortIngredient>();
        for (var entry : recipe.inputs().inputs()) {
            if (entry instanceof ConsumeRecipeIngredientEntry consume && isDisplayable(consume.getIngredient()) && inputs.size() < MAX_INPUTS) {
                inputs.add(consume.getIngredient());
            }
        }
        var outputs = new ArrayList<IPortIngredient>();
        for (var entry : recipe.outputs().outputs()) {
            if (entry instanceof SimpleRecipeOutputEntry simple && isDisplayable(simple.getIngredient()) && outputs.size() < MAX_OUTPUTS) {
                outputs.add(simple.getIngredient());
            }
        }
        for (int i = 0; i < inputs.size(); i++) {
            shown.add(new Shown(RECIPE_X + i * SLOT_STEP, RECIPE_Y, inputs.get(i), true));
        }
        int outputX = arrowX(inputs.size()) + 28;
        for (int i = 0; i < outputs.size(); i++) {
            shown.add(new Shown(outputX + i * SLOT_STEP, RECIPE_Y, outputs.get(i), false));
        }
        return shown;
    }

    private static boolean isDisplayable(IPortIngredient ingredient) {
        return !ingredient.displayItem().isEmpty() || !ingredient.displayFluid().isEmpty();
    }

    private static int arrowX(int inputCount) {
        return RECIPE_X + inputCount * SLOT_STEP + 3;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.GUI_LARGE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        var slots = recipeSlots();
        if (!slots.isEmpty()) {
            int inputs = (int) slots.stream().filter(Shown::input).count();
            for (Shown s : slots) {
                drawIngredient(gfx, this.leftPos + s.x(), this.topPos + s.y(), s.ingredient());
            }
            // MM's own progress arrow, as in the JEI categories
            int ax = this.leftPos + arrowX(inputs);
            int ay = this.topPos + RECIPE_Y;
            gfx.blit(Ref.UiTextures.SLOT_PARTS, ax, ay, 26, 0, 24, 17);
            var state = be.getRecipeState();
            int filled = state == null ? 0 : (int) Math.round(24 * Math.min(100, state.getTickPercentage()) / 100);
            gfx.blit(Ref.UiTextures.SLOT_PARTS, ax, ay, 26, 17, filled, 17);
        }

        int bx = this.leftPos + ROW_X;
        int by = this.topPos + REDSTONE_Y;
        GuiTextures.BUTTON_ACTIVE.blit(gfx, bx, by, 12, 12);
        var pose = gfx.pose();
        pose.pushPose();
        pose.translate(bx + 1, by + 1, 0);
        pose.scale(10f / 16f, 10f / 16f, 1);
        gfx.renderItem(new ItemStack(Items.REDSTONE), 0, 0);
        pose.popPose();
    }

    private void drawIngredient(GuiGraphics gfx, int x, int y, IPortIngredient ingredient) {
        gfx.blit(Ref.UiTextures.SLOT_PARTS, x, y, 0, 26, 18, 18);
        var item = ingredient.displayItem();
        if (!item.isEmpty()) {
            gfx.renderItem(item, x + 1, y + 1);
            if (item.getCount() > 1) {
                CountFormat.drawSlotCount(gfx, x, y, item.getCount());
            }
            return;
        }
        FluidRenderer.INSTANCE.render(gfx, x + 1, y + 1, ingredient.displayFluid(), 16);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, menu.getModel().name(), ROW_X, 10, TEXT, false);

        var structure = be.getStructure();
        Component formed = structure != null
                ? Component.translatable("gui.mm.controller.formed_as", structure.name())
                : Component.translatable("gui.mm.controller.not_formed");
        gfx.drawString(this.font, formed, ROW_X, 24, TEXT, false);

        var slots = recipeSlots();
        if (slots.isEmpty()) {
            gfx.drawString(this.font, Component.translatable("gui.mm.controller.idle"), ROW_X, RECIPE_Y + 5, TEXT, false);
        } else {
            var state = be.getRecipeState();
            int percent = state == null ? 0 : (int) Math.floor(state.getTickPercentage());
            int lastX = slots.get(slots.size() - 1).x();
            gfx.drawString(this.font, percent + "%", lastX + 21, RECIPE_Y + 5, TEXT, false);
        }

        if (structure != null) {
            gfx.drawString(this.font, Component.translatable("gui.mm.controller.parallel",
                    be.getActiveRecipeCount(), maxParallel()), ROW_X, PARALLEL_Y, TEXT, false);
        }

        String redstone = be.getRedstoneModeName().toLowerCase();
        gfx.drawString(this.font, Component.translatable("gui.mm.controller.redstone",
                Component.translatable("gui.mm.controller.redstone." + redstone)), ROW_X + 16, REDSTONE_Y + 2, TEXT, false);

        String mode = be.getRecipeSelectionMode().serializedName();
        gfx.drawString(this.font, Component.translatable("gui.mm.controller.mode",
                Component.translatable("gui.mm.controller.mode." + mode)), ROW_X, MODE_Y, TEXT, false);
    }

    private int maxParallel() {
        var structure = be.getStructure();
        if (structure != null && structure.maxParallelRecipes() > 0) {
            return structure.maxParallelRecipes();
        }
        var controllerModel = ControllerLoader.CONTROLLER_MODELS.get(menu.getModel().id());
        if (controllerModel != null && controllerModel.maxParallelRecipes() > 0) {
            return controllerModel.maxParallelRecipes();
        }
        return 1;
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partial);
        renderTooltip(gfx, mouseX, mouseY);

        for (Shown s : recipeSlots()) {
            if (!WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + s.x(), this.topPos + s.y(), 18, 18)) {
                continue;
            }
            var item = s.ingredient().displayItem();
            if (!item.isEmpty()) {
                gfx.renderTooltip(this.font, item, mouseX, mouseY);
            } else {
                var fluid = s.ingredient().displayFluid();
                gfx.renderComponentTooltip(this.font, List.of(fluid.getDisplayName(),
                        Component.literal(CountFormat.grouped(fluid.getAmount()) + " mB").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            }
        }
        if (isOnRedstoneRow(mouseX, mouseY)) {
            gfx.renderComponentTooltip(this.font, List.of(Component.translatable("gui.mm.controller.redstone.hint")), mouseX, mouseY);
        }
    }

    private boolean isOnRedstoneRow(double mouseX, double mouseY) {
        double mx = mouseX - this.leftPos;
        double my = mouseY - this.topPos;
        return mx >= ROW_X && mx <= ROW_X + 150 && my >= REDSTONE_Y && my <= REDSTONE_Y + 12;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOnRedstoneRow(mouseX, mouseY)) {
            int next = (be.getRedstoneModeOrdinal() + 1) % 3;
            MMNetwork.INSTANCE.sendToServer(new ToggleRedstoneModePkt(be.getBlockPos(), next));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
