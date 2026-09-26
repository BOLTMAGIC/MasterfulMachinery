package io.ticticboom.mods.mm.util;

/**
 * Case-insensitive search that keeps string indices valid: characters are compared one by one (as
 * {@link String#regionMatches(boolean, int, String, int, int)} does), so a returned index always points into the
 * original text, unlike an index into {@code text.toLowerCase()}, which may be longer (Turkish 'İ' lower-cases to
 * two chars).
 */
public final class TextMatch {
    private TextMatch() {
    }

    /** @return the first index in {@code text} where {@code query} is found ignoring case, -1 when not; 0 for an empty query */
    public static int indexOfIgnoreCase(String text, String query) {
        if (query.isEmpty()) {
            return 0;
        }
        for (int i = 0; i + query.length() <= text.length(); i++) {
            if (text.regionMatches(true, i, query, 0, query.length())) {
                return i;
            }
        }
        return -1;
    }

    public static boolean containsIgnoreCase(String text, String query) {
        return indexOfIgnoreCase(text, query) >= 0;
    }
}
