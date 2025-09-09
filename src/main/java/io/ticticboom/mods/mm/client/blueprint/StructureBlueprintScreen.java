package io.ticticboom.mods.mm.client.blueprint;

import io.ticticboom.mods.mm.client.gui.util.GuiAlignment;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.util.GuiPosHelper;
import io.ticticboom.mods.mm.client.gui.widgets.ArrowOptionSelectWidget;
import io.ticticboom.mods.mm.client.gui.widgets.TilingBackgroundGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StructureBlueprintScreen extends Screen {

    private GuiPosHelper guiHelper;

    protected StructureBlueprintScreen() {
        super(Component.empty());

    }

    @Override
    protected void init() {
        this.guiHelper = new GuiPosHelper(this.width, this.height, 10);
        addRenderableOnly(new TilingBackgroundGui(guiHelper.getGuiPos()));

        addRenderableWidget(new ArrowOptionSelectWidget(
                guiHelper.offset(
                        GuiAlignment.LEFT_TOP,
                        GuiPos.of(5, 5, guiHelper.getGuiWidth() - 10, 20))));

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
