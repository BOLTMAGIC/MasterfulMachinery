package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.util.ValueChangeProcessor;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;


public class TextBarWidget extends AbstractWidget {
    private final ValueChangeProcessor<String> textSupplier;
    private int textX;
    private final int centerX;
    private final int overflowSuffixWidth = mc.font.width("...");
    private final int maxTextWidth;

    public TextBarWidget(GuiPos pos, Supplier<String> textSupplier) {
        super(pos);
        this.textSupplier = ValueChangeProcessor.create(textSupplier, this::updateText);
        maxTextWidth = this.position.w() - 12;
        centerX = this.position.x() + this.position.w() / 2;
    }

    private String updateText(String newText) {
        var processedText = newText;
        int textWidth = mc.font.width(processedText);
        textX = centerX - textWidth / 2;
        if (textWidth > maxTextWidth) {
            processedText = mc.font.plainSubstrByWidth(newText, maxTextWidth + overflowSuffixWidth) + "...";
            textWidth = mc.font.width(processedText);
        }
        textX = centerX - textWidth / 2;
        return processedText;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        String text = this.textSupplier.get();
        guiGraphics.drawString(mc.font, text, this.textX, this.position.y(), 0x474747, false);
    }
}
