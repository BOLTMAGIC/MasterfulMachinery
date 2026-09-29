package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureRegistry;
import io.ticticboom.mods.mm.client.builder.StructureTierRows;
import io.ticticboom.mods.mm.client.config.MMConfigScreen;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.structure.GuiStructureRenderer;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.net.MMNetwork;
import io.ticticboom.mods.mm.net.packet.ToolSettingsPkt;
import io.ticticboom.mods.mm.net.packet.StructureCategoryEditPkt;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolSlot;
import io.ticticboom.mods.mm.tool.StructureCategories;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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

    private enum Tab {
        STRUCTURES("structures"), MATERIALS("materials"), SETTINGS("settings"), CONFIG("config"), ADMIN("admin");

        final ResourceLocation icon;

        Tab(String iconName) {
            this.icon = Ref.id("textures/gui/tool_tabs/" + iconName + ".png");
        }
    }

    // remembered while the game runs, like the controller's page
    private static Tab tab = Tab.STRUCTURES;

    private final TierPrefs prefs;
    private final GalleryList gallery;
    private final ToolMaterialsTab materials;
    private final ToolMaterialsTab compactMaterials;
    private final ToolSettingsTab settings;
    @Nullable
    private StructureModel selected;
    // or another mod's structure (at most one of the two is set), with its own preview renderer
    @Nullable
    private BuildableStructure selectedBuilder;
    @Nullable
    private GuiStructureRenderer builderRenderer;
    private StructureTierRows tierRows;
    // -1: all layers, else a layer from the bottom
    private int layer = -1;
    private EditBox search;
    private EditBox categoryNameBox;
    private String query = "";
    private String adminSelectedCategory = StructureCategories.DEFAULT;
    private int adminScroll;
    @Nullable
    private GalleryList.Entry categoryPopupTarget;
    private int categoryPopupX;
    private int categoryPopupY;
    private int categoryPopupScroll;
    private int lastClickX;
    private int lastClickY;

    // set by init() from the screen size, GUI-relative
    private int storeTop;
    private int bandBottom;
    private int inventoryX;
    private int inventoryY;
    private int feX;
    private int compactMaterialsX;
    private int compactMaterialsWidth;
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
        var builderId = ToolData.builderStructure(tool);
        this.selectedBuilder = builderId == null ? null : BuildableStructureRegistry.CLIENT.get(builderId);
        this.builderRenderer = selectedBuilder == null ? null : GuiStructureRenderer.ofBlocks(selectedBuilder.blocks());
        this.tierRows = new StructureTierRows(selected);
        // the screen's own font is only set in init()
        var font = Minecraft.getInstance().font;
        var entries = new ArrayList<GalleryList.Entry>();
        for (StructureModel model : StructureManager.STRUCTURES.values()) {
            entries.add(GalleryList.Entry.of(model));
        }
        for (BuildableStructure structure : BuildableStructureRegistry.CLIENT.all()) {
            entries.add(GalleryList.Entry.of(structure));
        }
        ResourceLocation selectedId = selected != null ? selected.id() : selectedBuilder != null ? selectedBuilder.id() : null;
        this.gallery = new GalleryList(font, entries, selectedId, selectedBuilder != null, this::select, this::openCategoryPopup);
        this.materials = new ToolMaterialsTab(font, menu, prefs, false);
        this.compactMaterials = new ToolMaterialsTab(font, menu, prefs, true);
        this.settings = new ToolSettingsTab(font, prefs, this::setTier, ToolData.useMe(tool), ToolData.autoCraft(tool),
                ToolData.instantBuild(tool), () -> ToolData.network(tool()) != null, this::toggleUseMe,
                this::toggleAutoCraft, this::toggleInstantBuild, this::forgetNetwork);
        GuiStructureRenderer renderer = renderer();
        if (renderer != null) {
            renderer.resetTransforms();
        }
    }

    /** The selected structure's 3D preview, MM or another mod's; null when nothing is selected. */
    @Nullable
    private GuiStructureRenderer renderer() {
        return selected != null ? selected.getGuiRenderer() : builderRenderer;
    }

    private boolean hasSelection() {
        return selected != null || selectedBuilder != null;
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
        compactMaterialsX = feX + FE_WIDTH + GAP;
        compactMaterialsWidth = inventoryX - GAP - compactMaterialsX;
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

        int adminX = this.leftPos + INSET + 4;
        int adminY = this.topPos + TOP + 27;
        int adminWidth = imageWidth - 2 * INSET - 8;
        categoryNameBox = new EditBox(this.font, adminX, adminY, Math.max(60, adminWidth - 152), 12,
                Component.translatable("gui.mm.tool.admin.name"));
        categoryNameBox.setHint(Component.translatable("gui.mm.tool.admin.name").withStyle(ChatFormatting.DARK_GRAY));
        categoryNameBox.setMaxLength(32);
        categoryNameBox.visible = tab == Tab.ADMIN && canManageCategories();
        addRenderableWidget(categoryNameBox);

        int listTop = TOP + 3 + SEARCH_HEIGHT + 3;
        gallery.setBounds(this.leftPos + INSET + 2, this.topPos + listTop, galleryWidth - 4, bandBottom - 2 - listTop);
        gallery.showSelected();
        materials.setBounds(this.leftPos + INSET + 2, this.topPos + TOP + 2, imageWidth - 2 * INSET - 4, bandBottom - TOP - 4);
        compactMaterials.setBounds(this.leftPos + compactMaterialsX + 2, this.topPos + storeTop + 2,
                compactMaterialsWidth - 4, STORE_HEIGHT - 4);
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

    private void select(GalleryList.Entry entry) {
        if (entry.structure() instanceof BuildableStructure builder) {
            if (selectedBuilder != null && selectedBuilder.id().equals(builder.id())) {
                return;
            }
            selected = null;
            selectedBuilder = builder;
            builderRenderer = GuiStructureRenderer.ofBlocks(builder.blocks());
            tierRows = new StructureTierRows(null);
        } else {
            StructureModel structure = (StructureModel) entry.structure();
            if (selected != null && selected.id().equals(structure.id())) {
                return;
            }
            selected = structure;
            selectedBuilder = null;
            builderRenderer = null;
            tierRows = new StructureTierRows(structure);
        }
        layer = -1;
        //noinspection DataFlowIssue - a structure was just selected
        renderer().resetTransforms();
        playClick();
        MMNetwork.INSTANCE.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SELECT_STRUCTURE, entry.id().toString(),
                entry.builder() ? ToolSettingsPkt.BUILDER_STRUCTURE : 0));
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

    private void toggleInstantBuild(boolean value) {
        playClick();
        MMNetwork.INSTANCE.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.SET_INSTANT_BUILD, "", value ? 1 : 0));
    }

    private void forgetNetwork() {
        playClick();
        MMNetwork.INSTANCE.sendToServer(new ToolSettingsPkt(ToolSettingsPkt.Action.FORGET_NETWORK, "", 0));
    }

    private void playClick() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void switchTab(Tab next) {
        if (next == Tab.ADMIN && !canManageCategories()) return;
        if (next == Tab.CONFIG) {
            this.minecraft.setScreen(new MMConfigScreen(null));
            return;
        }
        if (tab == next) {
            return;
        }
        categoryPopupTarget = null;
        tab = next;
        search.visible = tab == Tab.STRUCTURES;
        categoryNameBox.visible = tab == Tab.ADMIN;
        if (tab != Tab.ADMIN) unfocusCategoryName();
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

    private void unfocusCategoryName() {
        categoryNameBox.setFocused(false);
        if (getFocused() == categoryNameBox) {
            setFocused(null);
        }
    }

    // ---- drawing ----

    @Override
    protected void renderBg(@NotNull GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        if (tab == Tab.ADMIN && !canManageCategories()) switchTab(Tab.STRUCTURES);
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
            if (compactMaterialsVisible()) {
                drawPanel(gfx, x + compactMaterialsX, y + storeTop, compactMaterialsWidth, STORE_HEIGHT);
                compactMaterials.render(gfx, selected, selectedBuilder);
            }
        } else if (tab == Tab.MATERIALS) {
            drawPanel(gfx, x + INSET, y + TOP, imageWidth - 2 * INSET, bandBottom - TOP);
            materials.render(gfx, selected, selectedBuilder);
        } else if (tab == Tab.ADMIN) {
            drawPanel(gfx, x + INSET, y + TOP, imageWidth - 2 * INSET, bandBottom - TOP);
            drawCategoryAdmin(gfx, mouseX, mouseY);
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
        if (tab == Tab.STRUCTURES && categoryPopupTarget != null) drawCategoryPopup(gfx, mouseX, mouseY);
    }

    /** A dark screen panel: its edges from MM's texture, a plain middle. */
    private static void drawPanel(GuiGraphics gfx, int x, int y, int width, int height) {
        gfx.blitNineSlicedSized(Ref.UiTextures.GUI_LARGE, x, y, width, height, 2, 2, 2, 2, 162, 121, 6, 6, 256, 256);
        gfx.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL);
    }

    private boolean canManageCategories() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasPermissions(2);
    }

    @Nullable
    private String selectedStructureKey() {
        if (selected != null) return StructureCategories.key(selected.id(), false);
        if (selectedBuilder != null) return StructureCategories.key(selectedBuilder.id(), true);
        return null;
    }

    private int adminX() { return leftPos + INSET + 4; }
    private int adminWidth() { return imageWidth - 2 * INSET - 8; }
    private int adminButtonY() { return topPos + TOP + 27; }
    private int adminListTop() { return topPos + TOP + 46; }
    private int adminListBottom() { return topPos + bandBottom - 4; }

    private void drawCategoryAdmin(GuiGraphics gfx, int mouseX, int mouseY) {
        int x = adminX();
        int width = adminWidth();
        var snapshot = StructureCategories.clientSnapshot();
        if (!adminSelectedCategory.equals(StructureCategories.DEFAULT) &&
                !snapshot.categories().contains(adminSelectedCategory)) adminSelectedCategory = StructureCategories.DEFAULT;
        drawClipped(gfx, Component.translatable("gui.mm.tool.admin.title"), x, topPos + TOP + 4, width, TEXT);
        String key = selectedStructureKey();
        Component selection = key == null ? Component.translatable("gui.mm.tool.admin.select_structure")
                : Component.translatable("gui.mm.tool.admin.current", snapshot.assignments().getOrDefault(key, StructureCategories.DEFAULT));
        drawClipped(gfx, selection, x, topPos + TOP + 15, width, LABEL);
        drawAdminButton(gfx, x + width - 146, adminButtonY(), 42, "gui.mm.tool.admin.add", mouseX, mouseY);
        drawAdminButton(gfx, x + width - 100, adminButtonY(), 54, "gui.mm.tool.admin.rename", mouseX, mouseY);
        drawAdminButton(gfx, x + width - 42, adminButtonY(), 42, "gui.mm.tool.admin.delete", mouseX, mouseY);

        List<String> categories = adminCategories();
        int listTop = adminListTop();
        int listBottom = adminListBottom();
        int maxScroll = Math.max(0, categories.size() * 12 - (listBottom - listTop));
        adminScroll = Mth.clamp(adminScroll, 0, maxScroll);
        gfx.enableScissor(x, listTop, x + width, listBottom);
        for (int i = 0; i < categories.size(); i++) {
            int rowY = listTop + i * 12 - adminScroll;
            if (rowY + 12 <= listTop || rowY >= listBottom) continue;
            String category = categories.get(i);
            if (category.equals(adminSelectedCategory)) gfx.fill(x, rowY, x + width, rowY + 11, 0xFF3A4A6A);
            boolean assigned = key != null && snapshot.assignments().getOrDefault(key, StructureCategories.DEFAULT).equals(category);
            drawClipped(gfx, Component.literal((assigned ? "✓ " : "  ") + category), x + 3, rowY + 2, width - 6,
                    assigned ? 0xFF55D76A : TEXT);
        }
        gfx.disableScissor();
    }

    private List<String> adminCategories() {
        List<String> result = new ArrayList<>();
        result.add(StructureCategories.DEFAULT);
        result.addAll(StructureCategories.clientSnapshot().categories());
        return result;
    }

    private void openCategoryPopup(GalleryList.Entry entry) {
        if (!canManageCategories()) return;
        categoryPopupTarget = entry;
        categoryPopupScroll = 0;
        int popupWidth = 140;
        int popupHeight = Math.min(8, adminCategories().size()) * 12 + 6;
        categoryPopupX = Mth.clamp(lastClickX + 5, leftPos + INSET, leftPos + imageWidth - INSET - popupWidth);
        categoryPopupY = Mth.clamp(lastClickY, topPos + TOP, topPos + bandBottom - popupHeight);
        playClick();
    }

    private int popupHeight() { return Math.min(8, adminCategories().size()) * 12 + 6; }

    private boolean isOnCategoryPopup(double mouseX, double mouseY) {
        return categoryPopupTarget != null && mouseX >= categoryPopupX && mouseX < categoryPopupX + 140
                && mouseY >= categoryPopupY && mouseY < categoryPopupY + popupHeight();
    }

    private void drawCategoryPopup(GuiGraphics gfx, int mouseX, int mouseY) {
        List<String> categories = adminCategories();
        int visible = Math.min(8, categories.size());
        int x = categoryPopupX;
        int y = categoryPopupY;
        drawPanel(gfx, x, y, 140, visible * 12 + 6);
        gfx.enableScissor(x + 3, y + 3, x + 137, y + 3 + visible * 12);
        for (int row = 0; row < visible; row++) {
            int index = row + categoryPopupScroll;
            if (index >= categories.size()) break;
            int rowY = y + 3 + row * 12;
            if (mouseX >= x + 3 && mouseX < x + 137 && mouseY >= rowY && mouseY < rowY + 12) {
                gfx.fill(x + 3, rowY, x + 137, rowY + 12, 0xFF3A4A6A);
            }
            drawClipped(gfx, Component.literal(categories.get(index)), x + 6, rowY + 2, 126, TEXT);
        }
        gfx.disableScissor();
        if (categories.size() > visible) {
            int trackHeight = visible * 12;
            int thumb = Math.max(8, trackHeight * visible / categories.size());
            int thumbY = y + 3 + (trackHeight - thumb) * categoryPopupScroll / (categories.size() - visible);
            gfx.fill(x + 135, y + 3, x + 137, y + 3 + trackHeight, 0xFF383838);
            gfx.fill(x + 135, thumbY, x + 137, thumbY + thumb, 0xFFB0B0B0);
        }
    }

    private void drawAdminButton(GuiGraphics gfx, int x, int y, int width, String key, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 12;
        gfx.blitNineSlicedSized(hovered ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE,
                x, y, width, 12, 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
        drawClipped(gfx, Component.translatable(key), x + 3, y + 2, width - 6, TEXT);
    }

    private boolean adminMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !canManageCategories()) return false;
        int x = adminX();
        int width = adminWidth();
        if (mouseY >= adminButtonY() && mouseY < adminButtonY() + 12) {
            String name = categoryNameBox.getValue().strip();
            if (mouseX >= x + width - 146 && mouseX < x + width - 104) {
                MMNetwork.INSTANCE.sendToServer(new StructureCategoryEditPkt(StructureCategories.Action.CREATE, "", name));
                return true;
            }
            if (mouseX >= x + width - 100 && mouseX < x + width - 46) {
                if (!adminSelectedCategory.equals(StructureCategories.DEFAULT)) {
                    MMNetwork.INSTANCE.sendToServer(new StructureCategoryEditPkt(StructureCategories.Action.RENAME, adminSelectedCategory, name));
                }
                return true;
            }
            if (mouseX >= x + width - 42 && mouseX < x + width) {
                if (!adminSelectedCategory.equals(StructureCategories.DEFAULT)) {
                    MMNetwork.INSTANCE.sendToServer(new StructureCategoryEditPkt(StructureCategories.Action.DELETE, adminSelectedCategory, ""));
                }
                return true;
            }
        }
        if (mouseX >= x && mouseX < x + width && mouseY >= adminListTop() && mouseY < adminListBottom()) {
            int index = ((int) mouseY - adminListTop() + adminScroll) / 12;
            List<String> categories = adminCategories();
            if (index < 0 || index >= categories.size()) return true;
            adminSelectedCategory = categories.get(index);
            categoryNameBox.setValue(adminSelectedCategory.equals(StructureCategories.DEFAULT) ? "" : adminSelectedCategory);
            String key = selectedStructureKey();
            if (key != null) MMNetwork.INSTANCE.sendToServer(new StructureCategoryEditPkt(
                    StructureCategories.Action.ASSIGN, key, adminSelectedCategory));
            return true;
        }
        return false;
    }

    private boolean adminMouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < adminX() || mouseX >= adminX() + adminWidth() ||
                mouseY < adminListTop() || mouseY >= adminListBottom()) return false;
        int max = Math.max(0, adminCategories().size() * 12 - (adminListBottom() - adminListTop()));
        adminScroll = Mth.clamp(adminScroll - (int) Math.signum(delta) * 24, 0, max);
        return true;
    }

    private void drawTabs(GuiGraphics gfx, int mouseX, int mouseY) {
        for (Tab each : Tab.values()) {
            if (each == Tab.ADMIN && !canManageCategories()) continue;
            Rect2i area = tabArea(each);
            boolean active = each == tab;
            var texture = active || area.contains(mouseX, mouseY) ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
            gfx.blitNineSlicedSized(texture, area.getX(), area.getY(), area.getWidth(), area.getHeight(), 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
            gfx.blit(each.icon, area.getX() + area.getWidth() - TAB + 3, area.getY() + 3,
                    0, 0, 16, 16, 16, 16);
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
            if (each == Tab.ADMIN && !canManageCategories()) continue;
            areas.add(tabArea(each));
        }
        return areas;
    }

    /** The drawn material row under the pointer, for JEI recipe lookup and bookmarks. */
    @Nullable
    public ItemStack getJeiMaterialAt(double mouseX, double mouseY) {
        ToolMaterialsTab list = activeMaterialsAt(mouseX, mouseY);
        return list == null ? null : list.ingredientAt(mouseX, mouseY);
    }

    @Nullable
    public Rect2i getJeiMaterialAreaAt(double mouseX, double mouseY) {
        ToolMaterialsTab list = activeMaterialsAt(mouseX, mouseY);
        return list == null ? null : list.ingredientAreaAt(mouseX, mouseY);
    }

    @Nullable
    private ToolMaterialsTab activeMaterialsAt(double mouseX, double mouseY) {
        if (categoryPopupTarget != null && isOnCategoryPopup(mouseX, mouseY)) return null;
        if (tab == Tab.MATERIALS) return materials;
        if (tab == Tab.STRUCTURES && compactMaterialsVisible()) return compactMaterials;
        return null;
    }

    @Nullable
    private Tab tabAt(double mouseX, double mouseY) {
        for (Tab each : Tab.values()) {
            if (each == Tab.ADMIN && !canManageCategories()) continue;
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
        GuiStructureRenderer renderer = renderer();
        if (renderer == null) {
            return 0;
        }
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
        GuiStructureRenderer renderer = renderer();
        if (renderer == null) {
            Component hint = Component.translatable("gui.mm.tool.preview.none");
            int width = this.font.width(hint);
            gfx.drawString(this.font, hint, view.x() + (view.w() - width) / 2, view.y() + view.h() / 2 - 4, LABEL, false);
            return;
        }
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
        if (!hasSelection()) {
            drawClipped(gfx, Component.translatable("tooltip.mm.structure_builder.no_structure"), x, y, width, LABEL);
            return;
        }
        if (selectedBuilder != null) {
            drawClipped(gfx, builderInfoLine(), x, y, width, TEXT);
            return;
        }
        MutableComponent line = infoLine().copy().withStyle(Style.EMPTY.withColor(TEXT))
                .append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(LABEL)))
                .append(portsLine());
        drawClipped(gfx, line, x, y, width, TEXT);
    }

    /** Another mod's structure: name, size and block count, and why it can't be built when it can't. */
    private Component builderInfoLine() {
        //noinspection DataFlowIssue - only called with a builder structure selected
        Vec3i size = selectedBuilder.size();
        MutableComponent line = Component.translatable("gui.mm.tool.info", selectedBuilder.displayName(), size.getX(), size.getY(), size.getZ(),
                selectedBuilder.blockCount()).withStyle(Style.EMPTY.withColor(TEXT));
        if (!selectedBuilder.buildable()) {
            //noinspection DataFlowIssue - not buildable means there is an unbuildable block
            line.append(Component.literal(" · ").withStyle(Style.EMPTY.withColor(LABEL)))
                    .append(Component.translatable("gui.mm.tool.unbuildable", selectedBuilder.unbuildableBlock().getName())
                            .withStyle(Style.EMPTY.withColor(ADJUSTED)));
        }
        return line;
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
        // only MM structures have the ports tooltip
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

    private boolean compactMaterialsVisible() {
        return compactMaterialsWidth >= 90;
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
        if (categoryPopupTarget != null && isOnCategoryPopup(mouseX, mouseY)) return;
        if (this.hoveredSlot instanceof ToolSlot && this.menu.getCarried().isEmpty()) {
            // the store has no label on screen
            tooltip = List.of(Component.translatable("gui.mm.tool.store"));
        } else if (hoveredTab != null) {
            tooltip = List.of(Component.translatable(switch (hoveredTab) {
                case STRUCTURES -> "gui.mm.tool.tab.structures";
                case MATERIALS -> "gui.mm.tool.tab.materials";
                case SETTINGS -> "gui.mm.tool.tab.settings";
                case CONFIG -> "config.mm.title";
                case ADMIN -> "gui.mm.tool.tab.admin";
            }));
        } else if (isOnEnergy(mouseX, mouseY)) {
            tooltip = List.of(Component.translatable("tooltip.mm.structure_builder.energy",
                    CountFormat.grouped(energyStored()), CountFormat.grouped(MMConfigSetup.COMMON.toolEnergyCapacity.get())));
        } else if (isOnMeStatus(mouseX, mouseY)) {
            tooltip = List.of(meStatus());
        } else if (tab == Tab.SETTINGS) {
            tooltip = settings.tooltip(mouseX, mouseY);
        } else if (tab == Tab.MATERIALS) {
            tooltip = materials.tooltip(mouseX, mouseY);
        } else if (tab == Tab.STRUCTURES && compactMaterialsVisible() &&
                (tooltip = compactMaterials.tooltip(mouseX, mouseY)) != null) {
            // Show the full material name and exact counts in the compact list.
        } else if (tab == Tab.ADMIN) {
            tooltip = null;
        } else if (isOnLayerBar(mouseX, mouseY)) {
            tooltip = List.of(Component.translatable("jei.mm.structure.layer.hint"));
        } else if (isOnPortsLine(mouseX, mouseY)) {
            tooltip = portsTooltip();
        } else if (tab == Tab.STRUCTURES && !search.isMouseOver(mouseX, mouseY)) {
            tooltip = gallery.tooltip(mouseX, mouseY);
            if (tooltip != null && canManageCategories()) {
                tooltip = new ArrayList<>(tooltip);
                tooltip.add(Component.translatable("gui.mm.tool.admin.right_click").withStyle(ChatFormatting.GRAY));
            }
        }
        if (tooltip != null) {
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (categoryPopupTarget != null) {
            if (button == 0 && isOnCategoryPopup(mouseX, mouseY)
                    && mouseY >= categoryPopupY + 3 && mouseY < categoryPopupY + popupHeight() - 3) {
                int index = categoryPopupScroll + ((int) mouseY - categoryPopupY - 3) / 12;
                List<String> categories = adminCategories();
                if (index >= 0 && index < categories.size()) {
                    MMNetwork.INSTANCE.sendToServer(new StructureCategoryEditPkt(StructureCategories.Action.ASSIGN,
                            StructureCategories.key(categoryPopupTarget.id(), categoryPopupTarget.builder()), categories.get(index)));
                    playClick();
                }
            }
            categoryPopupTarget = null;
            return true;
        }
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
        } else if (tab == Tab.ADMIN) {
            if (categoryNameBox.isMouseOver(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);
            unfocusCategoryName();
            if (adminMouseClicked(mouseX, mouseY, button)) return true;
        } else if (tab == Tab.STRUCTURES) {
            if (button == 0 && isOnLayerBar(mouseX, mouseY)) {
                GuiPos view = viewport();
                if (mouseX < view.x() + LAYER_ARROW) {
                    stepLayer(-1);
                } else if (mouseX >= view.x() + view.w() - LAYER_ARROW) {
                    stepLayer(1);
                }
                return true;
            }
            lastClickX = (int) mouseX;
            lastClickY = (int) mouseY;
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
        if (isOnCategoryPopup(mouseX, mouseY)) {
            int max = Math.max(0, adminCategories().size() - 8);
            categoryPopupScroll = Mth.clamp(categoryPopupScroll - (int) Math.signum(delta), 0, max);
            return true;
        }
        if (tab == Tab.SETTINGS) {
            if (settings.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        } else if (tab == Tab.ADMIN) {
            if (adminMouseScrolled(mouseX, mouseY, delta)) return true;
        } else if (tab == Tab.MATERIALS) {
            if (materials.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        } else {
            if (compactMaterialsVisible() && compactMaterials.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
            if (isOnLayerBar(mouseX, mouseY)) {
                // scrolling up moves up the structure
                stepLayer(delta > 0 ? 1 : -1);
                return true;
            }
            if (renderer() != null && isOnView(mouseX, mouseY)) {
                renderer().zoom(delta);
                return true;
            }
            if (gallery.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (tab == Tab.STRUCTURES && gallery.mouseDragged(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (gallery.mouseReleased(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // The inventory key (normally E) must type into the category name instead of closing the menu.
        if (categoryNameBox.visible && categoryNameBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                unfocusCategoryName();
                return true;
            }
            categoryNameBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
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
