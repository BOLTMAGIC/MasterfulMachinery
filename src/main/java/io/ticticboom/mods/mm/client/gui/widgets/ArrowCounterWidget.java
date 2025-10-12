package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.event.ArrowCounterChangeEvent;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.widgets.state.ArrowCounterWidgetState;

public class ArrowCounterWidget extends ArrowSelectWidget {
    private final ArrowCounterWidgetState state;
    public final GuiEventHandler<ArrowCounterChangeEvent> changeEmitter = new GuiEventHandler<>();

    public ArrowCounterWidget(GuiPos pos, int start, int increment) {
        super(pos);
        state = new ArrowCounterWidgetState(start, increment, changeEmitter);
        setupElements(state::getSelectedOption);

        this.leftButton.clickEmitter.addListener(e -> state.increment());
        this.rightButton.clickEmitter.addListener(e -> state.decrement());
    }
}
