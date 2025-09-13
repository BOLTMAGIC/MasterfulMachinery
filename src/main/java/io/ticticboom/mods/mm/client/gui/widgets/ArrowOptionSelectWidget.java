package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.event.ArrowOptionSelectChangeEvent;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.widgets.state.ArrowOptionSelectWidgetState;

import java.util.List;

public class ArrowOptionSelectWidget extends AbstractWidget {
    protected final ArrowButtonWidget leftButton;
    protected final ArrowButtonWidget rightButton;
    protected final TextBarWidget textBar;

    protected final ArrowOptionSelectWidgetState state;
    public final GuiEventHandler<ArrowOptionSelectChangeEvent> changeEmitter = new GuiEventHandler<>();

    public ArrowOptionSelectWidget(GuiPos pos, List<String> options) {
        super(pos);
        this.state = new ArrowOptionSelectWidgetState(options, changeEmitter);
        this.leftButton = addWidget(new ArrowButtonWidget(GuiPos.of(pos.x(), pos.y(), 16, 16), ArrowButtonWidget.ArrowDirection.LEFT));
        this.rightButton = addWidget(new ArrowButtonWidget(GuiPos.of(pos.x() + pos.w() - 16, pos.y(), 16, 16), ArrowButtonWidget.ArrowDirection.RIGHT));
        this.textBar = addWidget(new TextBarWidget(GuiPos.of(pos.x() + 18, pos.y() + 6, pos.w() - 36, 16), state::getSelectedOption));

        this.leftButton.clickEmitter.addListener(e -> state.previous());
        this.rightButton.clickEmitter.addListener(e -> state.next());
    }
}
