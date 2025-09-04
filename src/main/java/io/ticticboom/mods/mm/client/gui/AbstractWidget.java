package io.ticticboom.mods.mm.client.gui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public abstract class AbstractWidget implements IWidget, GuiEventListener, NarratableEntry {

    @Getter
    @Setter
    protected int xPos = 0;
    @Setter
    @Getter
    protected int yPos = 0;



    @Override
    public void setFocused(boolean pFocused) {

    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }
}