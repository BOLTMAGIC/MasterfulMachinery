package io.ticticboom.mods.mm.util;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class RadiationText {

    private static final String[] UNITS = {"Bq", "kBq", "MBq", "GBq", "TBq", "PBq", "EBq"};

    private RadiationText() {
    }

    public static String bq(double value) {
        double scaled = Math.abs(value);
        int unit = 0;
        while (scaled >= 1000 && unit < UNITS.length - 1) {
            scaled /= 1000;
            unit++;
        }
        String sign = value < 0 ? "-" : "";
        if (scaled >= 1000) {
            return sign + String.format(Locale.ROOT, "%.2e", Math.abs(value)) + " Bq";
        }
        String number;
        if (scaled >= 100) {
            number = String.format(Locale.ROOT, "%.0f", scaled);
        } else if (scaled >= 10) {
            number = String.format(Locale.ROOT, "%.1f", scaled);
        } else {
            number = String.format(Locale.ROOT, "%.2f", scaled);
        }
        if (number.contains(".")) {
            number = number.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return sign + number + " " + UNITS[unit];
    }

    public static Component isotopeName(String id) {
        if (id == null) {
            return Component.translatable("jei.mm.ingredient.nuclear_radiation.any");
        }
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return Component.translatableWithFallback("isotope.nuclear_radiation." + path, readable(path));
    }

    private static String readable(String path) {
        int split = path.lastIndexOf('_');
        if (split <= 0 || split == path.length() - 1) {
            return path;
        }
        String element = path.substring(0, split);
        String mass = path.substring(split + 1);
        return Character.toUpperCase(element.charAt(0)) + element.substring(1) + "-" + mass;
    }
}
