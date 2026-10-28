package io.ticticboom.mods.mm.port.entity;

import java.util.Locale;

public enum EntityPortMode {
    STORED,
    STANDING;

    public static EntityPortMode parse(String value) {
        if (value == null) {
            return STORED;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "stored", "store", "storage", "capture" -> STORED;
            case "standing", "stand", "pedestal", "registered" -> STANDING;
            default -> throw new RuntimeException(String.format(
                    "Unknown entity port mode [%s], expected 'stored' or 'standing'", value));
        };
    }

    public String serialize() {
        return name().toLowerCase(Locale.ROOT);
    }
}
