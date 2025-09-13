package io.ticticboom.mods.mm.client.gui.widgets.state;

import io.ticticboom.mods.mm.util.MutableLazy;

import java.util.List;

public class ArrowOptionSelectWidgetState {
    private int selectedIndex = 0;
    private final int optionsSize;
    private final MutableLazy<String> selectedOption;
    public ArrowOptionSelectWidgetState(List<String> options) {
        this.optionsSize = options.size();
        selectedOption = new MutableLazy<>(() -> options.get(selectedIndex));
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
    }

    public void next() {
        change(1);
    }

    public void previous() {
        change(-1);
    }
}
