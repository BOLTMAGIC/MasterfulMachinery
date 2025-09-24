package io.ticticboom.mods.mm.client;

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
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        for(GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener.mouseClicked(pMouseX, pMouseY, pButton)) {
                this.setFocused(guieventlistener);
                if (pButton == 0) {
                    this.setDragging(true);
                }
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        this.setDragging(false);
        for (GuiEventListener child : children()) {
            child.mouseReleased(pMouseX, pMouseY, pButton);
        }
        return true;
    }
}
