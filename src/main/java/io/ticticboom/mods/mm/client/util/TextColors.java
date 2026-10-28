package io.ticticboom.mods.mm.client.util;

/**
 * Text colours are ARGB since 1.21.6, and text whose alpha is 0 is not drawn at all. MM's screens were written with
 * RGB colours like 0x404040, which the old font treated as opaque; this keeps them opaque.
 */
public final class TextColors {
    private TextColors() {
    }

    public static int argb(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }
}
