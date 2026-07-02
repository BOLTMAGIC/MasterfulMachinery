package io.ticticboom.mods.mm.model;

import java.util.Locale;

public enum RecipeSelectionMode {
    DEFAULT("default", false),
    AVOID_SAME_RECIPE("avoid_same_recipe", true),
    ROUND_ROBIN_INPUT_ITEM("round_robin_input_item", true);

    private final String serializedName;
    private final boolean fairScheduling;

    RecipeSelectionMode(String serializedName, boolean fairScheduling) {
        this.serializedName = serializedName;
        this.fairScheduling = fairScheduling;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean fairScheduling() {
        return fairScheduling;
    }

    public static RecipeSelectionMode parse(String value) {
        if (value == null || value.isBlank()) return DEFAULT;
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "avoid_same_recipe", "avoidsame", "avoid_same" -> AVOID_SAME_RECIPE;
            case "round_robin_input_item", "round_robin_input", "roundrobininputitem", "roundrobininput" -> ROUND_ROBIN_INPUT_ITEM;
            case "default", "normal", "vanilla", "current" -> DEFAULT;
            default -> DEFAULT;
        };
    }
}
