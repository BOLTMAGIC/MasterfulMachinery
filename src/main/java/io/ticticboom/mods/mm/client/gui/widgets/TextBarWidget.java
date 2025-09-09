package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import net.minecraft.client.gui.GuiGraphics;

public class TextBarWidget extends AbstractWidget {

    private final String text;
    private final int textWidth;
    private int textX;

    public TextBarWidget(GuiPos pos, String text) {
        super(pos);
        this.text = text;
        this.textWidth = mc.font.width(text);
        textX = pos.x() + textWidth / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.drawString(mc.font, text, this.textX, this.position.y(), 0x474747, false);
    }
}
