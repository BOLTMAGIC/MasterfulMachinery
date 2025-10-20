package io.ticticboom.mods.mm.client.gui.widgets.state;

import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.event.ArrowOptionSelectChangeEvent;
import io.ticticboom.mods.mm.util.MutableLazy;

import java.util.List;

public class ArrowOptionSelectWidgetState {
    private int selectedIndex = 0;
    private final int optionsSize;
    private final MutableLazy<String> selectedOption;
    private final GuiEventHandler<ArrowOptionSelectChangeEvent> changeEmitter;

    public ArrowOptionSelectWidgetState(List<String> options, GuiEventHandler<ArrowOptionSelectChangeEvent> changeEmitter) {
        this.optionsSize = options.size();
        selectedOption = new MutableLazy<>(() -> options.get(selectedIndex));
        this.changeEmitter = changeEmitter;
    }

    public String getSelectedOption() {
        return selectedOption.get();
    }

    public void change(int change) {
        selectedIndex += change;
        if (selectedIndex < 0) {
            selectedIndex = optionsSize - 1;
        }
        if (selectedIndex >= optionsSize) {
            selectedIndex = 0;
        }
        selectedOption.invalidate();
        changeEmitter.fireEvent(new ArrowOptionSelectChangeEvent(selectedIndex));
    }

    public void next() {
        change(1);
    }

    public void previous() {
        change(-1);
    }
}
