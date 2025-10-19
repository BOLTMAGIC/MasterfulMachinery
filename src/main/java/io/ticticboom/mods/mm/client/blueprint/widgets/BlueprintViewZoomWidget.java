package io.ticticboom.mods.mm.client.blueprint.widgets;

import io.ticticboom.mods.mm.client.blueprint.event.ZoomChangeEvent;
import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.event.ArrowCounterChangeEvent;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.widgets.ArrowCounterWidget;

public class BlueprintViewZoomWidget extends ArrowCounterWidget {
    public final GuiEventHandler<ZoomChangeEvent> changeEmitter = new GuiEventHandler<>();

    public BlueprintViewZoomWidget(GuiPos pos) {
        super(pos, 1, 1, 1, 30);
        super.changeEmitter.addListener(this::onChange);
    }

    private void onChange(ArrowCounterChangeEvent event) {
        this.changeEmitter.fireEvent(new ZoomChangeEvent(event.value()));
    }
}
