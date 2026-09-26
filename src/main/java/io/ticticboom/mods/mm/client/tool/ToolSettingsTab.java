package io.ticticboom.mods.mm.client.tool;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.PortTiers;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.TierResolver;
import io.ticticboom.mods.mm.client.util.TextRenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.ObjIntConsumer;

/**
 * The tool screen's Settings tab: the preferred tier per registered port type and direction (types that come in
 * more than one tier), cycled with {@code <} / {@code >}, then a note on how a structure may overrule the choice and
 * the dismantle controls.
 */
public class ToolSettingsTab {
    private static final int ROW = 14;
    private static final int BUTTON = 11;
    private static final int TEXT = 0xE0E0E0;
    private static final int LABEL = 0x9A9A9A;
    private static final int VALUE = 0xFFD84D;
    private static final int DIVIDER = 0xFF2C2F36;

    private final Font font;
    private final TierPrefs prefs;
    private final ObjIntConsumer<String> onChange;
    /** port type key -> tier -> block, from the registered ports */
    private final Map<String, NavigableMap<Integer, Block>> rows = PortTiers.registeredTiered();
    private final List<String> keys = List.copyOf(rows.keySet());
    /** the note and the key hint, wrapped to the width; set by {@link #setBounds} */
    private List<FormattedCharSequence> footer = List.of();
    private int x;
    private int y;
    private int width;
    private int height;
    private int scroll;

    /**
     * @param prefs    the tool's preferences; changed in place when a row is cycled
     * @param onChange called with the key and the new tier after a change
     */
    public ToolSettingsTab(Font font, TierPrefs prefs, ObjIntConsumer<String> onChange) {
        this.font = font;
        this.prefs = prefs;
        this.onChange = onChange;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        var lines = new ArrayList<FormattedCharSequence>();
        lines.addAll(font.split(Component.translatable("gui.mm.tool.settings.note"), width - 8));
        lines.addAll(font.split(Component.translatable("gui.mm.tool.settings.dismantle", ToolKeys.DISMANTLE.getTranslatedKeyMessage()), width - 8));
        footer = lines;
        clampScroll();
    }

    private int rowsTop() {
        return y + 14;
    }

    /** Rows end where the note and the key hint begin. */
    private int rowsBottom() {
        return y + height - footer.size() * 9 - 6;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * ROW - (rowsBottom() - rowsTop()));
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    private int labelWidth() {
        return Math.min(120, (width - 8) * 2 / 5);
    }

    // a row: label | < | value | >
    private int leftArrowX() {
        return x + 4 + labelWidth() + 4;
    }

    private int valueX() {
        return leftArrowX() + BUTTON + 4;
    }

    // leaves room for the scroll bar at the right edge
    private int rightArrowX() {
        return x + width - 6 - BUTTON;
    }

    private int valueWidth() {
        return rightArrowX() - 4 - valueX();
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY) {
        drawClipped(gfx, Component.translatable("gui.mm.tool.settings.title"), x + 4, y + 3, width - 8, LABEL);
        int top = rowsTop();
        int bottom = rowsBottom();
        if (rows.isEmpty()) {
            drawClipped(gfx, Component.translatable("gui.mm.tool.settings.none"), x + 4, top + 3, width - 8, TEXT);
        }
        gfx.enableScissor(x, top, x + width, bottom);
        int rowY = top - scroll;
        for (var entry : rows.entrySet()) {
            if (rowY + ROW > top && rowY < bottom) {
                drawRow(gfx, entry.getKey(), entry.getValue(), rowY, mouseX, mouseY);
            }
            rowY += ROW;
        }
        gfx.disableScissor();
        drawScrollBar(gfx, top, bottom);
        int footerY = bottom + 4;
        gfx.fill(x + 4, bottom + 1, x + width - 4, bottom + 2, DIVIDER);
        for (FormattedCharSequence line : footer) {
            gfx.drawString(font, line, x + 4, footerY, LABEL, false);
            footerY += 9;
        }
    }

    /** A thin bar at the right edge when the rows don't all fit, like the structure list's. */
    private void drawScrollBar(GuiGraphics gfx, int top, int bottom) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int visible = bottom - top;
        int total = rows.size() * ROW;
        int barHeight = Math.max(8, visible * visible / total);
        int barY = top + (visible - barHeight) * scroll / max;
        gfx.fill(x + width - 3, top, x + width - 1, bottom, 0xFF2A2A2A);
        gfx.fill(x + width - 3, barY, x + width - 1, barY + barHeight, 0xFF8A8A8A);
    }

    private void drawRow(GuiGraphics gfx, String key, NavigableMap<Integer, Block> tiers, int rowY, int mouseX, int mouseY) {
        gfx.fill(x + 2, rowY + ROW - 1, x + width - 2, rowY + ROW, DIVIDER);
        int textY = rowY + 3;
        drawClipped(gfx, rowName(key, tiers), x + 4, textY, labelWidth() - 4, TEXT);
        drawClipped(gfx, valueText(prefs.get(key), tiers), valueX(), textY, valueWidth(), VALUE);
        drawArrow(gfx, leftArrowX(), rowY + 1, "<", mouseX, mouseY);
        drawArrow(gfx, rightArrowX(), rowY + 1, ">", mouseX, mouseY);
    }

    private void drawArrow(GuiGraphics gfx, int bx, int by, String arrow, int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + BUTTON && mouseY >= by && mouseY < by + BUTTON
                && mouseY >= rowsTop() && mouseY < rowsBottom();
        var texture = hovered ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
        gfx.blitNineSlicedSized(texture, bx, by, BUTTON, BUTTON, 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
        gfx.drawString(font, arrow, bx + (BUTTON - font.width(arrow)) / 2 + 1, by + 2, 0x3A3A3A, false);
    }

    /** "Item Input": the port type's name and the direction. */
    static Component rowName(String key, NavigableMap<Integer, Block> tiers) {
        int slash = key.lastIndexOf('/');
        boolean input = key.endsWith("/input");
        ResourceLocation type = ResourceLocation.tryParse(slash < 0 ? key : key.substring(0, slash));
        Component typeName;
        String langKey = type == null ? "" : "tooltip." + type.getNamespace() + ".port_type." + type.getPath().replace('/', '_');
        if (Language.getInstance().has(langKey)) {
            typeName = Component.translatable(langKey);
        } else {
            // no name for the type: its smallest port says what it is
            typeName = tiers.firstEntry().getValue().getName();
        }
        return Component.translatable(input ? "gui.mm.tool.settings.input" : "gui.mm.tool.settings.output", typeName);
    }

    /** "Lowest: Small Item Input" or "T2 · Medium Item Input". */
    static Component valueText(int preferred, NavigableMap<Integer, Block> tiers) {
        if (preferred == TierResolver.LOWEST) {
            return Component.translatable("gui.mm.assemble.lowest", tiers.firstEntry().getValue().getName());
        }
        Block block = tiers.get(preferred);
        Component name = block == null ? Component.literal("?") : block.getName();
        return Component.translatable("gui.mm.tool.settings.tier", preferred, name);
    }

    private void drawClipped(GuiGraphics gfx, Component text, int drawX, int drawY, int maxWidth, int color) {
        TextRenderUtil.drawClipped(gfx, font, text, drawX, drawY, maxWidth, color);
    }

    @Nullable
    private String rowAt(double mouseY) {
        if (mouseY < rowsTop() || mouseY >= rowsBottom()) {
            return null;
        }
        int index = (int) ((mouseY - rowsTop() + scroll) / ROW);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return keys.get(index);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        String key = rowAt(mouseY);
        if (key == null) {
            return false;
        }
        int step;
        if (mouseX >= leftArrowX() && mouseX < leftArrowX() + BUTTON) {
            step = -1;
        } else if (mouseX >= rightArrowX() && mouseX < rightArrowX() + BUTTON) {
            step = 1;
        } else {
            return false;
        }
        // "lowest" followed by every registered tier
        int next = TierPrefs.cycle(prefs.get(key), rows.get(key).keySet(), step);
        prefs.set(key, next);
        onChange.accept(key, next);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < x || mouseX >= x + width || mouseY < rowsTop() || mouseY >= rowsBottom()) {
            return false;
        }
        scroll -= (int) Math.round(delta * ROW);
        clampScroll();
        return true;
    }

    /** Full row label and value, for rows cut short. */
    @Nullable
    public List<Component> tooltip(double mouseX, double mouseY) {
        boolean onArrow = (mouseX >= leftArrowX() && mouseX < leftArrowX() + BUTTON) || mouseX >= rightArrowX();
        if (mouseX < x || mouseX >= x + width || onArrow) {
            return null;
        }
        String key = rowAt(mouseY);
        if (key == null) {
            return null;
        }
        var tiers = rows.get(key);
        return List.of(rowName(key, tiers), valueText(prefs.get(key), tiers).copy().withStyle(s -> s.withColor(VALUE)));
    }
}
