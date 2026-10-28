package io.ticticboom.mods.mm.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.function.Supplier;

public final class NameStyles {

    private NameStyles() {
    }

    public static Supplier<Component> gradient(String text, List<Integer> colours) {
        if (colours.isEmpty()) {
            return () -> Component.literal(text);
        }
        Component built = colourPerCharacter(text, index -> sample(colours, progress(text, index)));
        return () -> built;
    }

    public static Supplier<Component> rainbow(String text, double speed, double spread) {
        return () -> {
            double time = System.currentTimeMillis() / 1000.0;
            return colourPerCharacter(text, index -> {
                double hue = time * speed + index * spread;
                return hsvToRgb(hue - Math.floor(hue));
            });
        };
    }

    public static Supplier<Component> cycle(String text, List<Integer> colours, double speed, double spread) {
        if (colours.isEmpty()) {
            return rainbow(text, speed, spread);
        }
        return () -> {
            double time = System.currentTimeMillis() / 1000.0;
            return colourPerCharacter(text, index -> {
                double position = time * speed + index * spread;
                return sampleLooping(colours, position - Math.floor(position));
            });
        };
    }

    private static int sampleLooping(List<Integer> colours, double t) {
        if (colours.size() == 1) {
            return colours.get(0);
        }
        double scaled = t * colours.size();
        int from = Math.min((int) scaled, colours.size() - 1);
        int to = (from + 1) % colours.size();
        return lerp(colours.get(from), colours.get(to), (float) (scaled - from));
    }

    private static Component colourPerCharacter(String text, java.util.function.IntUnaryOperator colourAt) {
        MutableComponent root = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            final int colour = colourAt.applyAsInt(i);
            root.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(style -> style.withColor(TextColor.fromRgb(colour))));
        }
        return root;
    }

    private static float progress(String text, int index) {
        return text.length() <= 1 ? 0 : (float) index / (text.length() - 1);
    }

    private static int sample(List<Integer> colours, float t) {
        if (colours.size() == 1) {
            return colours.get(0);
        }
        float scaled = t * (colours.size() - 1);
        int from = Math.min((int) scaled, colours.size() - 2);
        return lerp(colours.get(from), colours.get(from + 1), scaled - from);
    }

    private static int lerp(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int hsvToRgb(double hue) {
        double h = hue * 6;
        int sector = (int) Math.floor(h) % 6;
        double f = h - Math.floor(h);
        int full = 255;
        int rising = (int) Math.round(f * 255);
        int falling = 255 - rising;
        return switch (sector) {
            case 0 -> (full << 16) | (rising << 8);
            case 1 -> (falling << 16) | (full << 8);
            case 2 -> (full << 8) | rising;
            case 3 -> (falling << 8) | full;
            case 4 -> (rising << 16) | full;
            default -> (full << 16) | falling;
        };
    }
}
