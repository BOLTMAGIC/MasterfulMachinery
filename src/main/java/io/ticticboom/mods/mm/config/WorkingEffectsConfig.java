package io.ticticboom.mods.mm.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * config/mm/working_effects.json: one place to set every machine's working sound and particle, client side.
 * <pre>
 * { "mm:auto_crusher_controller": { "sound": "minecraft:block.grindstone.use", "interval": 25, "particle": "minecraft:smoke" } }
 * </pre>
 * Each field is optional and overrides the controller's own workingSound / workingSoundInterval / workingParticle
 * (KubeJS or JSON). "sound": "" or "particle": "" turns that effect off. Reloaded with F3+T.
 */
public final class WorkingEffectsConfig {

    public record Entry(@Nullable String sound, @Nullable Integer interval, @Nullable String particle) {
    }

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("mm").resolve("working_effects.json");
    private static final String HELP = "Working sound and particle per machine controller (block id). Each field is optional and "
            + "overrides the controller's KubeJS/JSON settings; \"\" turns it off. interval = ticks between sounds. Reload with F3+T. "
            + "Example: \"mm:auto_crusher_controller\": {\"sound\": \"minecraft:block.grindstone.use\", \"interval\": 25, \"particle\": \"minecraft:smoke\"}";

    private static Map<ResourceLocation, Entry> entries = Map.of();
    private static boolean loaded;
    // bumped on every reload so controller blocks know to resolve their effects again
    private static int generation;

    private WorkingEffectsConfig() {
    }

    @Nullable
    public static Entry get(ResourceLocation controllerId) {
        if (!loaded) reload();
        return entries.get(controllerId);
    }

    public static int generation() {
        if (!loaded) reload();
        return generation;
    }

    public static Map<ResourceLocation, Entry> snapshot() {
        if (!loaded) reload();
        return Map.copyOf(entries);
    }

    /** "inherit" removes an override; an empty sound or particle disables that effect. */
    public static boolean validOverride(String field, String value) {
        if (!field.equals("sound") && !field.equals("interval") && !field.equals("particle")) return false;
        if (value.equals("inherit")) return true;
        if (field.equals("interval")) {
            try {
                int ticks = Integer.parseInt(value);
                return ticks >= 1 && ticks <= 1200;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return value.isEmpty() || ResourceLocation.tryParse(value) != null;
    }

    /** Update only one machine field, preserving unrelated entries and custom fields in the JSON file. */
    public static synchronized boolean setOverride(ResourceLocation controller, String field, String value) {
        if (!validOverride(field, value)) return false;
        try {
            JsonObject json = Files.exists(FILE)
                    ? JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject()
                    : new JsonObject();
            JsonObject machine = json.has(controller.toString()) && json.get(controller.toString()).isJsonObject()
                    ? json.getAsJsonObject(controller.toString()) : new JsonObject();
            if (value.equals("inherit")) machine.remove(field);
            else if (field.equals("interval")) machine.addProperty(field, Integer.parseInt(value));
            else machine.addProperty(field, value);
            if (machine.size() == 0) json.remove(controller.toString());
            else json.add(controller.toString(), machine);
            Files.createDirectories(FILE.getParent());
            Path staged = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(staged, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
            try {
                Files.move(staged, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(staged, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            reload();
            return true;
        } catch (IOException | RuntimeException e) {
            Ref.LOG.error("Failed to update working effect {}.{}", controller, field, e);
            return false;
        }
    }

    public static synchronized void reload() {
        loaded = true;
        generation++;
        try {
            if (!Files.exists(FILE)) {
                writeDefault();
                entries = Map.of();
                return;
            }
            JsonObject json = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<ResourceLocation, Entry> read = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : json.entrySet()) {
                if (e.getKey().startsWith("_")) continue;
                ResourceLocation id = ResourceLocation.tryParse(e.getKey());
                if (id == null || !e.getValue().isJsonObject()) {
                    Ref.LOG.warn("Ignoring {} in {}: expected a controller id with an object", e.getKey(), FILE);
                    continue;
                }
                JsonObject o = e.getValue().getAsJsonObject();
                read.put(id, new Entry(
                        o.has("sound") ? o.get("sound").getAsString() : null,
                        o.has("interval") ? Math.max(1, o.get("interval").getAsInt()) : null,
                        o.has("particle") ? o.get("particle").getAsString() : null));
            }
            entries = read;
        } catch (IOException | RuntimeException e) {
            Ref.LOG.error("Failed to read {}, machine working effects fall back to the controllers' own settings", FILE, e);
            entries = Map.of();
        }
    }

    private static void writeDefault() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("_help", HELP);
        Files.createDirectories(FILE.getParent());
        Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
    }
}
