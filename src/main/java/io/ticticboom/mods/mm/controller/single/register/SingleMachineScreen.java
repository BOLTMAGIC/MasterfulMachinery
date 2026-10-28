package io.ticticboom.mods.mm.controller.single.register;

import io.ticticboom.mods.mm.client.FluidSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.client.util.TextColors;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortContentIcon;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.client.util.NineSliceUtil;
import io.ticticboom.mods.mm.net.packet.OpenMachineScreenPkt;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SingleMachineScreen extends AbstractContainerScreen<SingleMachineMenu> {

    private static final int TEXT = 0xDDDDDD;
    private static final int DIVIDER = 0xFF3A3A3A;
    private static final int PANEL = 0xFF1C1C1C;
    private static final int GREY = 0xFFC6C6C6;
    private static final int FADE = 0xB08B8B8B;
    private static final int SHADOW = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int TANK = 0xFF8B8B8B;
    private static final int MARK = 0x66373737;
    private static final int ENERGY = 0xFFC8342C;
    private static final int OTHER = 0xFF5B8DD9;
    private static final int RUNNING = 0x55FF55;
    private static final int BUTTON = 12;
    private static final int BUTTON_Y = 8;
    private static final Map<String, Integer> STATUS_COLORS = Map.of(
            "running", 0x55FF55, "idle", 0xE0C050, "stalled", 0xFF8844, "paused", 0xFFAA00, "not_formed", 0xFF5555);

    public SingleMachineScreen(SingleMachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = menu.getLayout().width();
        this.imageHeight = menu.getLayout().height();
    }

    private SingleMachineLayout layout() {
        return menu.getLayout();
    }

    private SingleMachineBlockEntity machine() {
        return menu.getMachine();
    }

    private int right() {
        return imageWidth - SingleMachineLayout.RIGHT_MARGIN;
    }

    private int buttonX() {
        return right() - BUTTON;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        var layout = layout();
        int x = leftPos;
        int y = topPos;
        int frame = SingleMachineLayout.FRAME;
        NineSliceUtil.blitNineSliced(gfx, Ref.UiTextures.GUI_LARGE, x, y, imageWidth, imageHeight, frame, frame, frame, 7, 174, 222, 0, 0, 256, 256);
        gfx.fill(x + frame, y + frame, x + imageWidth - frame, y + imageHeight - 7, GREY);
        NineSliceUtil.blitNineSliced(gfx, Ref.UiTextures.GUI_LARGE, x + frame, y + frame, imageWidth - 2 * frame, layout.panelBottom() - frame + 1,
                2, 2, 2, 2, 162, 121, 6, 6, 256, 256);
        gfx.fill(x + frame + 2, y + frame + 2, x + imageWidth - frame - 2, y + layout.panelBottom() - 1, PANEL);
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.GUI_LARGE, x + layout.inventoryX(), y + layout.inventoryY(), 6, 139, SingleMachineLayout.INVENTORY_W, SingleMachineLayout.INVENTORY_H, 256, 256);

        int left = x + SingleMachineLayout.LEFT;
        gfx.fill(left, y + SingleMachineLayout.STATUS_Y + 1, left + 5, y + SingleMachineLayout.STATUS_Y + 6, 0xFF000000 | statusColor());
        gfx.fill(left, y + SingleMachineLayout.STATUS_Y + 11, x + right(), y + SingleMachineLayout.STATUS_Y + 12, DIVIDER);

        for (var group : layout.groups()) {
            if (group.items()) {
                for (int cell = 0; cell < group.cells(); cell++) {
                    gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, x + group.cellX(cell), y + group.cellY(cell), 0, 26, 18, 18, 256, 256);
                }
                continue;
            }
            var contents = group.storage().contents();
            for (int gauge = 0; gauge < group.gauges(); gauge++) {
                drawTank(gfx, x + group.gaugeX(gauge), y + group.y(), group.gaugeH(), gauge < contents.size() ? contents.get(gauge) : null,
                        group.storage().fillRatio());
            }
        }

        int bx0 = x + layout.barX();
        int by0 = y + layout.barY();
        gfx.fill(bx0, by0, bx0 + SingleMachineLayout.BAR_W, by0 + SingleMachineLayout.BAR_H, 0xFF555555);
        gfx.fill(bx0 + 1, by0 + 1, bx0 + SingleMachineLayout.BAR_W - 1, by0 + SingleMachineLayout.BAR_H - 1, 0xFF111111);
        gfx.fill(bx0 + 1, by0 + 1, bx0 + 1 + (int) Math.round((SingleMachineLayout.BAR_W - 2) * progress()), by0 + SingleMachineLayout.BAR_H - 1,
                0xFF000000 | RUNNING);

        int bx = x + buttonX();
        int bty = y + BUTTON_Y;
        var texture = isOnButton(mouseX, mouseY) ? Ref.UiTextures.BUTTON_PRESSED : Ref.UiTextures.BUTTON_ACTIVE;
        NineSliceUtil.blitNineSliced(gfx, texture, bx, bty, BUTTON, BUTTON, 2, 2, 2, 2, 16, 16, 0, 0, 16, 16);
        var pose = gfx.pose();
        pose.pushMatrix();
        pose.translate(bx + 1, bty + 1);
        pose.scale(10 / 16f, 10 / 16f);
        gfx.item(new ItemStack(Items.COMPARATOR), 0, 0);
        pose.popMatrix();
    }

    private void drawTank(GuiGraphicsExtractor gfx, int x, int y, int h, @Nullable PortContent content, double ratio) {
        int w = SingleMachineLayout.GAUGE_W;
        gfx.fill(x, y, x + w - 1, y + 1, SHADOW);
        gfx.fill(x, y, x + 1, y + h - 1, SHADOW);
        gfx.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        gfx.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
        int left = x + 1;
        int right = x + w - 1;
        int top = y + 1;
        int bottom = y + h - 1;
        gfx.fill(left, top, right, bottom, TANK);
        double fraction = content != null && content.capacity() > 0 ? (double) content.amount() / content.capacity() : ratio;
        int fill = (int) Math.round((bottom - top) * Math.max(0, Math.min(1, fraction)));
        if (fill > 0) {
            fillCell(gfx, left, bottom - fill, right, bottom, content);
        } else {
            var icon = emptyIcon(content);
            if (icon != null) {
                int size = right - left - 2;
                var pose = gfx.pose();
                pose.pushMatrix();
                pose.translate(left + 1, top + (bottom - top - size) / 2f);
                pose.scale(size / 16f, size / 16f);
                gfx.item(new ItemStack(icon), 0, 0);
                pose.popMatrix();
                pose.pushMatrix();
                pose.translate(0, 0);
                gfx.fill(left, top, right, bottom, FADE);
                pose.popMatrix();
            }
        }
        for (int quarter = 1; quarter < 4; quarter++) {
            int markY = bottom - (bottom - top) * quarter / 4;
            gfx.fill(left, markY, left + (quarter == 2 ? 5 : 3), markY + 1, MARK);
        }
    }

    @Nullable
    private static Item emptyIcon(@Nullable PortContent content) {
        if (content == null) return null;
        return switch (content.kind()) {
            case FLUID -> Items.BUCKET;
            case ENERGY -> Items.REDSTONE;
            case CHEMICAL -> Items.GLASS_BOTTLE;
            case ITEM -> null;
        };
    }

    private void fillCell(GuiGraphicsExtractor gfx, int left, int top, int right, int bottom, @Nullable PortContent content) {
        if (content == null || content.kind() == PortContent.Kind.ENERGY || content.kind() == PortContent.Kind.ITEM) {
            gfx.fill(left, top, right, bottom, content == null ? OTHER : ENERGY);
            return;
        }
        TextureAtlasSprite sprite = null;
        int tint = 0xFFFFFFFF;
        if (content.kind() == PortContent.Kind.FLUID && !content.fluid().isEmpty()) {
            sprite = FluidSprites.still(content.fluid());
            tint = FluidSprites.tint(content.fluid());
        } else if (content.kind() == PortContent.Kind.CHEMICAL && content.sprite() != null) {
            sprite = FluidSprites.block(content.sprite());
            tint = content.tint();
        }
        if (sprite == null) {
            gfx.fill(left, top, right, bottom, OTHER);
            return;
        }
        gfx.enableScissor(left, top, right, bottom);
        for (int ty = bottom - 16; ty > top - 16; ty -= 16) {
            gfx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, left, ty, 16, 16, 0xFF000000 | tint);
        }
        gfx.disableScissor();
    }

    private double progress() {
        var state = machine().getRecipeState();
        return state == null ? 0 : Math.max(0, Math.min(100, state.getTickPercentage())) / 100;
    }

    private String statusKey() {
        var machine = machine();
        if (machine.getStructure() == null || !machine.isFormed()) return "not_formed";
        if (!machine.isAllowedByRedstone()) return "paused";
        if (machine.isWorking()) return "running";
        if (machine.getActiveRecipeCount() > 0) return "stalled";
        return "idle";
    }

    private int statusColor() {
        return STATUS_COLORS.getOrDefault(statusKey(), 0xAAAAAA);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        var layout = layout();
        int left = SingleMachineLayout.LEFT;
        if (machine().getRecipeState() != null) {
            var percent = Component.literal((int) Math.floor(progress() * 100) + "%");
            gfx.text(font, percent, layout.barX() + SingleMachineLayout.BAR_W / 2 - font.width(percent) / 2, layout.barY() + SingleMachineLayout.BAR_H + 3, TextColors.argb(TEXT), false);
        }
        drawClipped(gfx, machine().getName(), left, SingleMachineLayout.NAME_Y, buttonX() - left - 4, 0xFFFFFF);
        drawClipped(gfx, Component.translatable("gui.mm.controller.status." + statusKey()), left + 9, SingleMachineLayout.STATUS_Y,
                right() - left - 9, statusColor());

    }

    private void drawClipped(GuiGraphicsExtractor gfx, Component text, int x, int y, int maxWidth, int color) {
        var clipped = font.ellipsize(text, Math.max(0, maxWidth));
        gfx.text(font, Language.getInstance().getVisualOrder(clipped), x, y, TextColors.argb(color), false);
    }

    private boolean isOnButton(double mouseX, double mouseY) {
        return WidgetUtils.isPointerWithinSized((int) mouseX, (int) mouseY, leftPos + buttonX(), topPos + BUTTON_Y, BUTTON, BUTTON);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (isOnButton(mouseX, mouseY)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
            ClientPacketDistributor.sendToServer(new OpenMachineScreenPkt(machine().getBlockPos(), true));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        extractTooltip(gfx, mouseX, mouseY);
        List<Component> tooltip = null;
        if (isOnButton(mouseX, mouseY)) {
            tooltip = List.of(Component.translatable("gui.mm.single.controller"));
        } else if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, leftPos + SingleMachineLayout.LEFT, topPos + SingleMachineLayout.STATUS_Y - 1,
                right() - SingleMachineLayout.LEFT, 10)) {
            tooltip = List.of(Component.translatable("gui.mm.controller.status." + statusKey()),
                    Component.translatable("gui.mm.controller.status." + statusKey() + ".hint").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip = cellTooltip(mouseX, mouseY);
        }
        if (tooltip != null) {
            gfx.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    @Nullable
    private List<Component> cellTooltip(int mouseX, int mouseY) {
        for (var group : layout().groups()) {
            int count = group.cells();
            for (int cell = 0; cell < count; cell++) {
                boolean hovered = group.items()
                        ? WidgetUtils.isPointerWithinSized(mouseX, mouseY, leftPos + group.cellX(cell), topPos + group.cellY(cell), SingleMachineLayout.CELL, SingleMachineLayout.CELL)
                        : WidgetUtils.isPointerWithinSized(mouseX, mouseY, leftPos + group.gaugeX(cell), topPos + group.y(), SingleMachineLayout.GAUGE_W, group.gaugeH());
                if (!hovered) {
                    continue;
                }
                if (group.items() && hoveredSlot != null && hoveredSlot.hasItem()) {
                    return null;
                }
                var lines = new ArrayList<Component>();
                lines.add(slotName(group));
                if (group.items()) {
                    lines.add(Component.translatable(group.input() ? "gui.mm.single.input" : "gui.mm.single.output").withStyle(ChatFormatting.GRAY));
                    return lines;
                }
                var contents = group.storage().contents();
                if (cell < contents.size()) {
                    var content = contents.get(cell);
                    String unit = content.unit().isEmpty() ? "" : " " + content.unit();
                    lines.add(PortContentIcon.name(content).copy().withStyle(ChatFormatting.GRAY));
                    lines.add(Component.literal(CountFormat.grouped(content.amount()) + " / " + CountFormat.grouped(content.capacity()) + unit)
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    group.storage().describeContents().forEach(line -> lines.add(line.copy().withStyle(ChatFormatting.GRAY)));
                }
                return lines;
            }
        }
        return null;
    }

    private Component slotName(SingleMachineLayout.Group group) {
        String id = machine().getSlots().get(group.index()).id().replace('_', ' ');
        return Component.literal(id.isEmpty() ? id : Character.toUpperCase(id.charAt(0)) + id.substring(1));
    }
}
