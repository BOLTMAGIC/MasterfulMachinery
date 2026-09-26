package io.ticticboom.mods.mm.client.tool;

import com.mojang.blaze3d.systems.RenderSystem;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Placeholder screen for {@link MultiblockToolMenu}, just enough for the menu to open without
 * crashing. Reuses vanilla's generic 6-row chest background (same slot layout: 54 store slots + the
 * player's own inventory). Task 6 replaces this with the tool's real gallery/settings screen.
 */
public class MultiblockToolScreen extends AbstractContainerScreen<MultiblockToolMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("textures/gui/container/generic_54.png");

    public MultiblockToolScreen(MultiblockToolMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(@org.jetbrains.annotations.NotNull GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        gfx.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(@org.jetbrains.annotations.NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }
}
