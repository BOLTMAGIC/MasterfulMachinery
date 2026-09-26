package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.TextMatch;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The tool screen's structure list: structures grouped by the mod (namespace) they come from, groups collapsible.
 * A search text hides the structures whose name and id do not contain it, and groups left empty, and underlines
 * the match in the name.
 */
public class GalleryList {
    private static final int ROW = 10;
    private static final int INDENT = 8;
    private static final int TEXT = 0xE0E0E0;
    private static final int GROUP = 0x9A9A9A;
    private static final int MATCH = 0xFFD84D;
    private static final int SELECTED = 0xFF3A4A6A;
    private static final int HOVER = 0x30FFFFFF;
    // collapsed groups by namespace; remembered while the game runs
    private static final Set<String> COLLAPSED = new HashSet<>();

    private record Group(String namespace, Component name, List<StructureModel> structures) {
    }

    /** One shown line: a group header, or a structure with where the search text is in its name (-1 when not). */
    private record Line(Group group, @Nullable StructureModel structure, int shownCount, int matchStart) {
    }

    private final Font font;
    private final List<Group> groups;
    private final Consumer<StructureModel> onSelect;
    private List<Line> lines = List.of();
    private String query = "";
    private int scroll;
    private int x;
    private int y;
    private int width;
    private int height;
    @Nullable
    private ResourceLocation selected;

    public GalleryList(Font font, Collection<StructureModel> structures, @Nullable ResourceLocation selected, Consumer<StructureModel> onSelect) {
        this.font = font;
        this.onSelect = onSelect;
        this.selected = selected;
        this.groups = group(structures);
        refresh();
    }

    private static List<Group> group(Collection<StructureModel> structures) {
        Map<String, List<StructureModel>> byNamespace = new LinkedHashMap<>();
        for (StructureModel structure : structures) {
            byNamespace.computeIfAbsent(structure.id().getNamespace(), k -> new ArrayList<>()).add(structure);
        }
        var result = new ArrayList<Group>();
        byNamespace.forEach((namespace, list) -> {
            list.sort(Comparator.comparing((StructureModel s) -> s.name().toLowerCase(Locale.ROOT)).thenComparing(s -> s.id().toString()));
            result.add(new Group(namespace, Component.literal(modName(namespace)), list));
        });
        result.sort(Comparator.comparing(g -> g.name().getString().toLowerCase(Locale.ROOT)));
        return result;
    }

    /** The mod's display name, or the namespace itself for datapacks and scripts. */
    private static String modName(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        clampScroll();
    }

    public void setQuery(String query) {
        // not lower-cased: that may change the length; matching ignores case itself (TextMatch)
        String normalized = query.strip();
        if (!normalized.equals(this.query)) {
            this.query = normalized;
            scroll = 0;
            refresh();
        }
    }

    /** Scrolls so the selected structure is in view. */
    public void showSelected() {
        for (int i = 0; i < lines.size(); i++) {
            StructureModel structure = lines.get(i).structure();
            if (structure != null && structure.id().equals(selected)) {
                int top = i * ROW;
                if (top < scroll || top + ROW > scroll + height) {
                    scroll = top - height / 2;
                    clampScroll();
                }
                return;
            }
        }
    }

    private void refresh() {
        var result = new ArrayList<Line>();
        boolean searching = !query.isEmpty();
        for (Group group : groups) {
            var matches = new ArrayList<Line>();
            for (StructureModel structure : group.structures()) {
                int start = searching ? TextMatch.indexOfIgnoreCase(structure.name(), query) : -1;
                if (searching && start < 0 && !TextMatch.containsIgnoreCase(structure.id().toString(), query)) {
                    continue;
                }
                matches.add(new Line(group, structure, 0, start));
            }
            if (matches.isEmpty()) {
                // nothing of this group matches the search: the whole group is hidden
                continue;
            }
            result.add(new Line(group, null, matches.size(), -1));
            // a search shows its matches even in collapsed groups
            if (searching || !COLLAPSED.contains(group.namespace())) {
                result.addAll(matches);
            }
        }
        lines = result;
        clampScroll();
    }

    private void clampScroll() {
        int max = Math.max(0, lines.size() * ROW - height);
        scroll = Math.max(0, Math.min(max, scroll));
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY) {
        if (lines.isEmpty()) {
            String key = groups.isEmpty() ? "gui.mm.tool.gallery.empty" : "gui.mm.tool.gallery.no_match";
            drawClipped(gfx, Component.translatable(key), x + 2, y + 2, width - 4, GROUP);
            return;
        }
        boolean scrollbar = lines.size() * ROW > height;
        int textWidth = width - 2 - (scrollbar ? 4 : 0);
        gfx.enableScissor(x, y, x + width, y + height);
        Line hovered = lineAt(mouseX, mouseY);
        for (int i = 0; i < lines.size(); i++) {
            int top = y + i * ROW - scroll;
            if (top + ROW <= y || top >= y + height) {
                continue;
            }
            Line line = lines.get(i);
            StructureModel structure = line.structure();
            if (structure != null && structure.id().equals(selected)) {
                gfx.fill(x, top, x + textWidth, top + ROW, SELECTED);
            } else if (line == hovered) {
                gfx.fill(x, top, x + textWidth, top + ROW, HOVER);
            }
            if (structure == null) {
                boolean open = !query.isEmpty() || !COLLAPSED.contains(line.group().namespace());
                MutableComponent header = Component.literal(open ? "▾ " : "▸ ")
                        .append(line.group().name()).append(" (" + line.shownCount() + ")");
                drawClipped(gfx, header, x + 2, top + 1, textWidth - 2, GROUP);
            } else {
                drawClipped(gfx, highlighted(structure.name(), line.matchStart()), x + 2 + INDENT, top + 1, textWidth - 2 - INDENT, TEXT);
            }
        }
        gfx.disableScissor();
        if (scrollbar) {
            int total = lines.size() * ROW;
            int barHeight = Math.max(8, height * height / total);
            int barY = y + (height - barHeight) * scroll / (total - height);
            gfx.fill(x + width - 3, y, x + width - 1, y + height, 0xFF2A2A2A);
            gfx.fill(x + width - 3, barY, x + width - 1, barY + barHeight, 0xFF8A8A8A);
        }
    }

    /** The name with the search text underlined in yellow. */
    private Component highlighted(String name, int start) {
        int end = start + query.length();
        if (start < 0 || query.isEmpty() || end > name.length()) {
            return Component.literal(name);
        }
        return Component.literal(name.substring(0, start))
                .append(Component.literal(name.substring(start, end)).withStyle(Style.EMPTY.withColor(MATCH).withUnderlined(true)))
                .append(Component.literal(name.substring(end)));
    }

    private void drawClipped(GuiGraphics gfx, Component text, int drawX, int drawY, int maxWidth, int color) {
        TextRenderUtil.drawClipped(gfx, font, text, drawX, drawY, maxWidth, color);
    }

    @Nullable
    private Line lineAt(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return null;
        }
        int index = (int) ((mouseY - y + scroll) / ROW);
        return index >= 0 && index < lines.size() ? lines.get(index) : null;
    }

    /** @return the full name and id of the structure under the mouse (names may be cut short); null when none */
    @Nullable
    public List<Component> tooltip(double mouseX, double mouseY) {
        Line line = lineAt(mouseX, mouseY);
        if (line == null || line.structure() == null) {
            return null;
        }
        StructureModel structure = line.structure();
        return List.of(Component.literal(structure.name()), Component.literal(structure.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Line line = lineAt(mouseX, mouseY);
        if (line == null || button != 0) {
            return false;
        }
        if (line.structure() == null) {
            if (query.isEmpty()) {
                String namespace = line.group().namespace();
                if (!COLLAPSED.remove(namespace)) {
                    COLLAPSED.add(namespace);
                }
                refresh();
            }
            return true;
        }
        selected = line.structure().id();
        onSelect.accept(line.structure());
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scroll -= (int) Math.round(delta * ROW * 2);
        clampScroll();
        return true;
    }
}
