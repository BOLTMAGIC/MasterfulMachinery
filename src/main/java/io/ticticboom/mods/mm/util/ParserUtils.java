package io.ticticboom.mods.mm.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;


public class ParserUtils {

    public static Identifier parseId(JsonElement json) {
        var s = json.getAsString();
        var rl = Identifier.tryParse(s);
        if (rl == null) throw new RuntimeException("Invalid resource location: " + s);
        return rl;
    }

    public static Identifier parseId(JsonObject json, String key) {
        return parseId(json.get(key));
    }

    public static Identifier parseOptionalId(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return parseId(json, key);
    }

    /**
     * Parses a display name that may be given either as a plain string or as a translation object:
     * <pre>
     *   "name": "Assembler"
     *   "name": { "translation": "mypack.machine.assembler" }
     * </pre>
     * Plain strings stay literal, so existing datapacks keep working unchanged.
     */
    public static Component parseComponent(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            return Component.literal(json.getAsString());
        } else if (json.isJsonObject() && json.getAsJsonObject().has("translation")) {
            return Component.translatable(json.getAsJsonObject().get("translation").getAsString());
        }
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> new RuntimeException(
                        "Failed to parse text component: " + error + ". A name may be a plain string, "
                                + "a { \"translation\": \"key\" } object, or a full text component."));
    }

    /**
     * Name element as a supplier so an animated style can rebuild itself each time the name is
     * drawn. Everything except "rainbow" resolves once and hands back the same component.
     */
    public static Supplier<Component> parseNameSupplier(JsonElement json) {
        if (json != null && json.isJsonObject()) {
            var obj = json.getAsJsonObject();
            if (obj.has("gradient")) {
                var colours = new ArrayList<Integer>();
                for (JsonElement colour : obj.getAsJsonArray("gradient")) {
                    colours.add(parseColour(colour.getAsString()));
                }
                return NameStyles.gradient(obj.get("text").getAsString(), colours);
            }
            if (obj.has("rainbow") && obj.get("rainbow").getAsBoolean()) {
                double speed = obj.has("speed") ? obj.get("speed").getAsDouble() : 0.5;
                double spread = obj.has("spread") ? obj.get("spread").getAsDouble() : 0.05;
                if (obj.has("colors")) {
                    var colours = new ArrayList<Integer>();
                    for (JsonElement colour : obj.getAsJsonArray("colors")) {
                        colours.add(parseColour(colour.getAsString()));
                    }
                    return NameStyles.cycle(obj.get("text").getAsString(), colours, speed, spread);
                }
                return NameStyles.rainbow(obj.get("text").getAsString(), speed, spread);
            }
        }
        var component = parseComponent(json);
        return () -> component;
    }

    public static int parseColour(String value) {
        var text = value.startsWith("#") ? value.substring(1) : value;
        try {
            return (int) (Long.parseLong(text, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid colour [" + value + "], expected a hex value such as #55FFFF");
        }
    }

    /**
     * Raw string form of a name element, for serialization, data generation and NBT, where a
     * resolved {@link Component} is not usable. Returns the literal text for plain strings, or the
     * translation key for translation objects.
     */
    public static String parseComponentKey(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            return json.getAsString();
        } else if (json.isJsonObject() && json.getAsJsonObject().has("translation")) {
            return json.getAsJsonObject().get("translation").getAsString();
        }
        if (json.isJsonObject()) {
            var obj = json.getAsJsonObject();
            if (obj.has("text")) {
                return obj.get("text").getAsString();
            }
        }
        return parseComponent(json).getString();
    }

    @SuppressWarnings("unused")
    public static Component parseComponent(JsonObject json, String key) {
        return parseComponent(json.get(key));
    }

    public static <T> T parseOrDefault(JsonObject json, String key, Supplier<T> defaultSupplier, Function<JsonObject, T> parser) {
        if (json.has(key)) {
            return parser.apply(json);
        }
        return defaultSupplier.get();
    }

    public static <T> Supplier<T> parseOrDefaultSupplier(JsonObject json, String key, Supplier<T> defaultSupplier, Function<JsonElement, T> getter) {
        Supplier<T> autoPushSupplier = defaultSupplier;
        if (json.has(key)) {
            T autoPush = getter.apply(json.get(key));
            autoPushSupplier = () -> autoPush;
        }
        return autoPushSupplier;
    }


    public static <T extends Enum<T>> T parseEnum(JsonObject json, String key, Class<T> cls) {
        var name = json.get(key).getAsString();
        return T.valueOf(cls, name.toUpperCase());
    }
}
