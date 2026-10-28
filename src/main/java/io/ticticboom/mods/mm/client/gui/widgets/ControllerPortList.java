package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import java.util.function.Supplier;
import java.util.function.Function;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ControllerPortList {
    private static final int HEADER_H = 12;
    private static final int NAME_H = 11;
    private static final int SLOT = 18;
    private static final int MAX_ITEM_LINES = 2;
    private static final int BAR_H = 14;
    private static final int TEXT_H = 10;
    private static final int GAP = 4;
    private static final int LABEL = 0x8A8A8A;
    private static final int NAME = 0xBBBBBB;
    private static final int ENERGY_TEXTURE_W = 160;

    private final Function<Level, List<Entry>> source;
    private int x;
    private int y;
    private int width;
    private int height;
    private double scroll;
    private boolean inputs = true;

    public record Entry(Component name, ItemStack icon, boolean input, IPortStorage storage) {
    }

    private record Row(@Nullable Component header, @Nullable Entry port, List<PortContent> contents,
                       List<Component> text, int top, int h) {
    }

    public record HoveredContent(PortContent content, Rect2i area) {
    }

    public ControllerPortList(List<BlockPos> positions) {
        this.source = level -> {
            var entries = new ArrayList<Entry>();
            for (BlockPos pos : positions) {
                if (level.getBlockEntity(pos) instanceof IPortBlockEntity port) {
                    entries.add(new Entry(port.getModel().displayName(), portIcon(port), port.isInput(), port.getStorage()));
                }
            }
            return entries;
        };
    }

    public ControllerPortList(Supplier<List<Entry>> entries) {
        this.source = level -> entries.get();
    }

    public void showInputs(boolean inputs) {
        if (this.inputs != inputs) {
            this.inputs = inputs;
            scroll = 0;
        }
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    private int listWidth() {
        return width - 4;
    }

    private int slotsPerLine() {
        return Math.max(1, listWidth() / SLOT);
    }

    private int shownSlots(int items) {
        return Math.min(items, slotsPerLine() * MAX_ITEM_LINES);
    }

    private int contentHeight(List<PortContent> contents, List<Component> text) {
        if (contents.isEmpty()) return Math.max(1, text.size()) * TEXT_H;
        if (contents.get(0).isTank()) return contents.size() * (BAR_H + 1);
        int lines = (shownSlots(contents.size()) + slotsPerLine() - 1) / slotsPerLine();
        return lines * SLOT;
    }

    private List<Row> rows(@Nullable Level level) {
        var rows = new ArrayList<Row>();
        if (level == null) {
            return rows;
        }
        var shown = new ArrayList<Entry>();
        for (Entry entry : source.apply(level)) {
            if (entry.input() == inputs) {
                shown.add(entry);
            }
        }
        if (shown.isEmpty()) {
            rows.add(new Row(Component.translatable(inputs ? "gui.mm.controller.ports.no_inputs" : "gui.mm.controller.ports.no_outputs"),
                    null, List.of(), List.of(), 0, HEADER_H));
            return rows;
        }
        rows.add(new Row(Component.translatable(inputs ? "gui.mm.controller.ports.inputs" : "gui.mm.controller.ports.outputs", shown.size()),
                null, List.of(), List.of(), 0, HEADER_H));
        int top = HEADER_H;
        for (Entry port : shown) {
            var contents = port.storage().contents();
            List<Component> text = contents.isEmpty() ? port.storage().describeContents() : List.of();
            int h = NAME_H + contentHeight(contents, text) + GAP;
            rows.add(new Row(null, port, contents, text, top, h));
            top += h;
        }
        return rows;
    }

    private int maxScroll(List<Row> rows) {
        if (rows.isEmpty()) return 0;
        Row last = rows.get(rows.size() - 1);
        return Math.max(0, last.top() + last.h() - height);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta, @Nullable Level level) {
        if (!WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, x, y, width, height)) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll(rows(level)), scroll - delta * SLOT));
        return true;
    }

    public void render(GuiGraphics gfx, Font font, @Nullable Level level) {
        var rows = rows(level);
        scroll = Math.min(scroll, maxScroll(rows));
        gfx.enableScissor(x, y, x + width, y + height);
        for (Row row : rows) {
            int top = y + row.top() - (int) scroll;
            if (top + row.h() <= y || top >= y + height) continue;
            if (row.header() != null) {
                gfx.drawString(font, row.header(), x, top + 2, LABEL, false);
                continue;
            }
            drawPort(gfx, font, row, top);
        }
        gfx.disableScissor();

        int max = maxScroll(rows);
        if (max > 0) {
            int trackX = x + width - 2;
            gfx.fill(trackX, y, trackX + 2, y + height, 0xFF2A2A2A);
            int thumbH = Math.max(8, height * height / (height + max));
            int thumbY = y + (int) ((height - thumbH) * (scroll / max));
            gfx.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFF8A8A8A);
        }
    }

    private static ItemStack portIcon(IPortBlockEntity port) {
        BlockEntity be = port.getBlockEntity();
        return new ItemStack(be.getBlockState().getBlock());
    }

    private void drawPort(GuiGraphics gfx, Font font, Row row, int top) {
        var pose = gfx.pose();
        pose.pushPose();
        pose.translate(x, top, 0);
        pose.scale(9f / 16f, 9f / 16f, 1);
        gfx.renderItem(row.port().icon(), 0, 0);
        pose.popPose();
        var name = font.ellipsize(row.port().name(), listWidth() - 12);
        gfx.drawString(font, Language.getInstance().getVisualOrder(name), x + 12, top + 1, NAME, false);

        int cy = top + NAME_H;
        var contents = row.contents();
        if (contents.isEmpty()) {
            List<Component> text = row.text().isEmpty() ? List.of(Component.translatable("gui.mm.port.tank.empty")) : row.text();
            for (int i = 0; i < text.size(); i++) {
                var line = font.ellipsize(text.get(i), listWidth() - 2);
                gfx.drawString(font, Language.getInstance().getVisualOrder(line), x + 2, cy + 1 + i * TEXT_H, LABEL, false);
            }
            return;
        }
        if (contents.get(0).isTank()) {
            for (int i = 0; i < contents.size(); i++) {
                drawBar(gfx, font, contents.get(i), x, cy + i * (BAR_H + 1), listWidth(), BAR_H);
            }
            return;
        }
        int shown = shownSlots(contents.size());
        boolean overflow = shown < contents.size();
        for (int i = 0; i < shown; i++) {
            int sx = x + (i % slotsPerLine()) * SLOT;
            int sy = cy + (i / slotsPerLine()) * SLOT;
            gfx.blit(Ref.UiTextures.SLOT_PARTS, sx, sy, 0, 26, SLOT, SLOT);
            if (overflow && i == shown - 1) {
                String more = "+" + (contents.size() - shown + 1);
                gfx.drawString(font, more, sx + (SLOT - font.width(more)) / 2 + 1, sy + 5, 0xFFFFFF, true);
            } else {
                PortContentIcon.draw(gfx, contents.get(i), sx, sy);
            }
        }
    }

    private static void drawBar(GuiGraphics gfx, Font font, PortContent tank, int bx, int by, int bw, int bh) {
        gfx.fill(bx, by, bx + bw, by + bh, 0xFF555555);
        gfx.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, 0xFF111111);
        int innerW = bw - 2;
        int innerH = bh - 2;
        double fraction = tank.capacity() <= 0 ? 0 : Math.min(1, (double) tank.amount() / tank.capacity());
        int filled = (int) Math.round(innerW * fraction);
        if (filled > 0) {
            int fx = bx + 1;
            int fy = by + 1;
            if (tank.kind() == PortContent.Kind.ENERGY) {
                for (int tx = 0; tx < filled; tx += ENERGY_TEXTURE_W) {
                    gfx.blit(Ref.UiTextures.SLOT_PARTS, fx + tx, fy, 90, 0, Math.min(ENERGY_TEXTURE_W, filled - tx), innerH);
                }
            } else {
                TextureAtlasSprite sprite = null;
                int tint = tank.tint();
                if (tank.kind() == PortContent.Kind.FLUID && !tank.fluid().isEmpty()) {
                    var ext = IClientFluidTypeExtensions.of(tank.fluid().getFluid());
                    sprite = atlas(ext.getStillTexture(tank.fluid()));
                    tint = ext.getTintColor(tank.fluid());
                } else if (tank.sprite() != null) {
                    sprite = atlas(tank.sprite());
                }
                if (sprite == null && tank.kind() != PortContent.Kind.FLUID) {
                    gfx.fill(fx, fy, fx + filled, fy + innerH, 0xFF000000 | tint);
                } else if (sprite != null) {
                    float alpha = ((tint >> 24) & 0xFF) / 255f;
                    gfx.setColor((tint >> 16 & 0xFF) / 255f, (tint >> 8 & 0xFF) / 255f, (tint & 0xFF) / 255f, alpha == 0 ? 1 : alpha);
                    gfx.enableScissor(fx, fy, fx + filled, fy + innerH);
                    for (int tx = fx; tx < fx + filled; tx += 16) {
                        gfx.blit(tx, fy, 0, 16, 16, sprite);
                    }
                    gfx.disableScissor();
                    gfx.setColor(1, 1, 1, 1);
                }
            }
        }
        String amounts = CountFormat.compact(tank.amount()) + " / " + CountFormat.compact(tank.capacity()) + (tank.unit().isEmpty() ? "" : " " + tank.unit());
        Component label = Component.literal(amounts);
        if (tank.name() != null && (tank.kind() == PortContent.Kind.FLUID || tank.sprite() != null)) {
            Component named = tank.name().copy().append("  " + amounts);
            if (font.width(named) <= innerW - 4) {
                label = named;
            }
        }
        gfx.drawString(font, label, bx + (bw - font.width(label)) / 2, by + (bh - 8) / 2 + 1, 0xFFFFFF, true);
    }

    @Nullable
    private static TextureAtlasSprite atlas(@Nullable Identifier id) {
        if (id == null) return null;
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }

    @Nullable
    public HoveredContent ingredientAt(@Nullable Level level, double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return null;
        for (Row row : rows(level)) {
            if (row.port() == null || row.contents().isEmpty()) continue;
            int top = y + row.top() - (int) scroll;
            int cy = top + NAME_H;
            if (mouseY < cy || mouseY >= Math.min(y + height, top + row.h())) continue;
            List<PortContent> contents = row.contents();
            if (contents.get(0).isTank()) {
                PortContent tank = contents.get(0);
                if (tank.kind() != PortContent.Kind.FLUID || tank.fluid().isEmpty()
                        || mouseX >= x + listWidth() || mouseY >= cy + BAR_H) return null;
                int visibleTop = Math.max(cy, y);
                return new HoveredContent(tank, new Rect2i(x, visibleTop, listWidth(),
                        Math.min(cy + BAR_H, y + height) - visibleTop));
            }
            int col = ((int) mouseX - x) / SLOT;
            int line = ((int) mouseY - cy) / SLOT;
            int index = line * slotsPerLine() + col;
            int shown = shownSlots(contents.size());
            if (col >= slotsPerLine() || line < 0 || index >= shown
                    || (shown < contents.size() && index == shown - 1)) return null;
            PortContent item = contents.get(index);
            if (item.item().isEmpty()) return null;
            int sx = x + col * SLOT;
            int sy = cy + line * SLOT;
            int visibleTop = Math.max(sy, y);
            return new HoveredContent(item, new Rect2i(sx, visibleTop, SLOT,
                    Math.min(sy + SLOT, y + height) - visibleTop));
        }
        return null;
    }

    @Nullable
    public List<Component> tooltip(Font font, @Nullable Level level, int mouseX, int mouseY) {
        if (!WidgetUtils.isPointerWithinSized(mouseX, mouseY, x, y, width, height)) {
            return null;
        }
        for (Row row : rows(level)) {
            if (row.port() == null) continue;
            int top = y + row.top() - (int) scroll;
            if (mouseY < top || mouseY >= top + row.h()) continue;
            Entry port = row.port();
            if (mouseY < top + NAME_H) {
                return List.of(port.name(),
                        Component.translatable(port.input() ? "gui.mm.controller.ports.input" : "gui.mm.controller.ports.output")
                                .withStyle(ChatFormatting.GRAY));
            }
            var contents = row.contents();
            if (contents.isEmpty()) return row.text().isEmpty() ? null : row.text();
            if (contents.get(0).isTank()) {
                int index = (mouseY - top - NAME_H) / (BAR_H + 1);
                if (index < 0 || index >= contents.size()) return null;
                PortContent tank = contents.get(index);
                var lines = new ArrayList<Component>();
                lines.add(PortContentIcon.name(tank));
                if (contents.size() == 1 && tank.kind() == PortContent.Kind.CHEMICAL && tank.sprite() == null
                        && !port.storage().describeContents().isEmpty()) {
                    port.storage().describeContents().forEach(line -> lines.add(line.copy().withStyle(ChatFormatting.GRAY)));
                } else {
                    lines.add(Component.literal(CountFormat.grouped(tank.amount()) + " / "
                            + CountFormat.grouped(tank.capacity()) + " " + tank.unit()).withStyle(ChatFormatting.GRAY));
                }
                return lines;
            }
            int col = (mouseX - x) / SLOT;
            int line = (mouseY - top - NAME_H) / SLOT;
            if (col < 0 || col >= slotsPerLine() || line < 0) return null;
            int index = line * slotsPerLine() + col;
            int shown = shownSlots(contents.size());
            if (index >= shown) return null;
            if (shown < contents.size() && index == shown - 1) {
                var lines = new ArrayList<Component>();
                for (PortContent item : contents.subList(index, contents.size())) {
                    lines.add(PortContentIcon.name(item).copy().append(Component.literal("  " + CountFormat.grouped(item.amount())).withStyle(ChatFormatting.GRAY)));
                }
                return lines;
            }
            PortContent item = contents.get(index);
            return List.of(PortContentIcon.name(item), Component.literal(CountFormat.grouped(item.amount())).withStyle(ChatFormatting.GRAY));
        }
        return null;
    }
}
