package io.ticticboom.mods.mm.client.gui.widgets.state;

import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.event.ArrowCounterChangeEvent;
import lombok.Getter;

public class ArrowCounterWidgetState {
    private int selectedIndex = 0;
    @Getter
    private final int start;
    @Getter
    private final int increment;
    private final GuiEventHandler<ArrowCounterChangeEvent> changeEmitter;

    public ArrowCounterWidgetState(int start, int increment, GuiEventHandler<ArrowCounterChangeEvent> changeEmitter) {
        this.start = start;
        this.increment = increment;
        this.changeEmitter = changeEmitter;
    }

    public String getSelectedOption() {
        return String.valueOf(selectedIndex);
    }


    public void increment() {
        selectedIndex += increment;
        changeEmitter.fireEvent(new ArrowCounterChangeEvent(selectedIndex));
    }

    public void decrement() {
        selectedIndex -= increment;
        changeEmitter.fireEvent(new ArrowCounterChangeEvent(selectedIndex));
    }
}
