package io.ticticboom.mods.mm.client.gui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public abstract class AbstractWidget implements IWidget, GuiEventListener, NarratableEntry {

    @Getter
    @Setter
    protected int xPos = 0;
    @Setter
    @Getter
    protected int yPos = 0;

}