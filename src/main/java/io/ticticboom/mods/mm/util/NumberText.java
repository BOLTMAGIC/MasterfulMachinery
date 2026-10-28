package io.ticticboom.mods.mm.util;

import java.util.Locale;

public final class NumberText {

    private NumberText() {
    }

    public static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
