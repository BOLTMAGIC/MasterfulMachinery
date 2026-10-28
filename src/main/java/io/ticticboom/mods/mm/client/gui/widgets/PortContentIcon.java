package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.FluidSprites;
import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.FluidRenderer;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.PortContent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.List;

public final class PortContentIcon {

    private PortContentIcon() {
    }

    public static void draw(GuiGraphicsExtractor gfx, PortContent content, int slotX, int slotY) {
        int x = slotX + 1;
        int y = slotY + 1;
        switch (content.kind()) {
            case ITEM -> gfx.item(content.item(), x, y);
            case FLUID -> FluidRenderer.INSTANCE.extractRenderState(gfx, x, y, content.fluid(), 16);
            case CHEMICAL -> {
                if (content.sprite() != null) {
                    var sprite = FluidSprites.block(content.sprite());
                    int tint = content.tint();
                    gfx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16, 0xFF000000 | tint);
                } else {
                    gfx.fill(x, y, x + 16, y + 16, 0xFF000000 | content.tint());
                }
            }
            case ENERGY -> gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, x, y, 90, 0, 16, 16, 256, 256);
        }
        if (content.amount() > 1 || content.kind() != PortContent.Kind.ITEM) {
            CountFormat.drawSlotCount(gfx, slotX, slotY, content.amount());
        }
    }

    public static Component name(PortContent content) {
        if (content.kind() == PortContent.Kind.ENERGY) {
            return Component.translatable("gui.mm.port.energy");
        }
        return content.name() != null ? content.name() : Component.translatable("gui.mm.port.tank.empty");
    }

    public static List<Component> tooltip(PortContent content) {
        String unit = content.unit().isEmpty() ? "" : " " + content.unit();
        return List.of(name(content), Component.literal(CountFormat.grouped(content.amount()) + unit).withStyle(ChatFormatting.GRAY));
    }
}
