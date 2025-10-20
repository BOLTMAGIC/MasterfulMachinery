package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;

import java.util.function.Supplier;

public class ArrowSelectWidget extends AbstractWidget {

    protected ArrowButtonWidget leftButton;
    protected ArrowButtonWidget rightButton;
    protected TextBarWidget textBar;

    public ArrowSelectWidget(GuiPos pos) {
        super(pos);
    }

    protected void setupElements(Supplier<String> text) {
        this.leftButton = addWidget(new ArrowButtonWidget(GuiPos.of(this.position.x(), this.position.y(), 16, 16), ArrowButtonWidget.ArrowDirection.LEFT));
        this.rightButton = addWidget(new ArrowButtonWidget(GuiPos.of(this.position.x() + this.position.w() - 16, this.position.y(), 16, 16), ArrowButtonWidget.ArrowDirection.RIGHT));
        this.textBar = addWidget(new TextBarWidget(GuiPos.of(this.position.x() + 18, this.position.y() + 6, this.position.w() - 36, 16), text));
    }
}
