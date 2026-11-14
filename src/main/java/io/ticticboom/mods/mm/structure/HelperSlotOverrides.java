package io.ticticboom.mods.mm.structure;

import io.ticticboom.mods.mm.structure.layout.StructureKeyChar;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import io.ticticboom.mods.mm.structure.layout.StructureLayoutPiece;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * World-wide, admin-made replacement for a structure's own {@code allowHelper} keys and {@code maxHelpers}: which
 * blocks an Input Gateway / Pattern Port may stand in for. A structure without a rule here keeps its JSON / KubeJS
 * settings. Edited from the Structure Builder's admin tab.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HelperSlotOverrides extends SavedData {
    private static final String DATA_ID = "mm_helper_slots";
    public static final int MAX_LIMIT = 64;
    private static final int MAX_RULES = 4096;

    /** The key characters a helper may replace, and how many helpers at once (negative: no limit). */
    public record Rule(Set<Character> keys, int maxHelpers) {
        public Rule {
            keys = Set.copyOf(keys);
        }
    }

    public enum Action { TOGGLE_KEY, SET_MAX, RESET }

    // the server's rules once loaded (null until then), and the ones the server sent to this client
    private static volatile Map<ResourceLocation, Rule> server;
    private static volatile Map<ResourceLocation, Rule> client = Map.of();

    private final Map<ResourceLocation, Rule> rules = new LinkedHashMap<>();

    /** The admin rule for a structure on this side, or null when it uses its own settings. */
    @Nullable
    public static Rule rule(ResourceLocation structureId, boolean clientSide) {
        if (clientSide) {
            return client.get(structureId);
        }
        Map<ResourceLocation, Rule> current = server;
        if (current == null) {
            MinecraftServer running = ServerLifecycleHooks.getCurrentServer();
            if (running == null) {
                return null;
            }
            current = get(running).snapshot();
        }
        return current.get(structureId);
    }

    public static Map<ResourceLocation, Rule> clientSnapshot() {
        return client;
    }

    public static void receive(Map<ResourceLocation, Rule> rules) {
        client = Map.copyOf(rules);
    }

    public static HelperSlotOverrides get(MinecraftServer running) {
        HelperSlotOverrides data = running.overworld().getDataStorage()
                .computeIfAbsent(HelperSlotOverrides::load, HelperSlotOverrides::new, DATA_ID);
        server = data.snapshot();
        return data;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        server = null;
        client = Map.of();
    }

    public Map<ResourceLocation, Rule> snapshot() {
        return Map.copyOf(rules);
    }

    /** The structure's own settings, as the starting point of a new admin rule. */
    public static Rule fromStructure(StructureModel model) {
        Set<Character> keys = new TreeSet<>();
        StructureLayout layout = model.layout();
        for (Map.Entry<StructureKeyChar, StructureLayoutPiece> entry : layout.getPieces().entrySet()) {
            if (entry.getValue().allowsHelper() && !StructureLayout.isPortPiece(entry.getValue().piece())) {
                keys.add(entry.getKey().character());
            }
        }
        return new Rule(keys, layout.getMaxHelpers());
    }

    /** The settings in force for a structure on this side: the admin rule, else its own. */
    public static Rule effective(StructureModel model, boolean clientSide) {
        Rule admin = rule(model.id(), clientSide);
        return admin != null ? admin : fromStructure(model);
    }

    public boolean apply(Action action, ResourceLocation structureId, char key, int value) {
        StructureModel model = StructureManager.STRUCTURES.get(structureId);
        if (model == null) {
            return false;
        }
        Rule current = rules.containsKey(structureId) ? rules.get(structureId) : fromStructure(model);
        switch (action) {
            case TOGGLE_KEY -> {
                StructureLayoutPiece piece = model.layout().getPieces().get(new StructureKeyChar(key));
                if (piece == null || StructureLayout.isPortPiece(piece.piece())) {
                    return false;
                }
                Set<Character> keys = new TreeSet<>(current.keys());
                if (!keys.remove(key)) {
                    keys.add(key);
                }
                if (!put(structureId, new Rule(keys, current.maxHelpers()))) {
                    return false;
                }
            }
            case SET_MAX -> {
                if (value < -1 || value > MAX_LIMIT || !put(structureId, new Rule(current.keys(), value))) {
                    return false;
                }
            }
            case RESET -> {
                if (rules.remove(structureId) == null) {
                    return false;
                }
            }
        }
        server = snapshot();
        setDirty();
        return true;
    }

    private boolean put(ResourceLocation structureId, Rule rule) {
        if (!rules.containsKey(structureId) && rules.size() >= MAX_RULES) {
            return false;
        }
        rules.put(structureId, rule);
        return true;
    }

    public static HelperSlotOverrides load(CompoundTag tag) {
        HelperSlotOverrides data = new HelperSlotOverrides();
        ListTag entries = tag.getList("Rules", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size() && data.rules.size() < MAX_RULES; i++) {
            CompoundTag entry = entries.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Structure"));
            if (id == null) {
                continue;
            }
            Set<Character> keys = new LinkedHashSet<>();
            for (char c : entry.getString("Keys").toCharArray()) {
                keys.add(c);
            }
            int max = entry.contains("Max") ? entry.getInt("Max") : -1;
            data.rules.put(id, new Rule(keys, Math.max(-1, Math.min(MAX_LIMIT, max))));
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag entries = new ListTag();
        rules.forEach((id, rule) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Structure", id.toString());
            entry.putString("Keys", keysString(rule.keys()));
            entry.putInt("Max", rule.maxHelpers());
            entries.add(entry);
        });
        tag.put("Rules", entries);
        return tag;
    }

    public static String keysString(Set<Character> keys) {
        StringBuilder text = new StringBuilder();
        new TreeSet<>(keys).forEach(text::append);
        return text.toString();
    }
}
