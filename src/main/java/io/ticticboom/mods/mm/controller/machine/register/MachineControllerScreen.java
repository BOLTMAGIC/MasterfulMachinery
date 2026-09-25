package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;
import io.ticticboom.mods.mm.net.packet.ControllerSettingsPkt;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.FluidRenderer;
import io.ticticboom.mods.mm.client.gui.widgets.ControllerPortList;
import io.ticticboom.mods.mm.client.gui.widgets.PortContentIcon;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import io.ticticboom.mods.mm.compat.jei.JeiRecipeLookup;
import io.ticticboom.mods.mm.util.ChanceUtils;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ModList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final int VALUE_X = 80;

    // the texture's dark panel (y 7 to 125) is stretched by EXTRA_HEIGHT, repeating the plain band just above PANEL_SPLIT
    private static final int EXTRA_HEIGHT = MachineControllerMenu.EXTRA_HEIGHT;
    private static final int TEXTURE_HEIGHT = 222;
    private static final int PANEL_SPLIT = 80;
    private static final int NAME_Y = 10;
    private static final int STATUS_Y = 22;
    private static final int RECIPE_Y = 37;
    private static final int ROWS_Y = 64;
    private static final int ROW_STEP = 12;
    // one pixel shorter than a row, so stacked buttons don't touch
    private static final int BUTTON_HEIGHT = ROW_STEP - 1;

    private static final int MAX_INPUTS = 3;
    private static final int MAX_OUTPUTS = 2;
    private static final int SLOT_STEP = 19;
    private static final int TOOLTIP_MISSING = 10;
    // recipes with more inputs or outputs than fit show the next few this often
    private static final long CYCLE_MS = 2000;
    private static final boolean JEI = ModList.get().isLoaded("jei");

    private enum Row { STRUCTURE, TIER, PARALLEL, REDSTONE, MODE, SOUND, LINK }

    // packs name tiered structures like "Auto Crusher Tier 1.5"; the tier gets its own row
    private static final Pattern TIER = Pattern.compile("(?i)\\s*\\b(?:tier|seviye|level|lvl|mk)\\s*[.:#-]?\\s*(\\d+(?:[.,]\\d+)?|[ivx]+)\\b");

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

    // page toggle in the top-right corner of the screen
    private static final int PAGE_BTN_X = RIGHT - 12;
    private static final int PAGE_BTN_Y = 8;
    private static final int PAGE_BTN = 12;
    private static final int LIST_Y = 24;
    private static final int LIST_BOTTOM = 123 + EXTRA_HEIGHT;

    // 0 status, 1 input ports, 2 output ports; remembered while the game runs, like the port settings panel
    private static int page = 0;
    private static final int PAGE_COUNT = 3;
    // shown while the player renames the controller
    @Nullable
    private EditBox nameBox;

    private final MachineControllerMenu menu;
    private final MachineControllerBlockEntity be;
    private final ControllerPortList portList;
    // which few of a long input / output list are shown; advances every CYCLE_MS unless the mouse is on the recipe
    private int cycle = 0;
    private long nextCycleMs = 0;

    public MachineControllerScreen(MachineControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.menu = menu;
        this.be = (MachineControllerBlockEntity) menu.getBe();
        this.imageHeight = TEXTURE_HEIGHT + EXTRA_HEIGHT;
        this.imageWidth = 174;
        this.portList = new ControllerPortList(menu.getPortPositions());
    }

    @Override
    protected void init() {
        super.init();
        portList.setBounds(this.leftPos + LEFT, this.topPos + LIST_Y, RIGHT - LEFT + 2, LIST_BOTTOM - LIST_Y);
    }

    private boolean isOnPageButton(double mouseX, double mouseY) {
        return WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, this.leftPos + PAGE_BTN_X, this.topPos + PAGE_BTN_Y, PAGE_BTN, PAGE_BTN);
    }

    private void drawPageButton(GuiGraphics gfx, int mouseX, int mouseY) {
        int bx = this.leftPos + PAGE_BTN_X;
        int by = this.topPos + PAGE_BTN_Y;
        var texture = isOnPageButton(mouseX, mouseY) ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
        gfx.blitNineSlicedSized(texture, bx, by, PAGE_BTN, PAGE_BTN, 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
        // the button shows the page it leads to: hopper = inputs, dropper = outputs, comparator = status
        var next = switch (page) {
            case 0 -> Items.HOPPER;
            case 1 -> Items.DROPPER;
            default -> Items.COMPARATOR;
        };
        drawSmallItem(gfx, new ItemStack(next), bx + 1, by + 1);
    }

    private boolean onPortsPage() {
        return page != 0;
    }

    private static void drawSmallItem(GuiGraphics gfx, ItemStack stack, int x, int y) {
        drawSmallItem(gfx, stack, x, y, 10);
    }

    private static void drawSmallItem(GuiGraphics gfx, ItemStack stack, int x, int y, int size) {
        var pose = gfx.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(size / 16f, size / 16f, 1);
        gfx.renderItem(stack, 0, 0);
        pose.popPose();
    }

    /** @param chance 0-1 for outputs, 1 for inputs */
    private record Shown(int x, int y, PortContent content, boolean input, double chance) {
    }

    /**
     * The running recipe's row: the inputs and outputs shown right now, where the arrow and percentage go,
     * and how many turns each side needs to show everything (1 when all fit).
     */
    private record RecipeRow(List<Shown> slots, int arrowX, int percentX, int inputPages, int outputPages,
                             int outputX, int outputWidth) {
        static final RecipeRow EMPTY = new RecipeRow(List.of(), 0, 0, 1, 1, 0, 0);
    }

    private Status status() {
        if (be.getStructure() == null) return Status.NOT_FORMED;
        if (!be.isAllowedByRedstone()) return Status.PAUSED;
        if (be.getDisplayedRecipe() != null) return Status.RUNNING;
        return Status.IDLE;
    }

    private List<Shown> recipeSlots() {
        return recipeRow().slots();
    }

    /**
     * @return the running recipe laid out on the recipe row, GUI-relative. Recipes with more inputs or outputs
     * than fit show them a few at a time, in turn.
     */
    private RecipeRow recipeRow() {
        RecipeModel recipe = be.getDisplayedRecipe();
        if (recipe == null) {
            return RecipeRow.EMPTY;
        }
        var inputs = new ArrayList<PortContent>();
        for (var entry : recipe.inputs().inputs()) {
            if (entry instanceof ConsumeRecipeIngredientEntry consume) {
                PortContent content = consume.getIngredient().display();
                if (content != null) inputs.add(content);
            }
        }
        var outputs = new ArrayList<PortContent>();
        var chances = new ArrayList<Double>();
        for (var entry : recipe.outputs().outputs()) {
            for (DisplayedOutput output : entry.displayedOutputs()) {
                PortContent content = output.ingredient().display();
                if (content != null) {
                    outputs.add(content);
                    chances.add(output.chance());
                }
            }
        }
        int inputPages = pages(inputs.size(), MAX_INPUTS);
        int outputPages = pages(outputs.size(), MAX_OUTPUTS);
        var shown = new ArrayList<Shown>();
        // slots, arrow and percentage keep their place while the list turns, even on a shorter last turn
        int inputFrom = cycle % inputPages * MAX_INPUTS;
        for (int i = inputFrom; i < Math.min(inputs.size(), inputFrom + MAX_INPUTS); i++) {
            shown.add(new Shown(LEFT + (i - inputFrom) * SLOT_STEP, RECIPE_Y, inputs.get(i), true, 1));
        }
        int arrowX = LEFT + Math.min(inputs.size(), MAX_INPUTS) * SLOT_STEP + 3;
        int outputX = arrowX + 28;
        int outputFrom = cycle % outputPages * MAX_OUTPUTS;
        for (int i = outputFrom; i < Math.min(outputs.size(), outputFrom + MAX_OUTPUTS); i++) {
            shown.add(new Shown(outputX + (i - outputFrom) * SLOT_STEP, RECIPE_Y, outputs.get(i), false, chances.get(i)));
        }
        int outputWidth = Math.min(outputs.size(), MAX_OUTPUTS) * SLOT_STEP - 1;
        int percentX = outputs.isEmpty() ? outputX : outputX + outputWidth + 3;
        return new RecipeRow(shown, arrowX, percentX, inputPages, outputPages, outputX, outputWidth);
    }

    private static int pages(int count, int perPage) {
        return Math.max(1, (count + perPage - 1) / perPage);
    }

    /** Moves long input / output lists on, unless the mouse is on the recipe (so what it points at stays put). */
    private void advanceCycle(int mouseX, int mouseY) {
        // not reset when the shown recipe changes: short recipes come and go faster than one turn
        long now = Util.getMillis();
        boolean onRecipe = WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + LEFT, this.topPos + RECIPE_Y,
                RIGHT - LEFT, 18);
        if (onRecipe) {
            nextCycleMs = now + CYCLE_MS;
        } else if (now >= nextCycleMs) {
            cycle++;
            nextCycleMs = now + CYCLE_MS;
        }
    }

    /** A thin track under a group of slots, lit where the shown ones are in the whole list. */
    private void drawPageTrack(GuiGraphics gfx, int x, int width, int pages) {
        if (pages <= 1) return;
        int y = this.topPos + RECIPE_Y + 20;
        gfx.fill(x, y, x + width, y + 1, 0xFF333333);
        int page = cycle % pages;
        int from = width * page / pages;
        int to = width * (page + 1) / pages;
        gfx.fill(x + from, y, x + to, y + 1, 0xFFBBBBBB);
    }

    // rows are stacked in the order shown, so a hidden row leaves no gap
    private int rowY(Row row) {
        return ROWS_Y + rows().indexOf(row) * ROW_STEP;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int split = this.topPos + PANEL_SPLIT;
        gfx.blit(Ref.UiTextures.GUI_LARGE, this.leftPos, this.topPos, 0, 0, this.imageWidth, PANEL_SPLIT);
        gfx.blit(Ref.UiTextures.GUI_LARGE, this.leftPos, split, 0, PANEL_SPLIT - EXTRA_HEIGHT, this.imageWidth, EXTRA_HEIGHT);
        gfx.blit(Ref.UiTextures.GUI_LARGE, this.leftPos, split + EXTRA_HEIGHT, 0, PANEL_SPLIT, this.imageWidth, TEXTURE_HEIGHT - PANEL_SPLIT);
        drawPageButton(gfx, mouseX, mouseY);
        if (onPortsPage()) {
            portList.showInputs(page == 1);
            portList.render(gfx, this.font, be.getLevel());
            return;
        }
        int x = this.leftPos;
        int y = this.topPos;

        // status light
        gfx.fill(x + LEFT, y + STATUS_Y + 1, x + LEFT + 5, y + STATUS_Y + 6, 0xFF000000 | status().color);

        gfx.fill(x + LEFT, y + STATUS_Y + 11, x + RIGHT, y + STATUS_Y + 12, DIVIDER);
        gfx.fill(x + LEFT, y + ROWS_Y - 4, x + RIGHT, y + ROWS_Y - 3, DIVIDER);

        if (showsDiagnosis()) {
            // first missing block: its item in a slot, name and place beside it (renderLabels)
            var first = menu.getDiagnosis().missing().get(0);
            gfx.blit(Ref.UiTextures.SLOT_PARTS, x + LEFT, y + RECIPE_Y, 0, 26, 18, 18);
            if (!first.icon().isEmpty()) {
                gfx.renderItem(first.icon(), x + LEFT + 1, y + RECIPE_Y + 1);
            }
        }

        var row = recipeRow();
        if (!row.slots().isEmpty()) {
            for (Shown s : row.slots()) {
                gfx.blit(Ref.UiTextures.SLOT_PARTS, x + s.x(), y + s.y(), 0, 26, 18, 18);
                PortContentIcon.draw(gfx, s.content(), x + s.x(), y + s.y());
            }
            drawPageTrack(gfx, x + LEFT, MAX_INPUTS * SLOT_STEP - 1, row.inputPages());
            drawPageTrack(gfx, x + row.outputX(), row.outputWidth(), row.outputPages());
            // MM's own progress arrow, as in the JEI categories
            int ax = x + row.arrowX();
            int ay = y + RECIPE_Y;
            gfx.blit(Ref.UiTextures.SLOT_PARTS, ax, ay, 26, 0, 24, 17);
            var state = be.getRecipeState();
            // no state: a recent recipe that already finished (it took a tick or so)
            int filled = state == null ? 24 : (int) Math.round(24 * Math.min(100, state.getTickPercentage()) / 100);
            gfx.blit(Ref.UiTextures.SLOT_PARTS, ax, ay, 26, 17, filled, 17);
        }

        // the redstone value is a button: MM's button texture, pressed look on hover
        int by = y + rowY(Row.REDSTONE) - 2;
        var button = isOnRow(Row.REDSTONE, mouseX, mouseY) ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
        gfx.blitNineSlicedSized(button, x + VALUE_X - 2, by, RIGHT - VALUE_X + 2, BUTTON_HEIGHT, 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
        drawSmallItem(gfx, new ItemStack(Items.REDSTONE), x + VALUE_X, by + 1, 8);

        int my = y + rowY(Row.MODE) - 2;
        var modeButton = isOnRow(Row.MODE, mouseX, mouseY) ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
        gfx.blitNineSlicedSized(modeButton, x + VALUE_X - 2, my, RIGHT - VALUE_X + 2, BUTTON_HEIGHT, 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);

        if (hasWorkingSound()) {
            int sy = y + rowY(Row.SOUND) - 2;
            var soundButton = isOnRow(Row.SOUND, mouseX, mouseY) ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
            gfx.blitNineSlicedSized(soundButton, x + VALUE_X - 2, sy, RIGHT - VALUE_X + 2, BUTTON_HEIGHT, 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
            drawSmallItem(gfx, new ItemStack(Items.NOTE_BLOCK), x + VALUE_X, sy + 1, 8);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        if (nameBox == null) {
            drawClipped(gfx, be.getName(), LEFT, NAME_Y, PAGE_BTN_X - LEFT - 4, 0xFFFFFF);
        }
        if (onPortsPage()) {
            return;
        }

        Status status = status();
        int statusWidth = RIGHT - LEFT - 9;
        if (showsDiagnosis()) {
            // how many blocks are missing, right-aligned on the status line
            var count = Component.translatable("gui.mm.controller.missing", menu.getDiagnosis().total());
            int countWidth = Math.min(this.font.width(count), RIGHT - LEFT - 9 - 40);
            drawClipped(gfx, count, RIGHT - countWidth, STATUS_Y, countWidth, Status.NOT_FORMED.color);
            statusWidth -= countWidth + 4;
        }
        drawClipped(gfx, Component.translatable(status.key()), LEFT + 9, STATUS_Y, statusWidth, status.color);

        var recipe = recipeRow();
        if (showsDiagnosis()) {
            // the first missing block takes the recipe's place
            var first = menu.getDiagnosis().missing().get(0);
            drawClipped(gfx, first.required(), LEFT + 21, RECIPE_Y, RIGHT - LEFT - 21, TEXT);
            drawClipped(gfx, offsetText(first.pos()), LEFT + 21, RECIPE_Y + 10, RIGHT - LEFT - 21, LABEL);
        } else if (recipe.slots().isEmpty()) {
            gfx.drawString(this.font, Component.translatable("gui.mm.controller.recipe.none"), LEFT, RECIPE_Y + 5, LABEL, false);
        } else {
            var state = be.getRecipeState();
            int percent = state == null ? 100 : (int) Math.floor(state.getTickPercentage());
            drawClipped(gfx, Component.literal(percent + "%"), recipe.percentX(), RECIPE_Y + 5, RIGHT - recipe.percentX(), TEXT);
        }

        for (Row row : rows()) {
            drawClipped(gfx, Component.translatable("gui.mm.controller.row." + row.name().toLowerCase()),
                    LEFT, rowY(row), VALUE_X - LEFT - 4, LABEL);
        }
        var structure = be.getStructure();
        if (structure != null) {
            Matcher tier = TIER.matcher(structure.name());
            boolean hasTier = tier.find();
            String baseName = hasTier ? (structure.name().substring(0, tier.start()) + structure.name().substring(tier.end())).trim() : structure.name();
            drawClipped(gfx, Component.literal(baseName), VALUE_X, rowY(Row.STRUCTURE), RIGHT - VALUE_X, TEXT);
            if (hasTier) {
                gfx.drawString(this.font, tier.group(1), VALUE_X, rowY(Row.TIER), TEXT, false);
            } else {
                gfx.drawString(this.font, "-", VALUE_X, rowY(Row.TIER), LABEL, false);
            }
            drawClipped(gfx, Component.translatable("gui.mm.controller.parallel.value", be.getActiveRecipeCount(), be.getDisplayedParallelLimit()),
                    VALUE_X, rowY(Row.PARALLEL), RIGHT - VALUE_X, TEXT);
        } else {
            drawClipped(gfx, Component.translatable("gui.mm.controller.not_formed"), VALUE_X, rowY(Row.STRUCTURE), RIGHT - VALUE_X, Status.NOT_FORMED.color);
            gfx.drawString(this.font, "-", VALUE_X, rowY(Row.TIER), LABEL, false);
            gfx.drawString(this.font, "-", VALUE_X, rowY(Row.PARALLEL), LABEL, false);
        }
        drawClipped(gfx, Component.translatable("gui.mm.controller.redstone." + redstoneMode()),
                VALUE_X + 12, rowY(Row.REDSTONE), RIGHT - VALUE_X - 14, TEXT);
        drawClipped(gfx, Component.translatable("gui.mm.controller.mode." + recipeMode()),
                VALUE_X + 1, rowY(Row.MODE), RIGHT - VALUE_X - 3, TEXT);
        if (hasWorkingSound()) {
            drawClipped(gfx, Component.translatable(be.isSoundMuted() ? "gui.mm.controller.sound.off" : "gui.mm.controller.sound.on"),
                    VALUE_X + 12, rowY(Row.SOUND), RIGHT - VALUE_X - 14, be.isSoundMuted() ? LABEL : TEXT);
        } else {
            // not a button: there is nothing to mute
            drawClipped(gfx, Component.translatable("gui.mm.controller.sound.none"), VALUE_X, rowY(Row.SOUND), RIGHT - VALUE_X, LABEL);
        }
        if (NetworkLink.AVAILABLE) {
            LinkData link = be.getNetworkLink();
            if (link != null) {
                drawClipped(gfx, Component.literal(link.ownerName()), VALUE_X, rowY(Row.LINK), RIGHT - VALUE_X, TEXT);
            } else {
                drawClipped(gfx, Component.translatable("gui.mm.controller.link.none"), VALUE_X, rowY(Row.LINK), RIGHT - VALUE_X, LABEL);
            }
        }
    }

    /** The rows shown; the network link row only exists with AE2. */
    private static List<Row> rows() {
        return NetworkLink.AVAILABLE ? List.of(Row.values()) : List.of(Row.STRUCTURE, Row.TIER, Row.PARALLEL, Row.REDSTONE, Row.MODE, Row.SOUND);
    }

    private boolean hasWorkingSound() {
        return be.getBlockState().getBlock() instanceof MachineControllerBlock block && block.hasWorkingSound();
    }

    /** While the multiblock isn't formed, the recipe section lists what is missing where. */
    private boolean showsDiagnosis() {
        return status() == Status.NOT_FORMED && !menu.getDiagnosis().missing().isEmpty();
    }

    /**
     * Where a block goes, relative to the controller as seen by a player standing in front of it,
     * e.g. "2 up, 1 left, 3 back", followed by the coordinates.
     */
    private Component offsetText(BlockPos pos) {
        BlockPos delta = pos.subtract(be.getBlockPos());
        var state = be.getBlockState();
        var parts = new ArrayList<Component>();
        addOffset(parts, delta.getY(), "up", "down");
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            // the controller's screen faces opposite to FACING
            Direction front = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
            Direction left = PortSides.toWorld(PortSides.Relative.LEFT, front);
            addOffset(parts, dot(delta, front), "front", "back");
            addOffset(parts, dot(delta, left), "left", "right");
        }
        MutableComponent text = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) text.append(", ");
            text.append(parts.get(i));
        }
        return text.append(" (" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + ")");
    }

    private static void addOffset(List<Component> parts, int amount, String positive, String negative) {
        if (amount != 0) {
            parts.add(Component.translatable("gui.mm.controller.missing.offset." + (amount > 0 ? positive : negative), Math.abs(amount)));
        }
    }

    private static int dot(BlockPos delta, Direction dir) {
        return delta.getX() * dir.getStepX() + delta.getY() * dir.getStepY() + delta.getZ() * dir.getStepZ();
    }

    private List<Component> diagnosisTooltip() {
        var diagnosis = menu.getDiagnosis();
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("gui.mm.controller.missing", diagnosis.total()));
        int shown = Math.min(TOOLTIP_MISSING, diagnosis.missing().size());
        for (int i = 0; i < shown; i++) {
            var m = diagnosis.missing().get(i);
            MutableComponent line = m.required().copy().withStyle(ChatFormatting.WHITE);
            if (m.found() != null) {
                line.append(Component.translatable("gui.mm.controller.missing.found", m.found()).withStyle(ChatFormatting.RED));
            }
            lines.add(line);
            lines.add(Component.literal("  ").append(offsetText(m.pos())).withStyle(ChatFormatting.GRAY));
        }
        if (diagnosis.total() > shown) {
            lines.add(Component.translatable("gui.mm.controller.missing.more", diagnosis.total() - shown).withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.add(Component.translatable("gui.mm.controller.missing.hint").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
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

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        advanceCycle(mouseX, mouseY);
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partial);
        renderTooltip(gfx, mouseX, mouseY);

        Shown hovered = hoveredSlot(mouseX, mouseY);
        if (hovered != null) {
            var content = hovered.content();
            var lines = new ArrayList<Component>(content.kind() == PortContent.Kind.ITEM
                    ? Screen.getTooltipFromItem(Minecraft.getInstance(), content.item())
                    : PortContentIcon.tooltip(content));
            if (hovered.chance() < 1) {
                lines.add(Component.translatable("gui.mm.controller.recipe.chance", ChanceUtils.formatPercent(hovered.chance()))
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
            if (JEI && JeiRecipeLookup.canShow(content)) {
                lines.add(Component.translatable("gui.mm.controller.recipe.jei").withStyle(ChatFormatting.YELLOW));
            }
            if (content.kind() == PortContent.Kind.ITEM) {
                gfx.renderComponentTooltip(this.font, lines, mouseX, mouseY, content.item());
            } else {
                gfx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            }
            return;
        }
        if (isOnPageButton(mouseX, mouseY)) {
            String next = switch (page) {
                case 0 -> "gui.mm.controller.page.inputs";
                case 1 -> "gui.mm.controller.page.outputs";
                default -> "gui.mm.controller.page.status";
            };
            gfx.renderComponentTooltip(this.font, List.of(Component.translatable(next)), mouseX, mouseY);
            return;
        }
        List<Component> tooltip = onPortsPage() ? portList.tooltip(this.font, be.getLevel(), mouseX, mouseY) : rowTooltip(mouseX, mouseY);
        if (tooltip != null) {
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Nullable
    private List<Component> rowTooltip(int mouseX, int mouseY) {
        // long machine names are cut short on screen; show them in full on hover
        if (nameBox == null && isOnName(mouseX, mouseY)) {
            var lines = new ArrayList<Component>();
            lines.add(be.getName());
            if (be.getCustomName() != null) {
                lines.add(Component.literal(menu.getModel().name()).withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable("gui.mm.controller.rename.hint").withStyle(ChatFormatting.YELLOW));
            return lines;
        }
        if (showsDiagnosis() && WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + LEFT, this.topPos + RECIPE_Y - 1,
                RIGHT - LEFT, 20)) {
            return diagnosisTooltip();
        }
        if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + LEFT, this.topPos + STATUS_Y - 1, RIGHT - LEFT, 10)) {
            return List.of(Component.translatable(status().key() + ".hint").withStyle(ChatFormatting.GRAY));
        }
        for (Row row : rows()) {
            if (!isOnRow(row, mouseX, mouseY)) continue;
            String key = "gui.mm.controller.row." + row.name().toLowerCase();
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable(key));
            switch (row) {
                case STRUCTURE -> {
                    if (be.getStructure() != null) {
                        lines.add(Component.literal(be.getStructure().name()).withStyle(ChatFormatting.WHITE));
                    }
                    lines.add(Component.translatable(be.getStructure() != null
                        ? key + ".hint.formed" : key + ".hint.not_formed").withStyle(ChatFormatting.GRAY));
                }
                case TIER -> lines.add(Component.translatable(key + ".hint").withStyle(ChatFormatting.GRAY));
                case PARALLEL -> lines.add(Component.translatable(key + ".hint").withStyle(ChatFormatting.GRAY));
                case REDSTONE -> {
                    lines.add(Component.translatable("gui.mm.controller.redstone." + redstoneMode() + ".hint").withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable("gui.mm.controller.redstone.hint").withStyle(ChatFormatting.YELLOW));
                }
                case MODE -> {
                    lines.add(Component.translatable("gui.mm.controller.mode." + recipeMode() + ".hint").withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable("gui.mm.controller.mode.hint").withStyle(ChatFormatting.YELLOW));
                }
                case SOUND -> {
                    if (hasWorkingSound()) {
                        lines.add(Component.translatable(be.isSoundMuted() ? "gui.mm.controller.sound.off.hint" : "gui.mm.controller.sound.on.hint").withStyle(ChatFormatting.GRAY));
                        lines.add(Component.translatable("gui.mm.controller.sound.hint").withStyle(ChatFormatting.YELLOW));
                    } else {
                        lines.add(Component.translatable("gui.mm.controller.sound.none.hint").withStyle(ChatFormatting.GRAY));
                    }
                }
                case LINK -> {
                    LinkData link = be.getNetworkLink();
                    if (link != null) {
                        lines.add(Component.translatable("gui.mm.controller.link.owner", link.ownerName()).withStyle(ChatFormatting.WHITE));
                        lines.add(Component.translatable("gui.mm.controller.link.network", link.network().pos().toShortString(),
                                link.network().dimension().location().getPath()).withStyle(ChatFormatting.AQUA));
                        lines.add(Component.translatable("gui.mm.controller.row.link.hint.linked").withStyle(ChatFormatting.GRAY));
                    } else {
                        lines.add(Component.translatable("gui.mm.controller.row.link.hint.none").withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            return lines;
        }
        return null;
    }

    /** @return the recipe input or output under the mouse, null when none (or not on the status page) */
    @Nullable
    private Shown hoveredSlot(double mouseX, double mouseY) {
        if (onPortsPage()) return null;
        for (Shown s : recipeSlots()) {
            if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, this.leftPos + s.x(), this.topPos + s.y(), 18, 18)) {
                return s;
            }
        }
        return null;
    }

    private boolean isOnRow(Row row, double mouseX, double mouseY) {
        double mx = mouseX - this.leftPos;
        double my = mouseY - this.topPos;
        int y = rowY(row) - 2;
        return mx >= LEFT && mx < RIGHT && my >= y && my < y + ROW_STEP;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (nameBox != null) {
            if (nameBox.isMouseOver(mouseX, mouseY)) {
                return nameBox.mouseClicked(mouseX, mouseY, button);
            }
            // clicking elsewhere keeps the typed name
            finishRename(true);
        }
        if (!onPortsPage() && isOnName(mouseX, mouseY)) {
            startRename();
            return true;
        }
        Shown clicked = hoveredSlot(mouseX, mouseY);
        if (clicked != null && JEI && (button == 0 || button == 1)) {
            // left: how it's made, right: what uses it (JEI's R / U)
            return JeiRecipeLookup.show(clicked.content(), button == 1);
        }
        if (isOnPageButton(mouseX, mouseY)) {
            page = (page + 1) % PAGE_COUNT;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        if (!onPortsPage() && isOnRow(Row.MODE, mouseX, mouseY)) {
            var modes = RecipeSelectionMode.values();
            var next = modes[(be.getRecipeSelectionMode().ordinal() + 1) % modes.length];
            MMNetwork.INSTANCE.sendToServer(new ControllerSettingsPkt(be.getBlockPos(), ControllerSettingsPkt.Setting.RECIPE_ORDER, next.serializedName()));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        if (!onPortsPage() && hasWorkingSound() && isOnRow(Row.SOUND, mouseX, mouseY)) {
            MMNetwork.INSTANCE.sendToServer(new ControllerSettingsPkt(be.getBlockPos(), ControllerSettingsPkt.Setting.SOUND_MUTED, Boolean.toString(!be.isSoundMuted())));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        if (!onPortsPage() && isOnRow(Row.REDSTONE, mouseX, mouseY)) {
            int next = (be.getRedstoneModeOrdinal() + 1) % 3;
            MMNetwork.INSTANCE.sendToServer(new ToggleRedstoneModePkt(be.getBlockPos(), next));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOnName(double mouseX, double mouseY) {
        return WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, this.leftPos + LEFT, this.topPos + NAME_Y - 1, PAGE_BTN_X - LEFT - 4, 10);
    }

    private void startRename() {
        nameBox = new EditBox(this.font, this.leftPos + LEFT - 1, this.topPos + NAME_Y - 2, PAGE_BTN_X - LEFT - 2, 12, Component.empty());
        nameBox.setMaxLength(MachineControllerBlockEntity.MAX_NAME_LENGTH);
        nameBox.setValue(be.getName().getString());
        addRenderableWidget(nameBox);
        setFocused(nameBox);
        nameBox.setFocused(true);
    }

    /** @param keep send the typed name (an empty or unchanged name goes back to the machine's own name) */
    private void finishRename(boolean keep) {
        if (nameBox == null) return;
        if (keep) {
            String typed = nameBox.getValue().strip();
            String name = typed.equals(menu.getModel().name()) ? "" : typed;
            MMNetwork.INSTANCE.sendToServer(new ControllerSettingsPkt(be.getBlockPos(), ControllerSettingsPkt.Setting.NAME, name));
        }
        removeWidget(nameBox);
        nameBox = null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // while renaming, keys go to the name box (so E or Esc don't close the screen)
        if (nameBox != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                finishRename(true);
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                finishRename(false);
            } else {
                nameBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (onPortsPage() && portList.mouseScrolled(mouseX, mouseY, delta, be.getLevel())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
