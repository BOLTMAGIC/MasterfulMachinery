package io.ticticboom.mods.mm.util;

import java.util.Random;

public class ChanceUtils {
    private static final Random random = new Random();
    public static boolean shouldProceed(double chance) {
        var rnd = random.nextDouble();
        return chance >= rnd;
    }

    public static String formatPercent(double fraction) {
        return new java.math.BigDecimal(Double.toString(fraction * 100)).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
