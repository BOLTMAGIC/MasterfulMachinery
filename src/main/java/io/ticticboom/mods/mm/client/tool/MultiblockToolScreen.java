package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.client.builder.StructureTierRows;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.structure.GuiStructureRenderer;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.ToolSettingsPkt;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The Multiblock Tool's screen, sized to the window like the controller's big screen. Top: the Structures tab (a
 * searchable structure list on the left, a 3D preview with a layer selector on the right and an info line under it)
 * or the Settings tab (preferred port tiers). Bottom: the tool's 54-slot store with its FE bar, and the player
 * inventory. The two tabs hang on the window's right edge.
 */
public class MultiblockToolScreen extends AbstractContainerScreen<MultiblockToolMenu> {
    private static final int MIN_WIDTH = 378;
    private static final int MIN_HEIGHT = 240;
    private static final int MAX_WIDTH = 520;
    private static final int MAX_HEIGHT = 400;
    private static final int WINDOW_MARGIN = 12;
    // little room above and below: every pixel of height goes to the 3D preview
    private static final int WINDOW_MARGIN_Y = 4;
    private static final int FRAME = 6;
    private static final int FRAME_BOTTOM = 7;
    // content starts 2px inside the frame
    private static final int INSET = FRAME + 2;
    private static final int TOP = 17;
    private static final int GAP = 4;
    private static final int GREY = 0xFFC6C6C6;
    private static final int PANEL = 0xFF1C1C1C;
    private static final int TITLE = 0x404040;
    private static final int TEXT = 0xE0E0E0;
    private static final int LABEL = 0x9A9A9A;
    private static final int ADJUSTED = 0xFFD84D;

    private static final int SLOT = 18;
    private static final int COLUMNS = 9;
    private static final int STORE_ROWS = MultiblockToolMenu.STORE_SLOTS / COLUMNS;
    private static final int GRID_WIDTH = COLUMNS * SLOT;
    private static final int STORE_HEIGHT = STORE_ROWS * SLOT;
    private static final int INVENTORY_HEIGHT = 76;
    private static final int FE_WIDTH = 10;
    private static final int GALLERY_MIN = 100;
    private static final int GALLERY_MAX = 140;
    private static final int SEARCH_HEIGHT = 12;
    private static final int INFO_HEIGHT = 13;
    private static final int ME_HEIGHT = 11;
    private static final int ME_GAP = 2;
    private static final int LAYER_HEIGHT = 11;
    private static final int LAYER_ARROW = 9;
    private static final int TAB = 22;
    private static final int TAB_STEP = 24;

    private enum Tab { STRUCTURES, SETTINGS }

    // remembered while the game runs, like the controller's page
    private static Tab tab = Tab.STRUCTURES;

    private final TierPrefs prefs;
    private final GalleryList gallery;
    private final ToolSettingsTab settings;
    @Nullable
    private StructureModel selected;
    private StructureTierRows tierRows;
    // -1: all layers, else a layer from the bottom
    private int layer = -1;
    private EditBox search;
    private String query = "";

    // set by init() from the screen size, GUI-relative
    private int storeTop;
    private int bandBottom;
    private int inventoryX;
    private int inventoryY;
    private int feX;
    private int galleryWidth;
    private int previewX;
    private int previewWidth;
    private int previewHeight;
    private int infoY;

    public MultiblockToolScreen(MultiblockToolMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        ItemStack tool = tool();
        this.prefs = ToolData.tiers(tool);
        var id = ToolData.structure(tool);
        this.selected = id == null ? null : StructureManager.STRUCTURES.get(id);
        this.tierRows = new StructureTierRows(selected);
        // the screen's own font is only set in init()
        var font = Minecraft.getInstance().font;
        this.gallery = new GalleryList(font, StructureManager.STRUCTURES.values(), selected == null ? null : selected.id(), this::select);
        this.settings = new ToolSettingsTab(font, prefs, this::setTier, ToolData.useMe(tool), ToolData.autoCraft(tool),
                () -> ToolData.network(tool()) != null, this::toggleUseMe, this::toggleAutoCraft, this::forgetNetwork);
        if (selected != null) {
            selected.getGuiRenderer().resetTransforms();
        }
    }

    private ItemStack tool() {
        return Minecraft.getInstance().player.getItemInHand(menu.getHand());
    }

    @Override
    protected void init() {
        this.imageWidth = Mth.clamp(this.width - 2 * WINDOW_MARGIN - TAB, MIN_WIDTH, MAX_WIDTH);
        this.imageHeight = Mth.clamp(this.height - 2 * WINDOW_MARGIN_Y, MIN_HEIGHT, MAX_HEIGHT);
        super.init();
        // centre the window together with the tabs hanging on its right edge
        this.leftPos = (this.width - this.imageWidth - TAB) / 2;

        storeTop = imageHeight - FRAME_BOTTOM - STORE_HEIGHT;
        inventoryX = imageWidth - INSET - GRID_WIDTH;
        inventoryY = imageHeight - FRAME_BOTTOM - INVENTORY_HEIGHT;
        feX = INSET + GRID_WIDTH + GAP;
        // no store / inventory labels (hover an empty store slot for its name): the band ends just above the slots
        bandBottom = storeTop - 3;
        galleryWidth = Mth.clamp((imageWidth - 2 * INSET) * 3 / 10, GALLERY_MIN, GALLERY_MAX);
        previewX = INSET + galleryWidth + GAP;
        previewWidth = imageWidth - INSET - previewX;
        infoY = bandBottom - INFO_HEIGHT;
        previewHeight = infoY - 3 - TOP;
        placeSlots();

        search = new EditBox(this.font, this.leftPos + INSET + 3, this.topPos + TOP + 3, galleryWidth - 6, SEARCH_HEIGHT,
                Component.translatable("gui.mm.tool.search"));
        search.setHint(Component.translatable("gui.mm.tool.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(64);
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            gallery.setQuery(text);
        });
        search.visible = tab == Tab.STRUCTURES;
        addRenderableWidget(search);

        int listTop = TOP + 3 + SEARCH_HEIGHT + 3;
        gallery.setBounds(this.leftPos + INSET + 2, this.topPos + listTop, galleryWidth - 4, bandBottom - 2 - listTop);
        gallery.showSelected();
        settings.setBounds(this.leftPos + INSET + 2, this.topPos + TOP + 2, imageWidth - 2 * INSET - 4, bandBottom - TOP - 4);
    }

    /**
     * Store grid at the bottom left, player inventory at the bottom right, and the off hand (only when the tool is
     * held there) left of the hotbar. Menu slots: 54 store, 27 main, 9 hotbar, then maybe the off hand.
     */
    private void placeSlots() {
        int store = MultiblockToolMenu.STORE_SLOTS;
        for (int i = 0; i < store; i++) {
            menu.moveSlot(i, INSET + 1 + (i % COLUMNS) * SLOT, storeTop + 1 + (i / COLUMNS) * SLOT);
        }
        for (int i = 0; i < 36; i++) {
            int x = inventoryX + 1 + (i % COLUMNS) * SLOT;
            int y = i < 27 ? inventoryY + 1 + (i / COLUMNS) * SLOT : inventoryY + 59;
            menu.moveSlot(store + i, x, y);
        }
        if (menu.slots.size() > store + 36) {
            menu.moveSlot(store + 36, offhandX() + 1, inventoryY + 59);
        }
    }

    private int offhandX() {
        return inventoryX - GAP - SLOT;
    }

    // ---- selection and settings ----

    private void select(StructureModel structure) {
        if (selected != null && selected.id().equals(structure.id())) {
            return;
        }
        selected = structure;
        tierRows = new StructureTierRows(structure);
        layer = -1;
        structure.getGuiRenderer().resetTransforms();
        playClick();
        MMNetwork.INSTANCE.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SELECT_STRUCTURE, structure.id().toString(), 0));
    }

    private void setTier(String key, int tier) {
        playClick();
        MMNetwork.INSTANCE.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SET_TIER, key, tier));
    }

    private void toggleUseMe(boolean value) {
        playClick();
        MMNetwork.INSTANCE.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SET_USE_ME, "", value ? 1 : 0));
    }

    private void toggleAutoCraft(boolean value) {
        playClick();
        MMNetwork.INSTANCE.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SET_AUTOCRAFT, "", value ? 1 : 0));
    }

    private void forgetNetwork() {
        playClick();
        MMNetwork.INSTANCE.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.FORGET_NETWORK, "", 0));
    }

    private void playClick() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void switchTab(Tab next) {
        if (tab == next) {
            return;
        }
        tab = next;
        search.visible = tab == Tab.STRUCTURES;
        if (tab != Tab.STRUCTURES) {
            unfocusSearch();
        }
        playClick();
    }

    private void unfocusSearch() {
        search.setFocused(false);
        if (getFocused() == search) {
            setFocused(null);
        }
    }

    // ---- drawing ----

    @Override
    protected void renderBg(@NotNull GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        var texture = Ref.UiTextures.GUI_LARGE;
        // MM's large GUI frame at this size, with a plain grey middle (as the controller's big screen)
        gfx.blitNineSlicedSized(texture, x, y, imageWidth, imageHeight, FRAME, FRAME, FRAME, FRAME_BOTTOM, 174, 222, 0, 0, 256, 256);
        gfx.fill(x + FRAME, y + FRAME, x + imageWidth - FRAME, y + imageHeight - FRAME_BOTTOM, GREY);
        drawTabs(gfx, mouseX, mouseY);

        if (tab == Tab.STRUCTURES) {
            drawPanel(gfx, x + INSET, y + TOP, galleryWidth, bandBottom - TOP);
            gallery.render(gfx, mouseX, mouseY);
            drawPanel(gfx, x + previewX, y + TOP, previewWidth, previewHeight);
            drawPreview(gfx, mouseX, mouseY);
            drawPanel(gfx, x + previewX, y + infoY, previewWidth, INFO_HEIGHT);
            drawInfo(gfx);
        } else {
            drawPanel(gfx, x + INSET, y + TOP, imageWidth - 2 * INSET, bandBottom - TOP);
            settings.render(gfx, mouseX, mouseY);
        }
        drawMeStatus(gfx);

        // store slots, then the FE bar beside them
        for (int i = 0; i < MultiblockToolMenu.STORE_SLOTS; i++) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, x + INSET + (i % COLUMNS) * SLOT, y + storeTop + (i / COLUMNS) * SLOT, 0, 26, SLOT, SLOT);
        }
        drawEnergy(gfx);
        // the player inventory, as drawn in MM's texture
        gfx.blit(texture, x + inventoryX, y + inventoryY, 6, 139, GRID_WIDTH, INVENTORY_HEIGHT);
        if (menu.slots.size() > MultiblockToolMenu.STORE_SLOTS + 36) {
            gfx.blit(Ref.UiTextures.SLOT_PARTS, x + offhandX(), y + inventoryY + 58, 0, 26, SLOT, SLOT);
        }
    }

    /** A dark screen panel: its edges from MM's texture, a plain middle. */
    private static void drawPanel(GuiGraphics gfx, int x, int y, int width, int height) {
        gfx.blitNineSlicedSized(Ref.UiTextures.GUI_LARGE, x, y, width, height, 2, 2, 2, 2, 162, 121, 6, 6, 256, 256);
        gfx.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL);
    }

    private void drawTabs(GuiGraphics gfx, int mouseX, int mouseY) {
        for (Tab each : Tab.values()) {
            Rect2i area = tabArea(each);
            boolean active = each == tab;
            var texture = active || area.contains(mouseX, mouseY) ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
            gfx.blitNineSlicedSized(texture, area.getX(), area.getY(), area.getWidth(), area.getHeight(), 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
            ItemStack icon = each == Tab.STRUCTURES ? new ItemStack(MMRegisters.BLUEPRINT.get()) : new ItemStack(Items.COMPARATOR);
            gfx.renderItem(icon, area.getX() + area.getWidth() - TAB + 3, area.getY() + 3);
        }
    }

    /** The active tab reaches a little further into the window, so it looks attached. */
    private Rect2i tabArea(Tab each) {
        int x = this.leftPos + imageWidth - (each == tab ? 3 : 1);
        int width = TAB + (each == tab ? 2 : 0);
        return new Rect2i(x, this.topPos + TOP + each.ordinal() * TAB_STEP, width, TAB);
    }

    /** For JEI: the tabs lie outside the window's own area. */
    public List<Rect2i> getTabAreas() {
        var areas = new ArrayList<Rect2i>();
        for (Tab each : Tab.values()) {
            areas.add(tabArea(each));
        }
        return areas;
    }

    @Nullable
    private Tab tabAt(double mouseX, double mouseY) {
        for (Tab each : Tab.values()) {
            if (tabArea(each).contains((int) mouseX, (int) mouseY)) {
                return each;
            }
        }
        return null;
    }

    /** The 3D view's area on screen, inside the preview panel. */
    private GuiPos viewport() {
        return GuiPos.of(this.leftPos + previewX + 2, this.topPos + TOP + 2, previewWidth - 4, previewHeight - 4);
    }

    private int layerCount() {
        if (selected == null) {
            return 0;
        }
        GuiStructureRenderer renderer = selected.getGuiRenderer();
        renderer.init();
        return renderer.getStructureSize().y + 1;
    }

    private boolean isOnLayerBar(double mouseX, double mouseY) {
        if (layerCount() < 2) {
            return false;
        }
        GuiPos view = viewport();
        return mouseX >= view.x() && mouseX < view.x() + view.w() && mouseY >= view.y() + view.h() - LAYER_HEIGHT && mouseY < view.y() + view.h();
    }

    private boolean isOnView(double mouseX, double mouseY) {
        GuiPos view = viewport();
        return mouseX >= view.x() && mouseX < view.x() + view.w() && mouseY >= view.y() && mouseY < view.y() + view.h()
                && !isOnLayerBar(mouseX, mouseY);
    }

    /** The structure the same way as the blueprint screen and JEI draw it; this view's layer is applied every frame. */
    private void drawPreview(GuiGraphics gfx, int mouseX, int mouseY) {
        GuiPos view = viewport();
        if (selected == null) {
            Component hint = Component.translatable("gui.mm.tool.preview.none");
            int width = this.font.width(hint);
            gfx.drawString(this.font, hint, view.x() + (view.w() - width) / 2, view.y() + view.h() / 2 - 4, LABEL, false);
            return;
        }
        GuiStructureRenderer renderer = selected.getGuiRenderer();
        gfx.pose().pushPose();
        gfx.pose().setIdentity();
        renderer.setViewport(view);
        renderer.init();
        renderer.setYSlice(layer >= 0, renderer.getMinBound().y() + layer);
        renderer.render(gfx, mouseX, mouseY, isOnView(mouseX, mouseY));
        gfx.pose().popPose();
        drawLayerBar(gfx, mouseX, mouseY);
    }

    /** "&lt; All layers &gt;" over the bottom of the view, like the JEI structure view's selector. */
    private void drawLayerBar(GuiGraphics gfx, int mouseX, int mouseY) {
        int count = layerCount();
        if (count < 2) {
            return;
        }
        GuiPos view = viewport();
        int x = view.x();
        int y = view.y() + view.h() - LAYER_HEIGHT;
        int width = view.w();
        gfx.fill(x, y, x + width, y + LAYER_HEIGHT, 0x80000000);
        boolean hovered = isOnLayerBar(mouseX, mouseY);
        gfx.drawString(this.font, "<", x + 2, y + 2, hovered && mouseX < x + LAYER_ARROW ? 0xFFFFFF55 : 0xFFA0A0A0, false);
        gfx.drawString(this.font, ">", x + width - LAYER_ARROW + 2, y + 2, hovered && mouseX >= x + width - LAYER_ARROW ? 0xFFFFFF55 : 0xFFA0A0A0, false);
        Component label = layer < 0 ? Component.translatable("jei.mm.structure.layer.all") : Component.translatable("jei.mm.structure.layer", layer + 1, count);
        String text = this.font.plainSubstrByWidth(label.getString(), width - 2 * LAYER_ARROW);
        gfx.drawString(this.font, text, x + (width - this.font.width(text)) / 2, y + 2, 0xFFFFFFFF, false);
    }

    private void stepLayer(int direction) {
        // all, 0, 1, ... count-1, all
        int states = layerCount() + 1;
        layer = Math.floorMod(layer + 1 + direction, states) - 1;
        playClick();
    }

    /**
     * One line: name, size and block count, then the port blocks this structure will get, '*' where the preference is
     * not taken. Cut short when too long; the tooltip has it all.
     */
    private void drawInfo(GuiGraphics gfx) {
        int x = this.leftPos + previewX + 4;
        int y = this.topPos + infoY + 3;
        int width = previewWidth - 8;
        if (selected == null) {
            drawClipped(gfx, Component.translatable("tooltip.mm.multiblock_tool.no_structure"), x, y, width, LABEL);
            return;
        }
        MutableComponent line = infoLine().copy().withStyle(Style.EMPTY.withColor(TEXT))
                .append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(LABEL)))
                .append(portsLine());
        drawClipped(gfx, line, x, y, width, TEXT);
    }

    private Component infoLine() {
        GuiStructureRenderer renderer = selected.getGuiRenderer();
        renderer.init();
        Vector3i size = renderer.getStructureSize();
        // positioned pieces plus the controller
        int blocks = selected.layout().getPositionedPieces().size() + 1;
        return Component.translatable("gui.mm.tool.info", selected.name(), size.x + 1, size.y + 1, size.z + 1, blocks);
    }

    private Component portsLine() {
        if (tierRows.isEmpty()) {
            return Component.translatable("gui.mm.tool.ports.fixed").withStyle(Style.EMPTY.withColor(LABEL));
        }
        MutableComponent line = Component.translatable("gui.mm.tool.ports").withStyle(Style.EMPTY.withColor(LABEL));
        boolean first = true;
        for (String key : tierRows.rows().keySet()) {
            if (!first) {
                line.append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(LABEL)));
            }
            first = false;
            line.append(portPart(key));
        }
        return line;
    }

    private Component portPart(String key) {
        int preferred = prefs.get(key);
        Block block = tierRows.chosenBlock(key, preferred);
        MutableComponent part = (block == null ? Component.literal("?") : block.getName().copy());
        if (tierRows.adjusted(key, preferred)) {
            return part.append("*").withStyle(Style.EMPTY.withColor(ADJUSTED));
        }
        return part.withStyle(Style.EMPTY.withColor(TEXT));
    }

    private boolean isOnPortsLine(double mouseX, double mouseY) {
        int x = this.leftPos + previewX;
        int y = this.topPos + infoY;
        return tab == Tab.STRUCTURES && selected != null && mouseX >= x && mouseX < x + previewWidth && mouseY >= y && mouseY < y + INFO_HEIGHT;
    }

    private List<Component> portsTooltip() {
        var lines = new ArrayList<Component>();
        lines.add(infoLine());
        boolean anyAdjusted = false;
        for (String key : tierRows.rows().keySet()) {
            lines.add(portPart(key));
            anyAdjusted |= tierRows.adjusted(key, prefs.get(key));
        }
        if (anyAdjusted) {
            lines.add(Component.translatable("gui.mm.assemble.adjusted").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private int energyStored() {
        return new ToolEnergy(tool(), MMConfigSetup.COMMON.toolEnergyCapacity.get()).getEnergyStored();
    }

    private void drawEnergy(GuiGraphics gfx) {
        int x = this.leftPos + feX;
        int y = this.topPos + storeTop;
        gfx.fill(x, y, x + FE_WIDTH, y + STORE_HEIGHT, 0xFF373737);
        gfx.fill(x + 1, y + 1, x + FE_WIDTH - 1, y + STORE_HEIGHT - 1, PANEL);
        int capacity = MMConfigSetup.COMMON.toolEnergyCapacity.get();
        int inner = STORE_HEIGHT - 2;
        int filled = capacity <= 0 ? 0 : (int) Math.round((double) inner * energyStored() / capacity);
        if (filled > 0) {
            gfx.fillGradient(x + 1, y + 1 + inner - filled, x + FE_WIDTH - 1, y + STORE_HEIGHT - 1, 0xFFFF5555, 0xFFAA0000);
        }
    }

    private boolean isOnEnergy(double mouseX, double mouseY) {
        int x = this.leftPos + feX;
        int y = this.topPos + storeTop;
        return mouseX >= x && mouseX < x + FE_WIDTH && mouseY >= y && mouseY < y + STORE_HEIGHT;
    }

    /** The tool's bound ME network, or that it isn't bound. */
    private Component meStatus() {
        var network = ToolData.network(tool());
        return network == null ? Component.translatable("gui.mm.tool.me.not_bound")
                : Component.translatable("gui.mm.tool.me.bound", network.pos().toShortString(), network.dimension().location().getPath());
    }

    /**
     * The store has more rows than the player inventory, so the inventory's own column has empty space above it
     * (between the tab panel and the inventory grid); the ME status line lives there, clipped to that width, with
     * the full text in a tooltip. This never touches {@link #bandBottom} / the 3D preview height.
     */
    private int meStatusTop() {
        return inventoryY - ME_GAP - ME_HEIGHT;
    }

    private void drawMeStatus(GuiGraphics gfx) {
        // ME networks only exist with AE2
        if (!NetworkLink.AVAILABLE) {
            return;
        }
        int x = this.leftPos + inventoryX;
        int y = this.topPos + meStatusTop() + 1;
        drawClipped(gfx, meStatus(), x, y, GRID_WIDTH, LABEL);
    }

    private boolean isOnMeStatus(double mouseX, double mouseY) {
        if (!NetworkLink.AVAILABLE) {
            return false;
        }
        int x = this.leftPos + inventoryX;
        int y = this.topPos + meStatusTop();
        return mouseX >= x && mouseX < x + GRID_WIDTH && mouseY >= y && mouseY < y + ME_HEIGHT;
    }

    private void drawClipped(GuiGraphics gfx, Component text, int x, int y, int maxWidth, int color) {
        TextRenderUtil.drawClipped(gfx, this.font, text, x, y, maxWidth, color);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gfx, int mouseX, int mouseY) {
        drawClipped(gfx, this.title, INSET, 6, imageWidth - 2 * INSET, TITLE);
    }

    /** Store slots hold up to 512: counts above two digits are drawn compact, like the item port's screen. */
    @Override
    protected void renderSlot(@NotNull GuiGraphics gfx, @NotNull Slot slot) {
        ItemStack stack = slot.getItem();
        if (!(slot instanceof ToolSlot) || stack.getCount() < 100) {
            super.renderSlot(gfx, slot);
            return;
        }
        gfx.renderItem(stack, slot.x, slot.y, slot.x + slot.y * this.imageWidth);
        gfx.renderItemDecorations(this.font, stack, slot.x, slot.y, "");
        CountFormat.drawSlotCount(gfx, slot.x - 1, slot.y - 1, stack.getCount());
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }
        List<Component> tooltip = null;
        Tab hoveredTab = tabAt(mouseX, mouseY);
        if (this.hoveredSlot instanceof ToolSlot && this.menu.getCarried().isEmpty()) {
            // the store has no label on screen
            tooltip = List.of(Component.translatable("gui.mm.tool.store"));
        } else if (hoveredTab != null) {
            tooltip = List.of(Component.translatable(hoveredTab == Tab.STRUCTURES ? "gui.mm.tool.tab.structures" : "gui.mm.tool.tab.settings"));
        } else if (isOnEnergy(mouseX, mouseY)) {
            tooltip = List.of(Component.translatable("tooltip.mm.multiblock_tool.energy",
                    CountFormat.grouped(energyStored()), CountFormat.grouped(MMConfigSetup.COMMON.toolEnergyCapacity.get())));
        } else if (isOnMeStatus(mouseX, mouseY)) {
            tooltip = List.of(meStatus());
        } else if (tab == Tab.SETTINGS) {
            tooltip = settings.tooltip(mouseX, mouseY);
        } else if (isOnLayerBar(mouseX, mouseY)) {
            tooltip = List.of(Component.translatable("jei.mm.structure.layer.hint"));
        } else if (isOnPortsLine(mouseX, mouseY)) {
            tooltip = portsTooltip();
        } else if (!search.isMouseOver(mouseX, mouseY)) {
            tooltip = gallery.tooltip(mouseX, mouseY);
        }
        if (tooltip != null) {
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!search.isMouseOver(mouseX, mouseY)) {
            unfocusSearch();
        }
        Tab clickedTab = tabAt(mouseX, mouseY);
        if (clickedTab != null) {
            switchTab(clickedTab);
            return true;
        }
        if (tab == Tab.SETTINGS) {
            if (settings.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        } else {
            if (button == 0 && isOnLayerBar(mouseX, mouseY)) {
                GuiPos view = viewport();
                if (mouseX < view.x() + LAYER_ARROW) {
                    stepLayer(-1);
                } else if (mouseX >= view.x() + view.w() - LAYER_ARROW) {
                    stepLayer(1);
                }
                return true;
            }
            if (gallery.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        // the tabs are outside the window, but clicking them must not drop the carried stack
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton) && tabAt(mouseX, mouseY) == null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == Tab.SETTINGS) {
            if (settings.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        } else {
            if (isOnLayerBar(mouseX, mouseY)) {
                // scrolling up moves up the structure
                stepLayer(delta > 0 ? 1 : -1);
                return true;
            }
            if (selected != null && isOnView(mouseX, mouseY)) {
                selected.getGuiRenderer().zoom(delta);
                return true;
            }
            if (gallery.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // while typing a search, keys go to the search box (so E or number keys don't act on the inventory)
        if (search.visible && search.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                unfocusSearch();
                return true;
            }
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
