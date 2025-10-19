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
    private final int min;
    private final int max;
    private final GuiEventHandler<ArrowCounterChangeEvent> changeEmitter;

    public ArrowCounterWidgetState(int start, int increment, int min, int max, GuiEventHandler<ArrowCounterChangeEvent> changeEmitter) {
        this.start = start;
        this.increment = increment;
        this.min = min;
        this.max = max;
        this.selectedIndex = start;
        this.changeEmitter = changeEmitter;
    }

    public String getSelectedOption() {
        return String.valueOf(selectedIndex);
    }


    public void increment() {
        selectedIndex += increment;
        if (selectedIndex >= max) {
            selectedIndex = max;
        }
        changeEmitter.fireEvent(new ArrowCounterChangeEvent(selectedIndex));
    }

    public void decrement() {
        selectedIndex -= increment;
        if (selectedIndex <= min) {
            selectedIndex = min;
        }
        changeEmitter.fireEvent(new ArrowCounterChangeEvent(selectedIndex));
    }
}
