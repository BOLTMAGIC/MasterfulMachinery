package io.ticticboom.mods.mm.util;

import org.jetbrains.annotations.Nullable;

public final class ColorUtil {

    private ColorUtil() {
    }

    @Nullable
    public static Integer parse(@Nullable String value) {
        if (value == null) return null;
        var hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6) return null;
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
