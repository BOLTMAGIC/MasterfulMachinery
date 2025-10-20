package io.ticticboom.mods.mm.util;

import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {
    private Supplier<T> supplier;

    public Lazy(Supplier<T> supplier) {
        this.supplier = () -> {
            var val = supplier.get();
            this.supplier = () -> val;
            return val;
        };
    }

    @Override
    public T get() {
        return supplier.get();
    }

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }
}
