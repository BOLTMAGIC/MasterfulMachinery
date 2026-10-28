package io.ticticboom.mods.mm.util;

public final class TextMatch {
    private TextMatch() {
    }

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
