package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.FluidRenderer;
import io.ticticboom.mods.mm.client.util.CountFormat;
import io.ticticboom.mods.mm.port.PortContent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.List;

/**
 * Draws a {@link PortContent} as a 16x16 icon with its amount in the corner, like an item in a slot:
 * items as themselves, fluids and chemicals as their texture, energy as MM's energy bar texture.
 * Coordinates are absolute screen coordinates of the slot (18x18, icon inset by one pixel).
 */
public final class PortContentIcon {

    private PortContentIcon() {
    }

    public static void draw(GuiGraphics gfx, PortContent content, int slotX, int slotY) {
        int x = slotX + 1;
        int y = slotY + 1;
        switch (content.kind()) {
            case ITEM -> gfx.renderItem(content.item(), x, y);
            case FLUID -> FluidRenderer.INSTANCE.render(gfx, x, y, content.fluid(), 16);
            case CHEMICAL -> {
                if (content.sprite() != null) {
                    TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(content.sprite());
                    int tint = content.tint();
                    gfx.setColor((tint >> 16 & 0xFF) / 255f, (tint >> 8 & 0xFF) / 255f, (tint & 0xFF) / 255f, 1);
                    gfx.blit(x, y, 0, 16, 16, sprite);
                    gfx.setColor(1, 1, 1, 1);
                }
            }
            case ENERGY -> gfx.blit(Ref.UiTextures.SLOT_PARTS, x, y, 90, 0, 16, 16);
        }
        if (content.amount() > 1 || content.kind() != PortContent.Kind.ITEM) {
            CountFormat.drawSlotCount(gfx, slotX, slotY, content.amount());
        }
    }

    public static List<Component> tooltip(PortContent content) {
        Component name = content.kind() == PortContent.Kind.ENERGY ? Component.translatable("gui.mm.port.energy")
                : content.name() != null ? content.name() : Component.translatable("gui.mm.port.tank.empty");
        String unit = content.unit().isEmpty() ? "" : " " + content.unit();
        return List.of(name, Component.literal(CountFormat.grouped(content.amount()) + unit).withStyle(ChatFormatting.GRAY));
    }
}
