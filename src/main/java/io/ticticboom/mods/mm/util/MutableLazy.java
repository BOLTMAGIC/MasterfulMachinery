package io.ticticboom.mods.mm.util;

import java.util.function.Supplier;

public class MutableLazy<T> implements Supplier<T> {
    private T value;
    private boolean changed = true;
    private final Supplier<T> factory;

    public MutableLazy(Supplier<T> factory) {
        this.factory = factory;
    }

    @Override
    public T get() {
        if (changed) {
            value = factory.get();
            changed = false;
            return value;
        }
        return value;
    }

    public void invalidate() {
        changed = true;
    }
}
