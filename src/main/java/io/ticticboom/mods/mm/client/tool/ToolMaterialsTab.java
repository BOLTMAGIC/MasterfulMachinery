package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.PlayerMaterials;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.util.StructurePasteUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Materials for the selected structure, compared with the player's inventory and the tool's store. */
final class ToolMaterialsTab {
    private static final int ROW = 20;
    private static final int COMPACT_ROW = 18;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int LABEL = 0xFF9A9A9A;
    private static final int GREEN = 0xFF55D76A;
    private static final int MISSING = 0xFFFFD84D;

    private record Material(ItemStack stack, int needed) {}

    private final Font font;
    private final MultiblockToolMenu menu;
    private final TierPrefs prefs;
    private final boolean compact;
    private final Map<Item, Integer> owned = new HashMap<>();
    private List<Material> materials = List.of();
    private Object lastSelection;
    private int lastPrefsHash;
    private int x;
    private int y;
    private int width;
    private int height;
    private int scroll;

    ToolMaterialsTab(Font font, MultiblockToolMenu menu, TierPrefs prefs, boolean compact) {
        this.font = font;
        this.menu = menu;
        this.prefs = prefs;
        this.compact = compact;
    }

    void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        clampScroll();
    }

    void refresh(@Nullable StructureModel selected, @Nullable BuildableStructure selectedBuilder) {
        Map<Item, Integer> current = new HashMap<>();
        // Match the build source: tool store, main inventory and hotbar. It does not consume offhand items.
        for (int i = 0; i < MultiblockToolMenu.STORE_SLOTS + 36; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (!stack.isEmpty() && PlayerMaterials.buildable(stack, stack.getItem())) {
                current.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        Object selection = selected != null ? selected : selectedBuilder;
        int prefsHash = prefs.asMap().hashCode();
        if (selection == lastSelection && prefsHash == lastPrefsHash && current.equals(owned)) return;
        boolean selectionChanged = selection != lastSelection || prefsHash != lastPrefsHash;
        lastSelection = selection;
        lastPrefsHash = prefsHash;
        owned.clear();
        owned.putAll(current);
        materials = requirements(selected, selectedBuilder);
        if (selectionChanged) scroll = 0;
        clampScroll();
    }

    private List<Material> requirements(@Nullable StructureModel selected, @Nullable BuildableStructure builder) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        if (selected != null) {
            add(counts, StructurePasteUtil.findControllerBlock(selected));
            for (var positioned : selected.layout().getPositionedPieces()) {
                Block block = AssemblyPlanner.chooseBlock(positioned.piece().piece(), prefs,
                        candidate -> owned.getOrDefault(candidate.asItem(), 0) > 0);
                add(counts, block);
            }
        } else if (builder != null) {
            for (var placement : builder.blocks()) add(counts, placement.state().getBlock());
        }
        List<Material> result = new ArrayList<>();
        counts.forEach((item, count) -> result.add(new Material(item.getDefaultInstance(), count)));
        result.sort(Comparator.comparing(row -> row.stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static void add(Map<Item, Integer> counts, @Nullable Block block) {
        if (block != null && !block.asItem().getDefaultInstance().isEmpty()) {
            counts.merge(block.asItem(), 1, Integer::sum);
        }
    }

    private int rowHeight() { return compact ? COMPACT_ROW : ROW; }
    private int listTop() { return y + (compact ? 15 : 28); }
    private int listBottom() { return y + height - 3; }
    private int maxScroll() { return Math.max(0, materials.size() * rowHeight() - (listBottom() - listTop())); }
    private void clampScroll() { scroll = Math.max(0, Math.min(scroll, maxScroll())); }

    void render(GuiGraphics gfx, @Nullable StructureModel selected, @Nullable BuildableStructure builder) {
        refresh(selected, builder);
        gfx.drawString(font, Component.translatable("gui.mm.tool.materials.title"), x + 4, y + 3, TEXT, false);
        if (!compact) gfx.drawString(font, Component.translatable("gui.mm.tool.materials.source"), x + 4, y + 15, LABEL, false);
        if (selected == null && builder == null) {
            TextRenderUtil.drawClipped(gfx, font, Component.translatable("gui.mm.tool.materials.no_selection"),
                    x + 4, listTop() + 5, width - 8, LABEL);
            return;
        }
        gfx.enableScissor(x + 2, listTop(), x + width - 2, listBottom());
        for (int i = 0; i < materials.size(); i++) {
            int rowY = listTop() + i * rowHeight() - scroll;
            if (rowY + rowHeight() <= listTop() || rowY >= listBottom()) continue;
            Material material = materials.get(i);
            int available = owned.getOrDefault(material.stack.getItem(), 0);
            boolean enough = available >= material.needed;
            gfx.renderItem(material.stack, x + 4, rowY + 1);
            String count = compact ? CountFormat.compact(available) + "/" + CountFormat.compact(material.needed)
                    : available + "/" + material.needed;
            int countX = x + width - 20 - font.width(count);
            TextRenderUtil.drawClipped(gfx, font, material.stack.getHoverName(), x + 24, rowY + 4,
                    Math.max(0, countX - x - 27), TEXT);
            gfx.drawString(font, count, countX, rowY + 4, enough ? GREEN : MISSING, false);
            if (enough) drawCheck(gfx, x + width - 15, rowY + 5);
        }
        gfx.disableScissor();
        if (compact && maxScroll() > 0) {
            int trackHeight = listBottom() - listTop();
            int thumbHeight = Math.max(8, trackHeight * trackHeight / (materials.size() * rowHeight()));
            int thumbY = listTop() + scroll * (trackHeight - thumbHeight) / maxScroll();
            gfx.fill(x + width - 3, listTop(), x + width - 1, listBottom(), 0xFF383838);
            gfx.fill(x + width - 3, thumbY, x + width - 1, thumbY + thumbHeight, 0xFFB0B0B0);
        }
    }

    private static void drawCheck(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y + 4, x + 2, y + 6, GREEN);
        gfx.fill(x + 2, y + 6, x + 4, y + 8, GREEN);
        gfx.fill(x + 4, y + 4, x + 6, y + 6, GREEN);
        gfx.fill(x + 6, y + 2, x + 8, y + 4, GREEN);
        gfx.fill(x + 8, y, x + 10, y + 2, GREEN);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < x || mouseX >= x + width || mouseY < listTop() || mouseY >= listBottom()) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta) * rowHeight()));
        return true;
    }

    @Nullable
    List<Component> tooltip(double mouseX, double mouseY) {
        int index = hoveredIndex(mouseX, mouseY);
        if (index < 0) return null;
        Material material = materials.get(index);
        return List.of(material.stack.getHoverName(), Component.translatable("gui.mm.tool.materials.count",
                owned.getOrDefault(material.stack.getItem(), 0), material.needed));
    }

    private int hoveredIndex(double mouseX, double mouseY) {
        if (mouseX < x + 2 || mouseX >= x + width - 2 || mouseY < listTop() || mouseY >= listBottom()) return -1;
        int index = ((int) mouseY - listTop() + scroll) / rowHeight();
        return index >= 0 && index < materials.size() ? index : -1;
    }

    @Nullable
    ItemStack ingredientAt(double mouseX, double mouseY) {
        int index = hoveredIndex(mouseX, mouseY);
        return index < 0 ? null : materials.get(index).stack.copyWithCount(1);
    }

    @Nullable
    Rect2i ingredientAreaAt(double mouseX, double mouseY) {
        int index = hoveredIndex(mouseX, mouseY);
        if (index < 0) return null;
        int rowY = listTop() + index * rowHeight() - scroll;
        int top = Math.max(rowY, listTop());
        return new Rect2i(x + 2, top, width - 4, Math.min(rowY + rowHeight(), listBottom()) - top);
    }
}
