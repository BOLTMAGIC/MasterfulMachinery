package io.ticticboom.mods.mm.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class ChanceUtils {
    private static final Random random = new Random();
    public static boolean shouldProceed(double chance) {
        var rnd = random.nextDouble();
        return chance >= rnd;
    }

    /**
     * @param fraction 0-1
     * @return the percentage with at most two decimals, e.g. "12.5"
     */
    public static String formatPercent(double fraction) {
        return new BigDecimal(Double.toString(fraction * 100)).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
