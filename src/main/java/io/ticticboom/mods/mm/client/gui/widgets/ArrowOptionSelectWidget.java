package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import net.minecraft.client.gui.GuiGraphics;

public class ArrowOptionSelectWidget extends AbstractWidget {
    private final ArrowButtonWidget leftButton;
    private final ArrowButtonWidget rightButton;
    private final TextBarWidget textBar;

    public ArrowOptionSelectWidget(GuiPos pos) {
        super(pos);
        this.leftButton = new ArrowButtonWidget(GuiPos.of(pos.x(), pos.y(), 16, 16), ArrowButtonWidget.ArrowDirection.LEFT);
        this.rightButton = new ArrowButtonWidget(GuiPos.of(pos.x() + pos.w() - 16, pos.y(), 16, 16), ArrowButtonWidget.ArrowDirection.RIGHT);
        this.textBar = new TextBarWidget(GuiPos.of(pos.x(), pos.y() + 6, pos.w() - 32, 16), "Y like jazzzzzz? would you like AMm or FMmmmmmmmmMMMM?");
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        leftButton.render(guiGraphics, mouseX, mouseY, partialTicks);
        rightButton.render(guiGraphics, mouseX, mouseY, partialTicks);
        textBar.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}
