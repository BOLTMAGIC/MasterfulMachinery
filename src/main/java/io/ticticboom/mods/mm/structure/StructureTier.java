package io.ticticboom.mods.mm.structure;

import java.util.Locale;
import java.util.regex.Pattern;

public final class StructureTier {
    private static final Pattern IN_NAME = Pattern.compile("(?i)\\b(?:tier|level|lvl|mk)\\s*[.:#-]?\\s*(\\d+(?:[.,]\\d+)?|[ivx]+)\\b");

    private StructureTier() {
    }

    public static double of(StructureModel structure) {
        var config = structure.getConfig();
        if (config != null && config.has("tier") && config.get("tier").isJsonPrimitive()) {
            return config.get("tier").getAsDouble();
        }
        var m = IN_NAME.matcher(structure.name());
        if (!m.find()) {
            return 1;
        }
        String value = m.group(1).replace(',', '.');
        if (value.chars().allMatch(c -> "ivxIVX".indexOf(c) >= 0)) {
            return roman(value.toUpperCase(Locale.ROOT));
        }
        return Double.parseDouble(value);
    }

    private static int roman(String s) {
        int total = 0;
        int prev = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            int v = switch (s.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                default -> 10;
            };
            total += v < prev ? -v : v;
            prev = Math.max(prev, v);
        }
        return total;
    }

    public static String format(double tier) {
        return tier == Math.rint(tier) ? Long.toString((long) tier) : Double.toString(tier);
    }
}
