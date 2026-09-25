package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The controller's second page: every port of the formed machine with what it holds, inputs then
 * outputs. Each port is a small block: its name on top, then its contents at full width, items in
 * real slots (up to two rows, the rest summed up in a "+N" slot) or a fill bar for tanks (fluid,
 * chemical, energy) with the amount written on it. Reads the ports' client-side block entities, so
 * it updates live. All coordinates are absolute screen coordinates.
 */
public class ControllerPortList {
    private static final int HEADER_H = 12;
    private static final int NAME_H = 11;
    private static final int SLOT = 18;
    private static final int MAX_ITEM_LINES = 2;
    private static final int BAR_H = 14;
    private static final int EMPTY_H = 10;
    private static final int GAP = 4;
    private static final int LABEL = 0x8A8A8A;
    private static final int NAME = 0xBBBBBB;
    private static final int ENERGY_TEXTURE_W = 160;

    private final List<BlockPos> positions;
    private int x;
    private int y;
    private int width;
    private int height;
    private double scroll;
    // which ports the list shows: inputs, or outputs
    private boolean inputs = true;

    private record Row(@Nullable Component header, @Nullable IPortBlockEntity port, List<PortContent> contents, int top, int h) {
    }

    public ControllerPortList(List<BlockPos> positions) {
        this.positions = positions;
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

    /** Width available to rows; the scrollbar sits to the right of it. */
    private int listWidth() {
        return width - 4;
    }

    private int slotsPerLine() {
        return Math.max(1, listWidth() / SLOT);
    }

    /** Slots actually drawn for an item port, including a trailing "+N" slot when not everything fits. */
    private int shownSlots(int items) {
        int max = slotsPerLine() * MAX_ITEM_LINES;
        return Math.min(items, max);
    }

    private int contentHeight(List<PortContent> contents) {
        if (contents.isEmpty()) return EMPTY_H;
        if (contents.get(0).isTank()) return BAR_H;
        int lines = (shownSlots(contents.size()) + slotsPerLine() - 1) / slotsPerLine();
        return lines * SLOT;
    }

    private List<Row> rows(@Nullable Level level) {
        var rows = new ArrayList<Row>();
        if (level == null) {
            return rows;
        }
        var inputs = new ArrayList<IPortBlockEntity>();
        var outputs = new ArrayList<IPortBlockEntity>();
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof IPortBlockEntity port) {
                (port.isInput() ? inputs : outputs).add(port);
            }
        }
        if (this.inputs) {
            addGroup(rows, 0, "gui.mm.controller.ports.inputs", inputs);
        } else {
            addGroup(rows, 0, "gui.mm.controller.ports.outputs", outputs);
        }
        if (rows.isEmpty()) {
            rows.add(new Row(Component.translatable(this.inputs ? "gui.mm.controller.ports.no_inputs" : "gui.mm.controller.ports.no_outputs"),
                    null, List.of(), 0, HEADER_H));
        }
        return rows;
    }

    private int addGroup(List<Row> rows, int top, String key, List<IPortBlockEntity> ports) {
        if (ports.isEmpty()) {
            return top;
        }
        rows.add(new Row(Component.translatable(key, ports.size()), null, List.of(), top, HEADER_H));
        top += HEADER_H;
        for (IPortBlockEntity port : ports) {
            var contents = port.getStorage().contents();
            int h = NAME_H + contentHeight(contents) + GAP;
            rows.add(new Row(null, port, contents, top, h));
            top += h;
        }
        return top;
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
        if (rows.isEmpty()) {
            gfx.drawWordWrap(font, Component.translatable("gui.mm.controller.ports.none"), x, y + 2, width, LABEL);
            return;
        }
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
        // name line: small block icon + port name
        var pose = gfx.pose();
        pose.pushPose();
        pose.translate(x, top, 0);
        pose.scale(9f / 16f, 9f / 16f, 1);
        gfx.renderItem(portIcon(row.port()), 0, 0);
        pose.popPose();
        FormattedText name = font.ellipsize(Component.literal(row.port().getModel().name()), listWidth() - 12);
        gfx.drawString(font, Language.getInstance().getVisualOrder(name), x + 12, top + 1, NAME, false);

        int cy = top + NAME_H;
        var contents = row.contents();
        if (contents.isEmpty()) {
            gfx.drawString(font, Component.translatable("gui.mm.port.tank.empty"), x + 2, cy + 1, LABEL, false);
            return;
        }
        if (contents.get(0).isTank()) {
            drawBar(gfx, font, contents.get(0), x, cy, listWidth(), BAR_H);
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
                // MM's own energy bar texture, repeated: it is only ENERGY_TEXTURE_W wide
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
                if (sprite != null) {
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
        String amounts = CountFormat.compact(tank.amount()) + " / " + CountFormat.compact(tank.capacity()) + " " + tank.unit();
        Component label = Component.literal(amounts);
        if (tank.name() != null) {
            Component named = tank.name().copy().append("  " + amounts);
            if (font.width(named) <= innerW - 4) {
                label = named;
            }
        }
        gfx.drawString(font, label, bx + (bw - font.width(label)) / 2, by + (bh - 8) / 2 + 1, 0xFFFFFF, true);
    }

    @Nullable
    private static TextureAtlasSprite atlas(@Nullable net.minecraft.resources.ResourceLocation id) {
        if (id == null) return null;
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }

    /**
     * @return the tooltip for whatever is under the mouse, or null
     */
    @Nullable
    public List<Component> tooltip(Font font, @Nullable Level level, int mouseX, int mouseY) {
        if (!WidgetUtils.isPointerWithinSized(mouseX, mouseY, x, y, width, height)) {
            return null;
        }
        for (Row row : rows(level)) {
            if (row.port() == null) continue;
            int top = y + row.top() - (int) scroll;
            if (mouseY < top || mouseY >= top + row.h()) continue;
            IPortBlockEntity port = row.port();
            if (mouseY < top + NAME_H) {
                return List.of(Component.literal(port.getModel().name()),
                        Component.translatable(port.isInput() ? "gui.mm.controller.ports.input" : "gui.mm.controller.ports.output")
                                .withStyle(ChatFormatting.GRAY));
            }
            var contents = row.contents();
            if (contents.isEmpty()) return null;
            PortContent first = contents.get(0);
            if (first.isTank()) {
                return List.of(PortContentIcon.tooltip(first).get(0), Component.literal(CountFormat.grouped(first.amount()) + " / "
                        + CountFormat.grouped(first.capacity()) + " " + first.unit()).withStyle(ChatFormatting.GRAY));
            }
            int col = (mouseX - x) / SLOT;
            int line = (mouseY - top - NAME_H) / SLOT;
            if (col < 0 || col >= slotsPerLine() || line < 0) return null;
            int index = line * slotsPerLine() + col;
            int shown = shownSlots(contents.size());
            if (index >= shown) return null;
            if (shown < contents.size() && index == shown - 1) {
                // "+N": list what didn't fit
                var lines = new ArrayList<Component>();
                for (PortContent item : contents.subList(index, contents.size())) {
                    lines.add(item.name().copy().append(Component.literal("  " + CountFormat.grouped(item.amount())).withStyle(ChatFormatting.GRAY)));
                }
                return lines;
            }
            PortContent item = contents.get(index);
            return List.of(item.name(), Component.literal(CountFormat.grouped(item.amount())).withStyle(ChatFormatting.GRAY));
        }
        return null;
    }
}
