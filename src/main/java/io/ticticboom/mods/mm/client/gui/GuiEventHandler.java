package io.ticticboom.mods.mm.client.gui;

import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.function.Consumer;

public class GuiEventHandler<EVENT> {
    private final List<Consumer<EVENT>> listeners = Lists.newArrayList();

    public void addListener(Consumer<EVENT> listener) {
        listeners.add(listener);
    }

//    public void removeListener(Consumer<EVENT> listener) {
//        listeners.remove(listener);
//    }

    public void fireEvent(EVENT event) {
        listeners.forEach(l -> l.accept(event));
    }
}
