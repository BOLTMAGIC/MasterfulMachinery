package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.FluidRenderer;
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
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller status, top to bottom inside MM's dark screen: machine name, a coloured status line
 * (not formed / paused by redstone / running / idle), the running recipe (inputs, MM's progress arrow,
 * outputs, percentage), then aligned label / value rows for structure, parallel count, redstone mode
 * (click to cycle) and recipe order. Every row explains itself in a tooltip.
 */
public class MachineControllerScreen extends AbstractContainerScreen<MachineControllerMenu> {
    private static final int TEXT = 0xDDDDDD;
    private static final int LABEL = 0x8A8A8A;
    private static final int DIVIDER = 0xFF3A3A3A;
    private static final int LEFT = 10;
    private static final int RIGHT = 163;
    private static final int VALUE_X = 74;

    private static final int NAME_Y = 10;
    private static final int STATUS_Y = 22;
    private static final int RECIPE_LABEL_Y = 37;
    private static final int RECIPE_Y = 48;
    private static final int ROWS_Y = 75;
    private static final int ROW_STEP = 13;

    private static final int MAX_INPUTS = 3;
    private static final int MAX_OUTPUTS = 2;
    private static final int SLOT_STEP = 19;

    private enum Row { STRUCTURE, PARALLEL, REDSTONE, MODE }

    private enum Status {
        NOT_FORMED(0xFF5555), PAUSED(0xFFAA00), RUNNING(0x55FF55), IDLE(0xE0C050);

        final int color;

        Status(int color) {
            this.color = color;
        }

        String key() {
            return "gui.mm.controller.status." + name().toLowerCase();
        }
    }

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

    private Status status() {
        if (be.getStructure() == null) return Status.NOT_FORMED;
        if (!be.isAllowedByRedstone()) return Status.PAUSED;
        if (be.getCurrentRecipe() != null) return Status.RUNNING;
        return Status.IDLE;
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
            shown.add(new Shown(LEFT + i * SLOT_STEP, RECIPE_Y, inputs.get(i), true));
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
        return LEFT + inputCount * SLOT_STEP + 3;
    }

    private static int rowY(Row row) {
        return ROWS_Y + row.ordinal() * ROW_STEP;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.blit(Ref.UiTextures.GUI_LARGE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int x = this.leftPos;
        int y = this.topPos;

        // status light
        gfx.fill(x + LEFT, y + STATUS_Y + 1, x + LEFT + 5, y + STATUS_Y + 6, 0xFF000000 | status().color);

        gfx.fill(x + LEFT, y + STATUS_Y + 11, x + RIGHT, y + STATUS_Y + 12, DIVIDER);
        gfx.fill(x + LEFT, y + ROWS_Y - 5, x + RIGHT, y + ROWS_Y - 4, DIVIDER);

        var slots = recipeSlots();
        if (!slots.isEmpty()) {
            int inputs = (int) slots.stream().filter(Shown::input).count();
            for (Shown s : slots) {
                drawIngredient(gfx, x + s.x(), y + s.y(), s.ingredient());
            }
            // MM's own progress arrow, as in the JEI categories
            int ax = x + arrowX(inputs);
            int ay = y + RECIPE_Y;
            gfx.blit(Ref.UiTextures.SLOT_PARTS, ax, ay, 26, 0, 24, 17);
            var state = be.getRecipeState();
            int filled = state == null ? 0 : (int) Math.round(24 * Math.min(100, state.getTickPercentage()) / 100);
            gfx.blit(Ref.UiTextures.SLOT_PARTS, ax, ay, 26, 17, filled, 17);
        }

        // the redstone value is a button: MM's button texture, pressed look on hover
        int by = y + rowY(Row.REDSTONE) - 2;
        var button = isOnRow(Row.REDSTONE, mouseX, mouseY) ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
        gfx.blitNineSlicedSized(button, x + VALUE_X - 2, by, RIGHT - VALUE_X + 2, 12, 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
        var pose = gfx.pose();
        pose.pushPose();
        pose.translate(x + VALUE_X, by + 1, 0);
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
        drawClipped(gfx, Component.literal(menu.getModel().name()), LEFT, NAME_Y, RIGHT - LEFT, 0xFFFFFF);

        Status status = status();
        drawClipped(gfx, Component.translatable(status.key()), LEFT + 9, STATUS_Y, RIGHT - LEFT - 9, status.color);

        gfx.drawString(this.font, Component.translatable("gui.mm.controller.recipe"), LEFT, RECIPE_LABEL_Y, LABEL, false);
        var slots = recipeSlots();
        if (slots.isEmpty()) {
            gfx.drawString(this.font, Component.translatable("gui.mm.controller.recipe.none"), LEFT, RECIPE_Y + 5, LABEL, false);
        } else {
            var state = be.getRecipeState();
            int percent = state == null ? 0 : (int) Math.floor(state.getTickPercentage());
            int lastX = slots.get(slots.size() - 1).x();
            gfx.drawString(this.font, percent + "%", lastX + 21, RECIPE_Y + 5, TEXT, false);
        }

        for (Row row : Row.values()) {
            drawClipped(gfx, Component.translatable("gui.mm.controller.row." + row.name().toLowerCase()),
                    LEFT, rowY(row), VALUE_X - LEFT - 4, LABEL);
        }
        var structure = be.getStructure();
        if (structure != null) {
            drawClipped(gfx, Component.literal(structure.name()), VALUE_X, rowY(Row.STRUCTURE), RIGHT - VALUE_X, TEXT);
            drawClipped(gfx, Component.translatable("gui.mm.controller.parallel.value", be.getActiveRecipeCount(), maxParallel()),
                    VALUE_X, rowY(Row.PARALLEL), RIGHT - VALUE_X, TEXT);
        } else {
            drawClipped(gfx, Component.translatable("gui.mm.controller.not_formed"), VALUE_X, rowY(Row.STRUCTURE), RIGHT - VALUE_X, Status.NOT_FORMED.color);
            gfx.drawString(this.font, "-", VALUE_X, rowY(Row.PARALLEL), LABEL, false);
        }
        drawClipped(gfx, Component.translatable("gui.mm.controller.redstone." + redstoneMode()),
                VALUE_X + 12, rowY(Row.REDSTONE), RIGHT - VALUE_X - 14, TEXT);
        drawClipped(gfx, Component.translatable("gui.mm.controller.mode." + recipeMode()),
                VALUE_X, rowY(Row.MODE), RIGHT - VALUE_X, TEXT);
    }

    private void drawClipped(GuiGraphics gfx, Component text, int x, int y, int maxWidth, int color) {
        FormattedText clipped = this.font.ellipsize(text, maxWidth);
        gfx.drawString(this.font, Language.getInstance().getVisualOrder(clipped), x, y, color, false);
    }

    private String redstoneMode() {
        return be.getRedstoneModeName().toLowerCase();
    }

    private String recipeMode() {
        return be.getRecipeSelectionMode().serializedName();
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
            return;
        }
        List<Component> tooltip = rowTooltip(mouseX, mouseY);
        if (tooltip != null) {
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Nullable
    private List<Component> rowTooltip(int mouseX, int mouseY) {
        if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + LEFT, this.topPos + STATUS_Y - 1, RIGHT - LEFT, 10)) {
            return List.of(Component.translatable(status().key() + ".hint").withStyle(ChatFormatting.GRAY));
        }
        for (Row row : Row.values()) {
            if (!isOnRow(row, mouseX, mouseY)) continue;
            String key = "gui.mm.controller.row." + row.name().toLowerCase();
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable(key));
            switch (row) {
                case STRUCTURE -> lines.add(Component.translatable(be.getStructure() != null
                        ? key + ".hint.formed" : key + ".hint.not_formed").withStyle(ChatFormatting.GRAY));
                case PARALLEL -> lines.add(Component.translatable(key + ".hint").withStyle(ChatFormatting.GRAY));
                case REDSTONE -> {
                    lines.add(Component.translatable("gui.mm.controller.redstone." + redstoneMode() + ".hint").withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable("gui.mm.controller.redstone.hint").withStyle(ChatFormatting.YELLOW));
                }
                case MODE -> lines.add(Component.translatable("gui.mm.controller.mode." + recipeMode() + ".hint").withStyle(ChatFormatting.GRAY));
            }
            return lines;
        }
        return null;
    }

    private boolean isOnRow(Row row, double mouseX, double mouseY) {
        double mx = mouseX - this.leftPos;
        double my = mouseY - this.topPos;
        int y = rowY(row) - 2;
        return mx >= LEFT && mx < RIGHT && my >= y && my < y + 12;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOnRow(Row.REDSTONE, mouseX, mouseY)) {
            int next = (be.getRedstoneModeOrdinal() + 1) % 3;
            MMNetwork.INSTANCE.sendToServer(new ToggleRedstoneModePkt(be.getBlockPos(), next));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
