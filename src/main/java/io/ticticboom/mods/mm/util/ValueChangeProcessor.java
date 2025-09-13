package io.ticticboom.mods.mm.util;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class ValueChangeProcessor<T> {
    private final Supplier<T> supplier;
    private final Function<T, T> onChange;
    private final BiPredicate<T, T> equality;
    private T value;


    public static <T> ValueChangeProcessor<T> create(Supplier<T> supplier, Function<T, T> onChange) {
        return new ValueChangeProcessor<>(supplier, onChange, Object::equals);
    }

    public ValueChangeProcessor(Supplier<T> supplier, Function<T, T> onChange, BiPredicate<T, T> equality) {
        this.supplier = supplier;
        this.onChange = onChange;
        this.equality = equality;
    }

    public T get() {
        var supplied = supplier.get();
        if (!isEqual(supplied, this.value)) {
            this.value = onChange.apply(supplied);
        }
        return this.value;
    }

    private boolean isEqual(T a, T b) {
        if (a == null && b == null) {
            return true;
        }
        else if (a == null || b == null) {
            return false;
        }

        return equality.test(a, b);
    }
}
