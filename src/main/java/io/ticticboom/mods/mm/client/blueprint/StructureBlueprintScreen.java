package io.ticticboom.mods.mm.client.blueprint;

import io.ticticboom.mods.mm.client.gui.widgets.TilingBackgroundGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StructureBlueprintScreen extends Screen {

    protected StructureBlueprintScreen() {
        super(Component.empty());
        addRenderableWidget(new TilingBackgroundGui(0, 0, 100, 20, 10));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics p_281549_, int mouseX, int mouseY, float partialTicks) {
        super.render(p_281549_, mouseX, mouseY, partialTicks);
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        super.resize(pMinecraft, pWidth, pHeight);
    }
}
