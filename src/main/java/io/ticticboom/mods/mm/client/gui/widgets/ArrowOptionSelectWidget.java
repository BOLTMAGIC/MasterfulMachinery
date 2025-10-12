package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.event.ArrowOptionSelectChangeEvent;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.widgets.state.ArrowOptionSelectWidgetState;

import java.util.List;

public class ArrowOptionSelectWidget extends ArrowSelectWidget {
    public static final int DEFAULT_HEIGHT = 16;
    protected final List<String> options;

    protected final ArrowOptionSelectWidgetState state;
    public final GuiEventHandler<ArrowOptionSelectChangeEvent> changeEmitter = new GuiEventHandler<>();

    public ArrowOptionSelectWidget(GuiPos pos, List<String> options) {
        super(pos);
        this.options = options;
        this.state = new ArrowOptionSelectWidgetState(options, changeEmitter);
        setupElements(state::getSelectedOption);

        this.leftButton.clickEmitter.addListener(e -> state.previous());
        this.rightButton.clickEmitter.addListener(e -> state.next());
    }
}
