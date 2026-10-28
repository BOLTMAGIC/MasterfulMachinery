package io.ticticboom.mods.mm.port.replication.matter.register;

import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.client.util.TextColors;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.replication.matter.MatterTypes;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortStorage;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortTank;
import io.ticticboom.mods.mm.util.WidgetUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;

public class ReplicationMatterPortScreen extends AbstractContainerScreen<ReplicationMatterPortMenu> {

    private static final int AREA_X = 8;
    private static final int AREA_Y = 51;
    private static final int AREA_WIDTH = 160;
    private static final int AREA_HEIGHT = 78;
    private static final int SEPARATOR_COLOR = 0xFF373737;
    private static final int RESERVED_ALPHA = 0x38000000;
    private static final int HOVER_COLOR = 0x30FFFFFF;

    private final FormattedText header;

    public ReplicationMatterPortScreen(ReplicationMatterPortMenu menu, Inventory inv, Component displayName) {
        super(menu, inv, displayName);
        this.imageHeight = 222;
        this.imageWidth = 174;
        String name = menu.getModel().displayName().getString();
        int subStrLength = Math.min(55, name.length());
        header = FormattedText.of(name.substring(0, subStrLength) + (subStrLength < 55 ? "" : "..."));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(gfx, mouseX, mouseY, partialTicks);
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        gfx.textWithWordWrap(this.font, header, 8, 8, 150, TextColors.argb(0x404040));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        extractTooltip(gfx, mouseX, mouseY);
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, this.leftPos + 7, this.topPos + 50, 89, 78, 162, 80, 256, 256);

        ReplicationMatterPortBlockEntity be = menu.getBlockEntity();
        var storage = (ReplicationMatterPortStorage) be.getStorage();
        var tanks = storage.getHandler().tanks();
        if (tanks.isEmpty()) {
            return;
        }
        int count = tanks.size();
        int top = this.topPos + AREA_Y;
        int bottom = top + AREA_HEIGHT;
        ReplicationMatterPortTank hovered = null;
        int hoveredLeft = 0;
        int hoveredRight = 0;

        for (int i = 0; i < count; i++) {
            var tank = tanks.get(i);
            int left = columnLeft(i, count);
            int right = columnLeft(i + 1, count);
            var reserved = tank.getRequestedType();
            if (reserved != null) {
                gfx.fill(left, top, right, bottom, (MatterTypes.colorOf(reserved) & 0x00FFFFFF) | RESERVED_ALPHA);
            }
            var filled = (int) (Math.min(tank.getMatterAmount() / tank.getCapacity(), 1) * AREA_HEIGHT);
            if (filled > 0) {
                gfx.fill(left, bottom - filled, right, bottom, MatterTypes.colorOf(tank.getMatter().getMatterType()));
            }
            if (WidgetUtils.isPointerWithinSized(mouseX, mouseY, left, top, right - left, AREA_HEIGHT)) {
                hovered = tank;
                hoveredLeft = left;
                hoveredRight = right;
            }
        }

        for (int i = 1; i < count; i++) {
            int boundary = columnLeft(i, count);
            gfx.fill(boundary, top, boundary + 1, bottom, SEPARATOR_COLOR);
        }

        if (hovered != null) {
            gfx.fill(hoveredLeft, top, hoveredRight, bottom, HOVER_COLOR);
            gfx.setComponentTooltipForNextFrame(this.font, tooltip(hovered), mouseX, mouseY);
        }
    }

    private int columnLeft(int index, int count) {
        return this.leftPos + AREA_X + (AREA_WIDTH * index / count);
    }

    private ArrayList<Component> tooltip(ReplicationMatterPortTank tank) {
        var tooltip = new ArrayList<Component>();
        if (tank.getMatter().isEmpty()) {
            var filter = tank.getFilter();
            tooltip.add(filter == null
                    ? Component.translatable("gui.mm.port.replication_matter.empty")
                    : Component.translatable("gui.mm.port.replication_matter.reserved", filter.toString()));
        } else {
            tooltip.add(tank.getMatter().getDisplayName());
        }
        tooltip.add(Component.translatable("gui.mm.port.replication_matter.storage",
                (int) Math.floor(tank.getMatterAmount()), (int) tank.getCapacity()));
        return tooltip;
    }
}
