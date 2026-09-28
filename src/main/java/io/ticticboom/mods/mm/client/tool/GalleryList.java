package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.StructureCategories;
import io.ticticboom.mods.mm.util.TextMatch;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
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
 * The tool screen's structure list, grouped by the world's category settings. Groups can be collapsed and searched.
 */
public class GalleryList {
    private static final int ROW = 10;
    private static final int INDENT = 8;
    private static final int TEXT = 0xE0E0E0;
    private static final int GROUP = 0x9A9A9A;
    private static final int MATCH = 0xFFD84D;
    private static final int SELECTED = 0xFF3A4A6A;
    private static final int HOVER = 0x30FFFFFF;
    // collapsed categories; remembered while the game runs
    private static final Set<String> COLLAPSED = new HashSet<>();

    /**
     * One structure in the list: an MM {@link StructureModel} ({@code builder} false) or another mod's
     * {@link BuildableStructure} ({@code builder} true).
     */
    public record Entry(ResourceLocation id, String name, String namespace, boolean builder, Object structure) {
        public static Entry of(StructureModel model) {
            return new Entry(model.id(), model.name(), model.id().getNamespace(), false, model);
        }

        public static Entry of(BuildableStructure structure) {
            return new Entry(structure.id(), structure.displayName().getString(), structure.group(), true, structure);
        }

        public boolean is(@Nullable ResourceLocation otherId, boolean otherBuilder) {
            return id.equals(otherId) && builder == otherBuilder;
        }
    }

    private record Group(String category, Component name, List<Entry> structures) {
    }

    /** One shown line: a group header, or a structure with where the search text is in its name (-1 when not). */
    private record Line(Group group, @Nullable Entry structure, int shownCount, int matchStart) {
    }

    private final Font font;
    private final List<Entry> allEntries;
    private List<Group> groups;
    private long categoryRevision;
    private final Consumer<Entry> onSelect;
    private final Consumer<Entry> onCategorize;
    private List<Line> lines = List.of();
    private String query = "";
    private int scroll;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean draggingScrollbar;
    @Nullable
    private ResourceLocation selected;
    private boolean selectedBuilder;

    public GalleryList(Font font, Collection<Entry> structures, @Nullable ResourceLocation selected, boolean selectedBuilder,
                       Consumer<Entry> onSelect, Consumer<Entry> onCategorize) {
        this.font = font;
        this.onSelect = onSelect;
        this.onCategorize = onCategorize;
        this.selected = selected;
        this.selectedBuilder = selectedBuilder;
        // The pack also ships old Multi Builder Tool NBT copies of its MM machines. Prefer the live MM
        // structure, whose ports and tier choices stay in sync with the machine definition.
        Set<String> mmMachines = new HashSet<>();
        for (Entry entry : structures) {
            if (!entry.builder()) mmMachines.add(machineKey(entry.id()));
        }
        this.allEntries = structures.stream().filter(entry -> !entry.builder()
                || !entry.id().getNamespace().equals("mbtool")
                || !entry.id().getPath().startsWith("custom_multiblocks/")
                || !mmMachines.contains(machineKey(entry.id()))).toList();
        this.categoryRevision = StructureCategories.clientSnapshot().revision();
        this.groups = group(allEntries);
        refresh();
    }

    private static List<Group> group(Collection<Entry> structures) {
        Map<String, List<Entry>> byCategory = new LinkedHashMap<>();
        var config = StructureCategories.clientSnapshot();
        for (Entry structure : structures) {
            String category = config.category(structure.id(), structure.builder());
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(structure);
        }
        var result = new ArrayList<Group>();
        byCategory.forEach((category, list) -> {
            list.sort(Comparator.comparing((Entry s) -> s.name().toLowerCase(Locale.ROOT)).thenComparing(s -> s.id().toString()));
            result.add(new Group(category, Component.literal(category), list));
        });
        result.sort(Comparator.comparing((Group g) -> g.category().equals(StructureCategories.DEFAULT))
                .thenComparing(g -> g.name().getString().toLowerCase(Locale.ROOT)));
        return result;
    }

    private void refreshCategoriesIfNeeded() {
        long revision = StructureCategories.clientSnapshot().revision();
        if (revision == categoryRevision) return;
        categoryRevision = revision;
        groups = group(allEntries);
        refresh();
    }

    private static String machineKey(ResourceLocation id) {
        String path = id.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        String version = "";
        var structure = java.util.regex.Pattern.compile("(.+?)_structure([0-9._]*)$").matcher(name);
        var tier = java.util.regex.Pattern.compile("(.+?)_tier_?([0-9._]+)$").matcher(name);
        var numbered = java.util.regex.Pattern.compile("(.+?)_([0-9]+)$").matcher(name);
        if (structure.matches()) {
            name = structure.group(1);
            version = structure.group(2);
        } else if (tier.matches()) {
            name = tier.group(1);
            version = tier.group(2);
        } else if (numbered.matches()) {
            name = numbered.group(1);
            version = numbered.group(2);
        }
        var tierBeforeStructure = java.util.regex.Pattern.compile("(.+?)_tier_?([0-9._]+)$").matcher(name);
        if (tierBeforeStructure.matches()) {
            name = tierBeforeStructure.group(1);
            version = tierBeforeStructure.group(2);
        }
        String normalizedVersion = version.replaceAll("[^0-9]", ".").replaceAll("^\\.+|\\.+$", "");
        return name.replaceAll("[^a-z0-9]", "") + ":" + (normalizedVersion.isEmpty() ? "1" : normalizedVersion);
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
            Entry structure = lines.get(i).structure();
            if (structure != null && structure.is(selected, selectedBuilder)) {
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
            for (Entry structure : group.structures()) {
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
            if (searching || !COLLAPSED.contains(group.category())) {
                result.addAll(matches);
            }
        }
        lines = result;
        clampScroll();
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    private int maxScroll() { return Math.max(0, lines.size() * ROW - height); }
    private boolean hasScrollbar() { return maxScroll() > 0; }
    private int thumbHeight() { return Math.max(8, height * height / (lines.size() * ROW)); }

    private void scrollToThumb(double mouseY) {
        int range = height - thumbHeight();
        scroll = range <= 0 ? 0 : (int) Math.round((mouseY - y - thumbHeight() / 2.0) * maxScroll() / range);
        clampScroll();
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY) {
        refreshCategoriesIfNeeded();
        if (lines.isEmpty()) {
            String key = groups.isEmpty() ? "gui.mm.tool.gallery.empty" : "gui.mm.tool.gallery.no_match";
            drawClipped(gfx, Component.translatable(key), x + 2, y + 2, width - 4, GROUP);
            return;
        }
        boolean scrollbar = hasScrollbar();
        int textWidth = width - 2 - (scrollbar ? 4 : 0);
        gfx.enableScissor(x, y, x + width, y + height);
        Line hovered = lineAt(mouseX, mouseY);
        for (int i = 0; i < lines.size(); i++) {
            int top = y + i * ROW - scroll;
            if (top + ROW <= y || top >= y + height) {
                continue;
            }
            Line line = lines.get(i);
            Entry structure = line.structure();
            if (structure != null && structure.is(selected, selectedBuilder)) {
                gfx.fill(x, top, x + textWidth, top + ROW, SELECTED);
            } else if (line == hovered) {
                gfx.fill(x, top, x + textWidth, top + ROW, HOVER);
            }
            if (structure == null) {
                boolean open = !query.isEmpty() || !COLLAPSED.contains(line.group().category());
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
            int barHeight = thumbHeight();
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
        Entry structure = line.structure();
        return List.of(Component.literal(structure.name()), Component.literal(structure.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasScrollbar() && isMouseOver(mouseX, mouseY) && mouseX >= x + width - 5) {
            draggingScrollbar = true;
            scrollToThumb(mouseY);
            return true;
        }
        Line line = lineAt(mouseX, mouseY);
        if (line == null) {
            return false;
        }
        if (button == 1 && line.structure() != null) {
            onCategorize.accept(line.structure());
            return true;
        }
        if (button != 0) return false;
        if (line.structure() == null) {
            if (query.isEmpty()) {
                String category = line.group().category();
                if (!COLLAPSED.remove(category)) {
                    COLLAPSED.add(category);
                }
                refresh();
            }
            return true;
        }
        selected = line.structure().id();
        selectedBuilder = line.structure().builder();
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

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!draggingScrollbar || button != 0) return false;
        scrollToThumb(mouseY);
        return true;
    }

    public boolean mouseReleased(int button) {
        if (!draggingScrollbar || button != 0) return false;
        draggingScrollbar = false;
        return true;
    }
}
