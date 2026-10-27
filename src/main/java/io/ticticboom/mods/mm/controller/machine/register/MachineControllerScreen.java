package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.controller.single.register.SingleMachineBlockEntity;
import io.ticticboom.mods.mm.net.packet.OpenMachineScreenPkt;
import io.ticticboom.mods.mm.client.builder.AssemblyScreen;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.ControllerPortList;
import io.ticticboom.mods.mm.client.gui.widgets.PortContentIcon;
import io.ticticboom.mods.mm.client.util.NineSliceUtil;
import io.ticticboom.mods.mm.compat.jei.JeiRecipeLookup;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.net.packet.ControllerSettingsPkt;
import io.ticticboom.mods.mm.net.packet.SelectRecipePkt;
import io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.count.CountRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import io.ticticboom.mods.mm.util.ChanceUtils;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MachineControllerScreen extends AbstractContainerScreen<MachineControllerMenu> {
    private static final int TEXT = 0xDDDDDD;
    private static final int LABEL = 0x8A8A8A;
    private static final int DIVIDER = 0xFF3A3A3A;
    private static final int LEFT = 10;
    private static final int VALUE_X = 80;
    private static final int VALUE_WIDTH = 150;

    private static final int MIN_WIDTH = 174;
    private static final int MIN_HEIGHT = 246;
    private static final int MAX_WIDTH = 500;
    private static final int MAX_HEIGHT = 380;
    private static final int WINDOW_MARGIN = 24;
    private static final int FRAME = 6;
    private static final int FRAME_BOTTOM = 7;
    private static final int INVENTORY_WIDTH = 162;
    private static final int INVENTORY_HEIGHT = 76;
    private static final int PANEL_GAP = 12;
    private static final int GREY = 0xFFC6C6C6;
    private static final int WIDE_WIDTH = 480;
    private static final int INFO_WIDTH = 190;
    private static final int COLUMN_GAP = 12;
    private static final int PANEL = 0xFF1C1C1C;
    private static final int NAME_Y = 10;
    private static final int STATUS_Y = 22;
    private static final int RECIPE_Y = 37;
    private static final int ROWS_Y = 64;
    private static final int ROW_STEP = 12;
    private static final int BUTTON_HEIGHT = ROW_STEP - 1;

    private static final int MIN_INPUTS = 3;
    private static final int MIN_OUTPUTS = 2;
    private static final int SLOT_STEP = 19;
    private static final int TOOLTIP_MISSING = 10;
    private static final int TOOLTIP_OPTIONS = 8;
    private static final long CYCLE_MS = 1200;
    private static final int PREVIEW_PAD = 4;
    private static final int PREVIEW_ARROW = 32;
    private static final boolean JEI = ModList.get().isLoaded("jei");

    private enum Row { STRUCTURE, TIER, PARALLEL, REDSTONE, MODE, SOUND, LINK, RECIPE }

    private static final Pattern TIER = Pattern.compile("(?i)\\s*\\b(?:tier|level|lvl|mk)\\s*[.:#-]?\\s*(\\d+(?:[.,]\\d+)?|[ivx]+)\\b");

    private enum Status {
        NOT_FORMED(0xFF5555), PAUSED(0xFFAA00), RUNNING(0x55FF55), STALLED(0xFF8844), IDLE(0xE0C050);

        final int color;

        Status(int color) {
            this.color = color;
        }

        String key() {
            return "gui.mm.controller.status." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int PAGE_BTN_Y = 8;
    private static final int PAGE_BTN = 12;
    private static final int LIST_Y = 24;
    private static final int PAGE_COUNT = 3;

    private static int page = 0;
    @Nullable
    private EditBox nameBox;

    private final MachineControllerBlockEntity be;
    private final boolean single;
    private final ControllerPortList portList;
    private final ControllerPortList outputList;
    private int cycle = 0;
    private long nextCycleMs = 0;

    private int right;
    private int valueRight;
    private int pageBtnX;
    private int sizeBtnX;
    private int assembleBtnX;
    private int panelBottom;
    private int inventoryX;
    private int inventoryY;
    private boolean wide;
    private int infoRight;
    private int maxInputs = MIN_INPUTS;
    private int maxOutputs = MIN_OUTPUTS;

    public MachineControllerScreen(MachineControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = (MachineControllerBlockEntity) menu.getBe();
        this.single = be instanceof SingleMachineBlockEntity;
        this.imageHeight = MIN_HEIGHT;
        this.imageWidth = MIN_WIDTH;
        this.portList = single ? new ControllerPortList(this::slotEntries) : new ControllerPortList(menu.getPortPositions());
        this.outputList = single ? new ControllerPortList(this::slotEntries) : new ControllerPortList(menu.getPortPositions());
        this.outputList.showInputs(false);
    }

    @Override
    protected void init() {
        if (bigScreen()) {
            this.imageWidth = Mth.clamp(this.width - 2 * WINDOW_MARGIN, MIN_WIDTH, MAX_WIDTH);
            this.imageHeight = Mth.clamp(this.height - 2 * WINDOW_MARGIN, MIN_HEIGHT, MAX_HEIGHT);
        } else {
            this.imageWidth = MIN_WIDTH;
            this.imageHeight = MIN_HEIGHT;
        }
        right = imageWidth - 11;
        wide = imageWidth >= WIDE_WIDTH;
        infoRight = wide ? LEFT + INFO_WIDTH : right;
        valueRight = Math.min(infoRight, VALUE_X + VALUE_WIDTH);
        pageBtnX = right - 12;
        sizeBtnX = wide ? pageBtnX : pageBtnX - PAGE_BTN - 2;
        assembleBtnX = sizeBtnX - PAGE_BTN - 2;
        inventoryX = (imageWidth - INVENTORY_WIDTH) / 2;
        inventoryY = imageHeight - FRAME_BOTTOM - INVENTORY_HEIGHT;
        panelBottom = inventoryY - PANEL_GAP;
        int slots = (right - LEFT - 28 - 30) / SLOT_STEP;
        maxOutputs = Math.max(MIN_OUTPUTS, slots / 2);
        maxInputs = Math.max(MIN_INPUTS, slots - maxOutputs);
        super.init();
        placeInventorySlots();
        if (wide) {
            int listsX = infoRight + COLUMN_GAP;
            int listTop = ROWS_Y - 2;
            int listW = (right + 2 - listsX - COLUMN_GAP) / 2;
            portList.showInputs(true);
            portList.setBounds(this.leftPos + listsX, this.topPos + listTop, listW, panelBottom - 3 - listTop);
            outputList.setBounds(this.leftPos + listsX + listW + COLUMN_GAP, this.topPos + listTop, listW, panelBottom - 3 - listTop);
        } else {
            portList.setBounds(this.leftPos + LEFT, this.topPos + LIST_Y, right - LEFT + 2, panelBottom - 3 - LIST_Y);
        }
    }

    private void placeInventorySlots() {
        var slots = menu.slots;
        for (int i = 0; i < slots.size(); i++) {
            Slot old = slots.get(i);
            int x = inventoryX + 1 + (i % 9) * 18;
            int y = i < 27 ? inventoryY + 1 + (i / 9) * 18 : inventoryY + 59;
            Slot moved = new Slot(old.container, old.getContainerSlot(), x, y);
            moved.index = old.index;
            slots.set(i, moved);
        }
    }

    private void drawBackground(GuiGraphics gfx) {
        var texture = Ref.UiTextures.GUI_LARGE;
        int x = this.leftPos;
        int y = this.topPos;
        NineSliceUtil.blitNineSliced(gfx, texture, x, y, imageWidth, imageHeight, FRAME, FRAME, FRAME, FRAME_BOTTOM, 174, 222, 0, 0, 256, 256);
        gfx.fill(x + FRAME, y + FRAME, x + imageWidth - FRAME, y + imageHeight - FRAME_BOTTOM, GREY);
        NineSliceUtil.blitNineSliced(gfx, texture, x + FRAME, y + FRAME, imageWidth - 2 * FRAME, panelBottom - FRAME + 1, 2, 2, 2, 2, 162, 121, 6, 6, 256, 256);
        gfx.fill(x + FRAME + 2, y + FRAME + 2, x + imageWidth - FRAME - 2, y + panelBottom - 1, PANEL);
        gfx.blit(texture, x + inventoryX, y + inventoryY, 6, 139, INVENTORY_WIDTH, INVENTORY_HEIGHT);
    }

    private List<ControllerPortList.Entry> slotEntries() {
        var machine = (SingleMachineBlockEntity) be;
        var icon = new ItemStack(machine.getBlockState().getBlock());
        var entries = new ArrayList<ControllerPortList.Entry>();
        for (int i = 0; i < machine.getSlots().size(); i++) {
            var slot = machine.getSlots().get(i);
            String id = slot.id().replace('_', ' ');
            var name = Component.literal(id.isEmpty() ? id : Character.toUpperCase(id.charAt(0)) + id.substring(1));
            entries.add(new ControllerPortList.Entry(name, icon, slot.input(), machine.getStorages().get(i)));
        }
        return entries;
    }

    private static boolean bigScreen() {
        return MMConfigSetup.CLIENT.bigControllerScreen.get();
    }

    private void drawButtonFrame(GuiGraphics gfx, int bx, int by, int w, int h, boolean hovered) {
        var texture = hovered ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
        NineSliceUtil.blitNineSliced(gfx, texture, bx, by, w, h, 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
    }

    private boolean isOnSizeButton(double mouseX, double mouseY) {
        return WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, this.leftPos + sizeBtnX, this.topPos + PAGE_BTN_Y, PAGE_BTN, PAGE_BTN);
    }

    private void drawSizeButton(GuiGraphics gfx, int mouseX, int mouseY) {
        int bx = this.leftPos + sizeBtnX;
        int by = this.topPos + PAGE_BTN_Y;
        drawButtonFrame(gfx, bx, by, PAGE_BTN, PAGE_BTN, isOnSizeButton(mouseX, mouseY));
        int ink = 0xFF3A3A3A;
        if (bigScreen()) {
            gfx.fill(bx + 4, by + 4, bx + 8, by + 8, ink);
        } else {
            gfx.fill(bx + 2, by + 2, bx + 10, by + 3, ink);
            gfx.fill(bx + 2, by + 9, bx + 10, by + 10, ink);
            gfx.fill(bx + 2, by + 3, bx + 3, by + 9, ink);
            gfx.fill(bx + 9, by + 3, bx + 10, by + 9, ink);
        }
    }

    private boolean isOnAssembleButton(double mouseX, double mouseY) {
        return WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, this.leftPos + assembleBtnX, this.topPos + PAGE_BTN_Y, PAGE_BTN, PAGE_BTN);
    }

    private void drawAssembleButton(GuiGraphics gfx, int mouseX, int mouseY) {
        int bx = this.leftPos + assembleBtnX;
        int by = this.topPos + PAGE_BTN_Y;
        drawButtonFrame(gfx, bx, by, PAGE_BTN, PAGE_BTN, isOnAssembleButton(mouseX, mouseY));
        drawSmallItem(gfx, new ItemStack(single ? Items.CHEST : Items.BRICKS), bx + 1, by + 1, 10);
    }

    private void toggleSize() {
        MMConfigSetup.CLIENT.bigControllerScreen.set(!bigScreen());
        MMConfigSetup.CLIENT.bigControllerScreen.save();
        if (nameBox != null) finishRename(true);
        init(this.minecraft, this.width, this.height);
    }

    private boolean isOnPageButton(double mouseX, double mouseY) {
        return !wide && WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, this.leftPos + pageBtnX, this.topPos + PAGE_BTN_Y, PAGE_BTN, PAGE_BTN);
    }

    private void drawPageButton(GuiGraphics gfx, int mouseX, int mouseY) {
        if (wide) return;
        int bx = this.leftPos + pageBtnX;
        int by = this.topPos + PAGE_BTN_Y;
        drawButtonFrame(gfx, bx, by, PAGE_BTN, PAGE_BTN, isOnPageButton(mouseX, mouseY));
        var next = switch (page) {
            case 0 -> Items.HOPPER;
            case 1 -> Items.DROPPER;
            default -> Items.COMPARATOR;
        };
        drawSmallItem(gfx, new ItemStack(next), bx + 1, by + 1, 10);
    }

    private boolean onPortsPage() {
        return !wide && page != 0;
    }

    private static void drawSmallItem(GuiGraphics gfx, ItemStack stack, int x, int y, int size) {
        var pose = gfx.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(size / 16f, size / 16f, 1);
        gfx.renderItem(stack, 0, 0);
        pose.popPose();
    }

    private record Shown(int x, int y, PortContent content, boolean input, double chance) {
    }

    private record RecipeContents(List<PortContent> inputs, List<PortContent> outputs, List<Double> chances) {
    }

    private record Preview(List<Shown> slots, int top, int height, int arrowX, int arrowY) {
        static final Preview EMPTY = new Preview(List.of(), 0, 0, 0, 0);
    }

    private record RecipeRow(List<Shown> slots, int arrowX, int percentX, int inputPages, int outputPages,
                             int outputX, int outputWidth) {
        static final RecipeRow EMPTY = new RecipeRow(List.of(), 0, 0, 1, 1, 0, 0);
    }

    private Status status() {
        if (be.getStructure() == null) return Status.NOT_FORMED;
        if (!be.isAllowedByRedstone()) return Status.PAUSED;
        if (be.isWorking()) return Status.RUNNING;
        if (be.getActiveRecipeCount() > 0) return Status.STALLED;
        return Status.IDLE;
    }

    @Nullable
    private RecipeModel shownRecipe() {
        return status() == Status.IDLE ? null : be.getDisplayedRecipe();
    }

    private RecipeRow recipeRow() {
        RecipeModel recipe = shownRecipe();
        if (recipe == null) {
            return RecipeRow.EMPTY;
        }
        var contents = recipeContents(recipe);
        var inputs = contents.inputs();
        var outputs = contents.outputs();
        var chances = contents.chances();
        int inputPages = pages(inputs.size(), maxInputs);
        int outputPages = pages(outputs.size(), maxOutputs);
        var shown = new ArrayList<Shown>();
        int inputFrom = cycle % inputPages * maxInputs;
        for (int i = inputFrom; i < Math.min(inputs.size(), inputFrom + maxInputs); i++) {
            shown.add(new Shown(LEFT + (i - inputFrom) * SLOT_STEP, RECIPE_Y, inputs.get(i), true, 1));
        }
        int arrowX = LEFT + Math.min(inputs.size(), maxInputs) * SLOT_STEP + 3;
        int outputX = arrowX + 28;
        int outputFrom = cycle % outputPages * maxOutputs;
        for (int i = outputFrom; i < Math.min(outputs.size(), outputFrom + maxOutputs); i++) {
            shown.add(new Shown(outputX + (i - outputFrom) * SLOT_STEP, RECIPE_Y, outputs.get(i), false, chances.get(i)));
        }
        int outputWidth = Math.min(outputs.size(), maxOutputs) * SLOT_STEP - 1;
        int percentX = outputs.isEmpty() ? outputX : outputX + outputWidth + 3;
        return new RecipeRow(shown, arrowX, percentX, inputPages, outputPages, outputX, outputWidth);
    }

    private static RecipeContents recipeContents(RecipeModel recipe) {
        var inputs = new ArrayList<PortContent>();
        for (var entry : recipe.inputs().inputs()) {
            PortContent content = null;
            if (entry instanceof ConsumeRecipeIngredientEntry consume) {
                content = consume.getIngredient().display();
            } else if (entry instanceof FlexRecipeIngredientEntry flex) {
                content = flex.getIngredient().display();
            } else if (entry instanceof CountRecipeIngredientEntry count) {
                content = count.getIngredient().display();
            }
            if (content != null) inputs.add(content);
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
        return new RecipeContents(inputs, outputs, chances);
    }

    private Preview preview() {
        RecipeModel recipe = rows().contains(Row.RECIPE) ? selectedRecipe() : null;
        if (recipe == null) {
            return Preview.EMPTY;
        }
        var contents = recipeContents(recipe);
        if (contents.inputs().isEmpty() && contents.outputs().isEmpty()) {
            return Preview.EMPTY;
        }
        int top = wide && shownRecipe() != null ? progressY() + 24 : progressY() - 4;
        int width = infoRight - LEFT - 2 * PREVIEW_PAD;
        int outCols = Mth.clamp(contents.outputs().size(), 1, 3);
        int inCols = Math.max(1, (width - PREVIEW_ARROW - outCols * SLOT_STEP + 1) / SLOT_STEP);
        int maxLines = (panelBottom - 3 - top - 2 * PREVIEW_PAD + 1) / SLOT_STEP;
        int lines = Math.min(maxLines, Math.max(pages(contents.inputs().size(), inCols), pages(contents.outputs().size(), outCols)));
        if (lines < 1) {
            return Preview.EMPTY;
        }
        int x = LEFT + PREVIEW_PAD;
        int y = top + PREVIEW_PAD;
        var slots = new ArrayList<Shown>();
        for (int i = 0; i < Math.min(contents.inputs().size(), inCols * lines); i++) {
            slots.add(new Shown(x + i % inCols * SLOT_STEP, y + i / inCols * SLOT_STEP, contents.inputs().get(i), true, 1));
        }
        int arrowX = x + Math.min(contents.inputs().size(), inCols) * SLOT_STEP + 3;
        int outputX = arrowX + PREVIEW_ARROW - 4;
        for (int i = 0; i < Math.min(contents.outputs().size(), outCols * lines); i++) {
            slots.add(new Shown(outputX + i % outCols * SLOT_STEP, y + i / outCols * SLOT_STEP, contents.outputs().get(i), false,
                    contents.chances().get(i)));
        }
        int height = lines * SLOT_STEP - 1 + 2 * PREVIEW_PAD;
        return new Preview(slots, top, height, arrowX, y + (lines * SLOT_STEP - 1 - 17) / 2);
    }

    private void drawPreview(GuiGraphics gfx) {
        var preview = preview();
        if (preview.slots().isEmpty()) return;
        int x = this.leftPos;
        int y = this.topPos;
        gfx.fill(x + LEFT, y + preview.top(), x + infoRight, y + preview.top() + preview.height(), GREY);
        for (Shown s : preview.slots()) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, x + s.x(), y + s.y(), 0, 26, 18, 18);
            PortContentIcon.draw(gfx, s.content(), x + s.x(), y + s.y());
        }
        gfx.blit(Ref.UiTextures.SLOT_PARTS, x + preview.arrowX(), y + preview.arrowY(), 26, 0, 24, 17);
    }

    private static int pages(int count, int perPage) {
        return Math.max(1, (count + perPage - 1) / perPage);
    }

    private void advanceCycle(int mouseX, int mouseY) {
        long now = Util.getMillis();
        boolean onRecipe = WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + LEFT, this.topPos + RECIPE_Y, right - LEFT, 18);
        if (onRecipe) {
            nextCycleMs = now + CYCLE_MS;
        } else if (now >= nextCycleMs) {
            cycle++;
            nextCycleMs = now + CYCLE_MS;
        }
    }

    private void drawPageTrack(GuiGraphics gfx, int x, int width, int pages) {
        if (pages <= 1) return;
        int y = this.topPos + RECIPE_Y + 20;
        gfx.fill(x, y, x + width, y + 1, 0xFF333333);
        int current = cycle % pages;
        gfx.fill(x + width * current / pages, y, x + width * (current + 1) / pages, y + 1, 0xFFBBBBBB);
    }

    private List<Row> rows() {
        var rows = new ArrayList<>(List.of(Row.STRUCTURE, Row.TIER, Row.PARALLEL, Row.REDSTONE, Row.MODE));
        if (hasWorkingSound()) {
            rows.add(Row.SOUND);
        }
        if (NetworkLink.AVAILABLE) {
            rows.add(Row.LINK);
        }
        if (be.getRecipeSelectionMode() == RecipeSelectionMode.MANUAL) {
            rows.add(Row.RECIPE);
        }
        return rows;
    }

    private int rowY(Row row) {
        return ROWS_Y + rows().indexOf(row) * ROW_STEP;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        drawBackground(gfx);
        drawPageButton(gfx, mouseX, mouseY);
        drawSizeButton(gfx, mouseX, mouseY);
        drawAssembleButton(gfx, mouseX, mouseY);
        if (onPortsPage()) {
            portList.showInputs(page == 1);
            portList.render(gfx, this.font, be.getLevel());
            return;
        }
        int x = this.leftPos;
        int y = this.topPos;

        gfx.fill(x + LEFT, y + STATUS_Y + 1, x + LEFT + 5, y + STATUS_Y + 6, 0xFF000000 | status().color);
        gfx.fill(x + LEFT, y + STATUS_Y + 11, x + right, y + STATUS_Y + 12, DIVIDER);
        gfx.fill(x + LEFT, y + ROWS_Y - 4, x + right, y + ROWS_Y - 3, DIVIDER);

        if (showsDiagnosis()) {
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
            drawPageTrack(gfx, x + LEFT, maxInputs * SLOT_STEP - 1, row.inputPages());
            drawPageTrack(gfx, x + row.outputX(), row.outputWidth(), row.outputPages());
            int ax = x + row.arrowX();
            int ay = y + RECIPE_Y;
            gfx.blit(Ref.UiTextures.SLOT_PARTS, ax, ay, 26, 0, 24, 17);
            int filled = (int) Math.round(24 * progress());
            gfx.blit(Ref.UiTextures.SLOT_PARTS, ax, ay, 26, 17, filled, 17);
        }

        int by = y + rowY(Row.REDSTONE) - 2;
        drawButtonFrame(gfx, x + VALUE_X - 2, by, valueRight - VALUE_X + 2, BUTTON_HEIGHT, isOnRow(Row.REDSTONE, mouseX, mouseY));
        drawSmallItem(gfx, new ItemStack(Items.REDSTONE), x + VALUE_X, by + 1, 8);

        int my = y + rowY(Row.MODE) - 2;
        drawButtonFrame(gfx, x + VALUE_X - 2, my, valueRight - VALUE_X + 2, BUTTON_HEIGHT, isOnRow(Row.MODE, mouseX, mouseY));

        if (hasWorkingSound()) {
            int sy = y + rowY(Row.SOUND) - 2;
            drawButtonFrame(gfx, x + VALUE_X - 2, sy, valueRight - VALUE_X + 2, BUTTON_HEIGHT, isOnRow(Row.SOUND, mouseX, mouseY));
        }

        if (rows().contains(Row.RECIPE)) {
            int ry = y + rowY(Row.RECIPE) - 2;
            drawButtonFrame(gfx, x + VALUE_X - 2, ry, valueRight - VALUE_X + 2, BUTTON_HEIGHT, isOnRow(Row.RECIPE, mouseX, mouseY));
            var selected = selectedRecipe();
            var icon = selected == null ? null : recipeIcon(selected);
            if (icon != null && icon.kind() == PortContent.Kind.ITEM) {
                drawSmallItem(gfx, icon.item(), x + VALUE_X, ry + 1, 8);
            }
        }

        drawPreview(gfx);

        if (wide) {
            drawProgressBar(gfx);
            int lineX = x + infoRight + COLUMN_GAP / 2;
            gfx.fill(lineX, y + ROWS_Y - 3, lineX + 1, y + panelBottom - 3, DIVIDER);
            portList.render(gfx, this.font, be.getLevel());
            outputList.render(gfx, this.font, be.getLevel());
        }
    }

    private double progress() {
        var state = be.getRecipeState();
        return state == null ? 1 : Math.min(100, state.getTickPercentage()) / 100;
    }

    private int progressY() {
        List<Row> shown = rows();
        return rowY(shown.get(shown.size() - 1)) + ROW_STEP + 8;
    }

    private void drawProgressBar(GuiGraphics gfx) {
        if (shownRecipe() == null) return;
        int bx = this.leftPos + LEFT;
        int by = this.topPos + progressY() + 11;
        int bw = infoRight - LEFT;
        gfx.fill(bx, by, bx + bw, by + 6, 0xFF555555);
        gfx.fill(bx + 1, by + 1, bx + bw - 1, by + 5, 0xFF111111);
        gfx.fill(bx + 1, by + 1, bx + 1 + (int) Math.round((bw - 2) * progress()), by + 5, 0xFF000000 | Status.RUNNING.color);
    }

    private void drawProgressText(GuiGraphics gfx) {
        RecipeModel recipe = shownRecipe();
        if (recipe == null) return;
        int y = progressY();
        drawClipped(gfx, Component.translatable("gui.mm.controller.progress"), LEFT, y, VALUE_X - LEFT - 4, LABEL);
        String elapsed = String.format(Locale.ROOT, "%.1f", recipe.ticks() * progress() / 20);
        String total = String.format(Locale.ROOT, "%.1f", recipe.ticks() / 20.0);
        drawClipped(gfx, Component.translatable("gui.mm.controller.progress.time", elapsed, total), VALUE_X, y, infoRight - VALUE_X, TEXT);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        if (nameBox == null) {
            drawClipped(gfx, be.getName(), LEFT, NAME_Y, nameWidth(), 0xFFFFFF);
        }
        if (onPortsPage()) {
            return;
        }

        Status status = status();
        int statusWidth = right - LEFT - 9;
        if (showsDiagnosis()) {
            var count = Component.translatable("gui.mm.controller.missing", menu.getDiagnosis().total());
            int countWidth = Math.min(this.font.width(count), right - LEFT - 9 - 40);
            drawClipped(gfx, count, right - countWidth, STATUS_Y, countWidth, Status.NOT_FORMED.color);
            statusWidth -= countWidth + 4;
        }
        drawClipped(gfx, Component.translatable(status.key()), LEFT + 9, STATUS_Y, statusWidth, status.color);

        var recipe = recipeRow();
        if (showsDiagnosis()) {
            var first = menu.getDiagnosis().missing().get(0);
            drawClipped(gfx, first.required(), LEFT + 21, RECIPE_Y, right - LEFT - 21, TEXT);
            drawClipped(gfx, offsetText(first.pos()), LEFT + 21, RECIPE_Y + 10, right - LEFT - 21, LABEL);
        } else if (recipe.slots().isEmpty()) {
            gfx.drawString(this.font, Component.translatable("gui.mm.controller.recipe.none"), LEFT, RECIPE_Y + 5, LABEL, false);
        } else {
            int percent = (int) Math.floor(progress() * 100);
            drawClipped(gfx, Component.literal(percent + "%"), recipe.percentX(), RECIPE_Y + 5, right - recipe.percentX(), TEXT);
        }

        for (Row row : rows()) {
            drawClipped(gfx, Component.translatable("gui.mm.controller.row." + row.name().toLowerCase(Locale.ROOT)),
                    LEFT, rowY(row), VALUE_X - LEFT - 4, LABEL);
        }
        var structure = be.getStructure();
        if (structure != null) {
            String name = structure.displayName().getString();
            Matcher tier = TIER.matcher(name);
            boolean hasTier = tier.find();
            String baseName = hasTier ? (name.substring(0, tier.start()) + name.substring(tier.end())).trim() : name;
            drawClipped(gfx, Component.literal(baseName), VALUE_X, rowY(Row.STRUCTURE), valueRight - VALUE_X, TEXT);
            gfx.drawString(this.font, hasTier ? tier.group(1) : "-", VALUE_X, rowY(Row.TIER), hasTier ? TEXT : LABEL, false);
            drawClipped(gfx, Component.translatable("gui.mm.controller.parallel.value", be.getActiveRecipeCount(), be.getDisplayedParallelLimit()),
                    VALUE_X, rowY(Row.PARALLEL), valueRight - VALUE_X, TEXT);
        } else {
            drawClipped(gfx, Component.translatable("gui.mm.controller.not_formed"), VALUE_X, rowY(Row.STRUCTURE), valueRight - VALUE_X, Status.NOT_FORMED.color);
            gfx.drawString(this.font, "-", VALUE_X, rowY(Row.TIER), LABEL, false);
            gfx.drawString(this.font, "-", VALUE_X, rowY(Row.PARALLEL), LABEL, false);
        }
        drawClipped(gfx, Component.translatable("gui.mm.controller.redstone." + redstoneMode()),
                VALUE_X + 12, rowY(Row.REDSTONE), valueRight - VALUE_X - 14, TEXT);
        drawClipped(gfx, Component.translatable("gui.mm.controller.mode." + recipeMode()),
                VALUE_X + 1, rowY(Row.MODE), valueRight - VALUE_X - 3, TEXT);
        if (hasWorkingSound()) {
            drawClipped(gfx, Component.translatable(be.isSoundMuted() ? "gui.mm.controller.sound.off" : "gui.mm.controller.sound.on"),
                    VALUE_X + 1, rowY(Row.SOUND), valueRight - VALUE_X - 3, TEXT);
        }
        if (rows().contains(Row.LINK)) {
            LinkData link = be.getNetworkLink();
            if (link != null) {
                drawClipped(gfx, Component.literal(link.ownerName()), VALUE_X, rowY(Row.LINK), valueRight - VALUE_X, TEXT);
            } else {
                drawClipped(gfx, Component.translatable("gui.mm.controller.link.none"), VALUE_X, rowY(Row.LINK), valueRight - VALUE_X, LABEL);
            }
        }
        if (rows().contains(Row.RECIPE)) {
            drawClipped(gfx, recipeName(selectedRecipe()), VALUE_X + 12, rowY(Row.RECIPE), valueRight - VALUE_X - 14, TEXT);
        }
        if (wide) {
            drawProgressText(gfx);
        }
    }

    private Component lastSentLine() {
        long last = be.getLastLinkExport();
        if (last < 0 || be.getLevel() == null) {
            return Component.translatable("gui.mm.controller.link.never_sent").withStyle(ChatFormatting.GRAY);
        }
        long seconds = Math.max(0, (be.getLevel().getGameTime() - last) / 20);
        String ago = seconds < 60 ? seconds + "s" : seconds < 3600 ? seconds / 60 + "m" : seconds / 3600 + "h";
        return Component.translatable("gui.mm.controller.link.last_sent", ago).withStyle(ChatFormatting.GRAY);
    }

    private boolean hasWorkingSound() {
        return be.getBlockState().getBlock() instanceof MachineControllerBlock block && block.hasWorkingSound();
    }

    private int nameWidth() {
        return assembleBtnX - LEFT - 4;
    }

    private List<RecipeModel> selectableRecipes() {
        if (be.getStructure() == null) {
            return List.of();
        }
        return MachineRecipeManager.getRecipesByStrucutreId(be.getStructure().id()).stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .toList();
    }

    @Nullable
    private RecipeModel selectedRecipe() {
        var id = be.getSelectedRecipeId();
        return id == null ? null : MachineRecipeManager.RECIPES.get(id);
    }

    @Nullable
    private static PortContent recipeIcon(RecipeModel recipe) {
        for (var entry : recipe.outputs().outputs()) {
            for (DisplayedOutput output : entry.displayedOutputs()) {
                PortContent content = output.ingredient().display();
                if (content != null) {
                    return content;
                }
            }
        }
        return null;
    }

    private static Component recipeName(@Nullable RecipeModel recipe) {
        if (recipe == null) {
            return Component.translatable("gui.mm.controller.recipe_none");
        }
        var icon = recipeIcon(recipe);
        if (icon != null && icon.name() != null) {
            return icon.name();
        }
        return Component.literal(recipe.id().getPath());
    }

    private void cycleRecipe(int step) {
        var recipes = selectableRecipes();
        int index = -1;
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).id().equals(be.getSelectedRecipeId())) {
                index = i;
            }
        }
        int next = Math.floorMod(index + 1 + step, recipes.size() + 1) - 1;
        String id = next < 0 ? "" : recipes.get(next).id().toString();
        PacketDistributor.sendToServer(new SelectRecipePkt(be.getBlockPos(), id));
    }

    private boolean showsDiagnosis() {
        return status() == Status.NOT_FORMED && !menu.getDiagnosis().missing().isEmpty();
    }

    private Component offsetText(BlockPos pos) {
        BlockPos delta = pos.subtract(be.getBlockPos());
        var state = be.getBlockState();
        var parts = new ArrayList<Component>();
        addOffset(parts, delta.getY(), "up", "down");
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
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
            int options = Math.min(TOOLTIP_OPTIONS, m.options().size());
            for (int j = 0; j < options; j++) {
                lines.add(Component.literal("   - ").append(m.options().get(j)).withStyle(ChatFormatting.DARK_AQUA));
            }
            if (m.options().size() > options) {
                lines.add(Component.literal("   ").append(Component.translatable("gui.mm.controller.missing.more", m.options().size() - options))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (diagnosis.total() > shown) {
            lines.add(Component.translatable("gui.mm.controller.missing.more", diagnosis.total() - shown).withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.add(Component.translatable("gui.mm.controller.missing.hint").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private void drawClipped(GuiGraphics gfx, Component text, int x, int y, int maxWidth, int color) {
        var clipped = this.font.ellipsize(text, maxWidth);
        gfx.drawString(this.font, Language.getInstance().getVisualOrder(clipped), x, y, color, false);
    }

    private String redstoneMode() {
        return be.getRedstoneModeName().toLowerCase(Locale.ROOT);
    }

    private String recipeMode() {
        return be.getRecipeSelectionMode().serializedName();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        advanceCycle(mouseX, mouseY);
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
            gfx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        if (isOnAssembleButton(mouseX, mouseY)) {
            gfx.renderComponentTooltip(this.font, List.of(Component.translatable(single ? "gui.mm.single.slots" : "gui.mm.controller.assemble")), mouseX, mouseY);
            return;
        }
        if (isOnSizeButton(mouseX, mouseY)) {
            gfx.renderComponentTooltip(this.font, List.of(Component.translatable(bigScreen() ? "gui.mm.controller.size.small" : "gui.mm.controller.size.big")),
                    mouseX, mouseY);
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
        if (tooltip == null && wide) {
            tooltip = portList.tooltip(this.font, be.getLevel(), mouseX, mouseY);
            if (tooltip == null) tooltip = outputList.tooltip(this.font, be.getLevel(), mouseX, mouseY);
        }
        if (tooltip != null) {
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Nullable
    private List<Component> rowTooltip(int mouseX, int mouseY) {
        if (nameBox == null && isOnName(mouseX, mouseY)) {
            var lines = new ArrayList<Component>();
            lines.add(be.getName());
            if (be.getCustomName() != null) {
                lines.add(menu.getModel().displayName().copy().withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable("gui.mm.controller.rename.hint").withStyle(ChatFormatting.YELLOW));
            return lines;
        }
        if (showsDiagnosis() && WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + LEFT, this.topPos + RECIPE_Y - 1, right - LEFT, 20)) {
            return diagnosisTooltip();
        }
        if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, this.leftPos + LEFT, this.topPos + STATUS_Y - 1, right - LEFT, 10)) {
            return List.of(Component.translatable(status().key() + ".hint").withStyle(ChatFormatting.GRAY));
        }
        for (Row row : rows()) {
            if (!isOnRow(row, mouseX, mouseY)) continue;
            String key = "gui.mm.controller.row." + row.name().toLowerCase(Locale.ROOT);
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable(key));
            switch (row) {
                case STRUCTURE -> {
                    if (be.getStructure() != null) {
                        lines.add(be.getStructure().displayName().copy().withStyle(ChatFormatting.WHITE));
                    }
                    lines.add(Component.translatable(be.getStructure() != null ? key + ".hint.formed" : key + ".hint.not_formed").withStyle(ChatFormatting.GRAY));
                }
                case TIER, PARALLEL -> lines.add(Component.translatable(key + ".hint").withStyle(ChatFormatting.GRAY));
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
                        lines.add(lastSentLine());
                        lines.add(Component.translatable("gui.mm.controller.row.link.hint.linked").withStyle(ChatFormatting.GRAY));
                    } else {
                        lines.add(Component.translatable("gui.mm.controller.row.link.hint.none").withStyle(ChatFormatting.GRAY));
                    }
                }
                case RECIPE -> {
                    lines.add(recipeName(selectedRecipe()).copy().withStyle(ChatFormatting.WHITE));
                    lines.add(Component.translatable("gui.mm.controller.row.recipe.hint").withStyle(ChatFormatting.YELLOW));
                }
            }
            return lines;
        }
        return null;
    }

    @Nullable
    private Shown hoveredSlot(double mouseX, double mouseY) {
        if (onPortsPage()) return null;
        for (Shown s : recipeRow().slots()) {
            if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, this.leftPos + s.x(), this.topPos + s.y(), 18, 18)) {
                return s;
            }
        }
        for (Shown s : preview().slots()) {
            if (WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, this.leftPos + s.x(), this.topPos + s.y(), 18, 18)) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    public ControllerPortList.HoveredContent getJeiIngredientAt(double mouseX, double mouseY) {
        Shown shown = hoveredSlot(mouseX, mouseY);
        if (shown != null && (shown.content().kind() == PortContent.Kind.ITEM && !shown.content().item().isEmpty()
                || shown.content().kind() == PortContent.Kind.FLUID && !shown.content().fluid().isEmpty())) {
            return new ControllerPortList.HoveredContent(shown.content(),
                    new Rect2i(this.leftPos + shown.x(), this.topPos + shown.y(), 18, 18));
        }
        if (onPortsPage() || wide) {
            var input = portList.ingredientAt(be.getLevel(), mouseX, mouseY);
            if (input != null) return input;
            if (wide) return outputList.ingredientAt(be.getLevel(), mouseX, mouseY);
        }
        return null;
    }

    private boolean isOnRow(Row row, double mouseX, double mouseY) {
        if (!rows().contains(row)) return false;
        double mx = mouseX - this.leftPos;
        double my = mouseY - this.topPos;
        int y = rowY(row) - 2;
        return mx >= LEFT && mx < infoRight && my >= y && my < y + ROW_STEP;
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (nameBox != null) {
            if (nameBox.isMouseOver(mouseX, mouseY)) {
                return nameBox.mouseClicked(mouseX, mouseY, button);
            }
            finishRename(true);
        }
        if (!onPortsPage() && isOnName(mouseX, mouseY)) {
            startRename();
            return true;
        }
        Shown clicked = hoveredSlot(mouseX, mouseY);
        if (clicked != null && JEI && (button == 0 || button == 1)) {
            return JeiRecipeLookup.show(clicked.content(), button == 1);
        }
        if (isOnAssembleButton(mouseX, mouseY)) {
            playClick();
            if (single) {
                PacketDistributor.sendToServer(new OpenMachineScreenPkt(be.getBlockPos(), false));
            } else {
                Minecraft.getInstance().setScreen(new AssemblyScreen(this, be));
            }
            return true;
        }
        if (isOnSizeButton(mouseX, mouseY)) {
            playClick();
            toggleSize();
            return true;
        }
        if (isOnPageButton(mouseX, mouseY)) {
            page = (page + 1) % PAGE_COUNT;
            playClick();
            return true;
        }
        if (!onPortsPage()) {
            if (isOnRow(Row.MODE, mouseX, mouseY)) {
                var modes = RecipeSelectionMode.values();
                int step = button == 1 ? modes.length - 1 : 1;
                var next = modes[(be.getRecipeSelectionMode().ordinal() + step) % modes.length];
                PacketDistributor.sendToServer(new ControllerSettingsPkt(be.getBlockPos(), ControllerSettingsPkt.Setting.RECIPE_ORDER, next.serializedName()));
                playClick();
                return true;
            }
            if (isOnRow(Row.REDSTONE, mouseX, mouseY)) {
                int next = (be.getRedstoneModeOrdinal() + (button == 1 ? 2 : 1)) % 3;
                PacketDistributor.sendToServer(new ToggleRedstoneModePkt(be.getBlockPos(), next));
                playClick();
                return true;
            }
            if (hasWorkingSound() && isOnRow(Row.SOUND, mouseX, mouseY)) {
                PacketDistributor.sendToServer(new ControllerSettingsPkt(be.getBlockPos(), ControllerSettingsPkt.Setting.SOUND_MUTED, Boolean.toString(!be.isSoundMuted())));
                playClick();
                return true;
            }
            if (isOnRow(Row.RECIPE, mouseX, mouseY)) {
                cycleRecipe(button == 1 ? -1 : 1);
                playClick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOnName(double mouseX, double mouseY) {
        return WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, this.leftPos + LEFT, this.topPos + NAME_Y - 1, nameWidth(), 10);
    }

    private void startRename() {
        nameBox = new EditBox(this.font, this.leftPos + LEFT - 1, this.topPos + NAME_Y - 2, nameWidth() + 2, 12, Component.empty());
        nameBox.setMaxLength(MachineControllerBlockEntity.MAX_NAME_LENGTH);
        nameBox.setValue(be.getName().getString());
        addRenderableWidget(nameBox);
        setFocused(nameBox);
        nameBox.setFocused(true);
    }

    private void finishRename(boolean keep) {
        if (nameBox == null) return;
        if (keep) {
            String typed = nameBox.getValue().strip();
            String name = typed.equals(menu.getModel().displayName().getString()) ? "" : typed;
            PacketDistributor.sendToServer(new ControllerSettingsPkt(be.getBlockPos(), ControllerSettingsPkt.Setting.NAME, name));
        }
        removeWidget(nameBox);
        nameBox = null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if ((onPortsPage() || wide) && portList.mouseScrolled(mouseX, mouseY, scrollY, be.getLevel())) {
            return true;
        }
        if (wide && outputList.mouseScrolled(mouseX, mouseY, scrollY, be.getLevel())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
