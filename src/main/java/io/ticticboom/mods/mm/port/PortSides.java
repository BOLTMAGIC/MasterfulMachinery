package io.ticticboom.mods.mm.port;

import com.google.gson.JsonElement;

import java.util.Locale;

public enum PortSides {
    BOTH,
    INPUT,
    OUTPUT;

    public static PortSides parse(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return BOTH;
        }
        return parse(element.getAsString());
    }

    public static PortSides parse(String value) {
        if (value == null || value.isBlank()) {
            return BOTH;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "both", "all" -> BOTH;
            case "input", "in" -> INPUT;
            case "output", "out" -> OUTPUT;
            default -> throw new RuntimeException(String.format(
                    "Unknown port side [%s], expected 'input', 'output' or 'both'", value));
        };
    }

    public boolean hasInput() {
        return this != OUTPUT;
    }

    public boolean hasOutput() {
        return this != INPUT;
    }

    public String serialize() {
        return name().toLowerCase(Locale.ROOT);
    }
}
