package io.ticticboom.mods.mm.client;

import io.ticticboom.mods.mm.client.util.InputEvents;
import net.minecraft.client.input.MouseButtonEvent;
import com.google.common.collect.Queues;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Queue;

public abstract class MMAbstractScreen extends Screen {

    public Queue<Runnable> tickCallbacks = Queues.newArrayDeque();
    protected MMAbstractScreen() {
        super(Component.empty());
    }

    protected void schedule(Runnable runnable) {
        tickCallbacks.add(runnable);
    }

    @Override
    public void tick() {
        runScheduledActions();

    }

    private void runScheduledActions() {
        if (tickCallbacks.isEmpty()) return;
        for (int i = 0; i < tickCallbacks.size(); i++) {
            Runnable runnable = tickCallbacks.poll();
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double pMouseX = event.x();
        double pMouseY = event.y();
        int pButton = event.button();
        for(GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener.mouseClicked(InputEvents.mouse(pMouseX, pMouseY, pButton), false)) {
                this.setFocused(guieventlistener);
                if (pButton == 0) {
                    this.setDragging(true);
                }
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double pMouseX = event.x();
        double pMouseY = event.y();
        int pButton = event.button();
        this.setDragging(false);
        for (GuiEventListener child : children()) {
            child.mouseReleased(InputEvents.mouse(pMouseX, pMouseY, pButton));
        }
        return true;
    }
}
