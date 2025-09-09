package io.ticticboom.mods.mm.client.gui;

import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public abstract class AbstractWidget implements IWidget, GuiEventListener, NarratableEntry {

    @Getter
    @Setter
    protected GuiPos position;

    protected boolean focused = false;
    protected Minecraft mc = Minecraft.getInstance();

    public AbstractWidget(GuiPos pos) {
        this.position = pos;
    }

    @Override
    public void setFocused(boolean pFocused) {
        focused = pFocused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }
}