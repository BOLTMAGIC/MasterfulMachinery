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
import net.minecraft.network.chat.Component;
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
 * outputs. Item ports show their items with counts, tanks (fluid, chemical, energy) a fill bar with
 * the amount written on it. Reads the ports' client-side block entities, so it updates live.
 * All coordinates are absolute screen coordinates.
 */
public class ControllerPortList {
    private static final int HEADER_H = 12;
    private static final int ROW_H = 19;
    private static final int ICON_STEP = 18;
    private static final int LABEL = 0x8A8A8A;
    private static final int TEXT = 0xDDDDDD;

    private final List<BlockPos> positions;
    private int x;
    private int y;
    private int width;
    private int height;
    private double scroll;

    private record Row(@Nullable Component header, @Nullable IPortBlockEntity port, List<PortContent> contents, int top, int h) {
    }

    public ControllerPortList(List<BlockPos> positions) {
        this.positions = positions;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
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
        int top = 0;
        top = addGroup(rows, top, "gui.mm.controller.ports.inputs", inputs);
        addGroup(rows, top, "gui.mm.controller.ports.outputs", outputs);
        return rows;
    }

    private static int addGroup(List<Row> rows, int top, String key, List<IPortBlockEntity> ports) {
        if (ports.isEmpty()) {
            return top;
        }
        rows.add(new Row(Component.translatable(key, ports.size()), null, List.of(), top, HEADER_H));
        top += HEADER_H;
        for (IPortBlockEntity port : ports) {
            rows.add(new Row(null, port, port.getStorage().contents(), top, ROW_H));
            top += ROW_H;
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
        scroll = Math.max(0, Math.min(maxScroll(rows(level)), scroll - delta * ROW_H));
        return true;
    }

    public void render(GuiGraphics gfx, Font font, @Nullable Level level) {
        var rows = rows(level);
        scroll = Math.min(scroll, maxScroll(rows));
        if (rows.isEmpty()) {
            gfx.drawWordWrap(font, Component.translatable("gui.mm.controller.ports.none"), x, y + 2, width, LABEL);
            return;
        }
        int listW = width - 4;
        gfx.enableScissor(x, y, x + width, y + height);
        for (Row row : rows) {
            int top = y + row.top() - (int) scroll;
            if (top + row.h() <= y || top >= y + height) continue;
            if (row.header() != null) {
                gfx.drawString(font, row.header(), x, top + 2, LABEL, false);
                continue;
            }
            drawPort(gfx, font, row, x, top, listW);
        }
        gfx.disableScissor();

        // scrollbar
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

    private void drawPort(GuiGraphics gfx, Font font, Row row, int left, int top, int listW) {
        gfx.renderItem(portIcon(row.port()), left, top + 1);
        int cx = left + 20;
        int cw = listW - 20;
        var contents = row.contents();
        if (contents.isEmpty()) {
            gfx.drawString(font, Component.translatable("gui.mm.port.tank.empty"), cx, top + 5, LABEL, false);
            return;
        }
        if (contents.get(0).isTank()) {
            drawBar(gfx, font, contents.get(0), cx, top + 2, cw, 14);
            return;
        }
        int fit = cw / ICON_STEP;
        int shown = contents.size() > fit ? fit - 1 : contents.size();
        for (int i = 0; i < shown; i++) {
            int ix = cx + i * ICON_STEP;
            PortContent item = contents.get(i);
            gfx.renderItem(item.item(), ix, top + 1);
            CountFormat.drawSlotCount(gfx, ix - 1, top, item.amount());
        }
        if (shown < contents.size()) {
            String more = "+" + (contents.size() - shown);
            gfx.drawString(font, more, cx + shown * ICON_STEP + 2, top + 5, TEXT, false);
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
                // MM's own energy bar texture
                gfx.blit(Ref.UiTextures.SLOT_PARTS, fx, fy, 90, 0, filled, innerH);
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
            Component named = tank.name().copy().append(" " + amounts);
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
            if (mouseX < x + 18) {
                return List.of(Component.literal(port.getModel().name()),
                        Component.translatable(port.isInput() ? "gui.mm.controller.ports.input" : "gui.mm.controller.ports.output")
                                .withStyle(ChatFormatting.GRAY));
            }
            var contents = row.contents();
            if (contents.isEmpty()) return null;
            PortContent first = contents.get(0);
            if (first.isTank()) {
                Component name = first.kind() == PortContent.Kind.ENERGY ? Component.translatable("gui.mm.port.energy")
                        : first.name() != null ? first.name() : Component.translatable("gui.mm.port.tank.empty");
                return List.of(name, Component.literal(CountFormat.grouped(first.amount()) + " / "
                        + CountFormat.grouped(first.capacity()) + " " + first.unit()).withStyle(ChatFormatting.GRAY));
            }
            int fit = (width - 24) / ICON_STEP;
            int shown = contents.size() > fit ? fit - 1 : contents.size();
            int index = (mouseX - (x + 20)) / ICON_STEP;
            if (index < 0) return null;
            if (index < shown) {
                PortContent item = contents.get(index);
                return List.of(item.name(), Component.literal(CountFormat.grouped(item.amount())).withStyle(ChatFormatting.GRAY));
            }
            if (index == shown && shown < contents.size()) {
                // "+N": list what didn't fit
                var lines = new ArrayList<Component>();
                for (PortContent item : contents.subList(shown, contents.size())) {
                    lines.add(item.name().copy().append(Component.literal("  " + CountFormat.grouped(item.amount())).withStyle(ChatFormatting.GRAY)));
                }
                return lines;
            }
            return null;
        }
        return null;
    }
}
