package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureRegistry;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** World-wide, admin-managed grouping for the Structure Builder gallery. */
public final class StructureCategories extends SavedData {
    public static final String DEFAULT = "Uncategorized";
    private static final String DATA_ID = "mm_structure_categories";
    private static final int MAX_CATEGORIES = 64;
    private static final int MAX_ASSIGNMENTS = 4096;

    public record Snapshot(List<String> categories, Map<String, String> assignments, long revision) {
        public String category(ResourceLocation id, boolean builder) {
            return assignments.getOrDefault(key(id, builder), DEFAULT);
        }
    }

    private static volatile Snapshot client = new Snapshot(List.of(), Map.of(), 0);
    private final List<String> categories = new ArrayList<>();
    private final Map<String, String> assignments = new LinkedHashMap<>();
    private final Set<String> importedStructures = new LinkedHashSet<>();

    public enum Action { CREATE, RENAME, DELETE, ASSIGN }

    public static String key(ResourceLocation id, boolean builder) {
        return (builder ? "builder|" : "mm|") + id;
    }

    public static Snapshot clientSnapshot() { return client; }

    public static void receive(List<String> names, Map<String, String> assignments) {
        client = new Snapshot(List.copyOf(names), Map.copyOf(assignments), client.revision() + 1);
    }

    public static StructureCategories get(MinecraftServer server) {
        StructureCategories data = server.overworld().getDataStorage().computeIfAbsent(StructureCategories::load, StructureCategories::new, DATA_ID);
        data.importDefaults(StructureManager.STRUCTURES.keySet(), BuildableStructureRegistry.SERVER.all());
        return data;
    }

    public void importDefaults(Collection<ResourceLocation> machines, Collection<BuildableStructure> builders) {
        for (ResourceLocation id : machines) {
            importDefault(key(id, false), "Custom Multiblocks");
        }
        for (BuildableStructure structure : builders) {
            String path = structure.id().getPath();
            int slash = path.indexOf('/');
            String folder = slash > 0 ? path.substring(0, slash) : "";
            String category = switch (folder) {
                case "voidminer" -> "Void Miner";
                case "crazy_ae2" -> "Crazy AE2";
                case "woot" -> "Woot";
                case "" -> structure.groupName();
                default -> BuildableStructure.fallbackName(new ResourceLocation(structure.id().getNamespace(), folder));
            };
            importDefault(key(structure.id(), true), category);
        }
    }

    private void importDefault(String key, String category) {
        if (importedStructures.contains(key) || importedStructures.size() >= MAX_ASSIGNMENTS) return;
        if (!assignments.containsKey(key)) {
            if (!validName(category) || category.equalsIgnoreCase(DEFAULT) || assignments.size() >= MAX_ASSIGNMENTS) return;
            String importedName = category;
            String existing = categories.stream().filter(name -> name.equalsIgnoreCase(importedName)).findFirst().orElse(null);
            if (existing != null) {
                category = existing;
            } else {
                if (categories.size() >= MAX_CATEGORIES) return;
                categories.add(category);
            }
            assignments.put(key, category);
        }
        importedStructures.add(key);
        setDirty();
    }

    public Snapshot snapshot() {
        return new Snapshot(List.copyOf(categories), Map.copyOf(assignments), 0);
    }

    public static StructureCategories load(CompoundTag tag) {
        StructureCategories data = new StructureCategories();
        ListTag names = tag.getList("Categories", Tag.TAG_STRING);
        for (int i = 0; i < names.size() && data.categories.size() < MAX_CATEGORIES; i++) {
            String name = names.getString(i);
            if (validName(name) && !data.containsName(name)) data.categories.add(name);
        }
        ListTag entries = tag.getList("Assignments", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size() && data.assignments.size() < MAX_ASSIGNMENTS; i++) {
            CompoundTag entry = entries.getCompound(i);
            String key = entry.getString("Structure");
            String category = entry.getString("Category");
            if (validKey(key) && data.categories.contains(category)) data.assignments.put(key, category);
        }
        ListTag imported = tag.getList("ImportedStructures", Tag.TAG_STRING);
        for (int i = 0; i < imported.size() && data.importedStructures.size() < MAX_ASSIGNMENTS; i++) {
            String key = imported.getString(i);
            if (validKey(key)) data.importedStructures.add(key);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag names = new ListTag();
        for (String name : categories) names.add(StringTag.valueOf(name));
        tag.put("Categories", names);
        ListTag entries = new ListTag();
        assignments.forEach((key, category) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Structure", key);
            entry.putString("Category", category);
            entries.add(entry);
        });
        tag.put("Assignments", entries);
        ListTag imported = new ListTag();
        for (String key : importedStructures) imported.add(StringTag.valueOf(key));
        tag.put("ImportedStructures", imported);
        return tag;
    }

    public boolean apply(Action action, String target, String value) {
        switch (action) {
            case CREATE -> {
                if (!validName(value) || containsName(value) || categories.size() >= MAX_CATEGORIES) return false;
                categories.add(value);
            }
            case RENAME -> {
                if (!categories.contains(target) || !validName(value) || containsName(value)) return false;
                categories.set(categories.indexOf(target), value);
                assignments.replaceAll((key, category) -> category.equals(target) ? value : category);
            }
            case DELETE -> {
                if (!categories.remove(target)) return false;
                assignments.values().removeIf(category -> category.equals(target));
            }
            case ASSIGN -> {
                if (!validKey(target) || !knownStructure(target) ||
                        (!value.equals(DEFAULT) && !categories.contains(value))) return false;
                if (value.equals(DEFAULT)) assignments.remove(target);
                else if (assignments.size() < MAX_ASSIGNMENTS || assignments.containsKey(target)) assignments.put(target, value);
                else return false;
            }
        }
        setDirty();
        return true;
    }

    private boolean containsName(String name) {
        return name.equalsIgnoreCase(DEFAULT) || categories.stream().anyMatch(other -> other.equalsIgnoreCase(name));
    }

    private static boolean validName(@Nullable String name) {
        if (name == null || name.isBlank() || name.length() > 32 || !name.equals(name.strip())) return false;
        return name.chars().noneMatch(c -> Character.isISOControl(c) || c == '\u00a7');
    }

    private static boolean validKey(String key) {
        int split = key.indexOf('|');
        if (split < 1 || split == key.length() - 1) return false;
        if (!key.startsWith("builder|") && !key.startsWith("mm|")) return false;
        return ResourceLocation.tryParse(key.substring(split + 1)) != null;
    }

    private static boolean knownStructure(String key) {
        ResourceLocation id = ResourceLocation.tryParse(key.substring(key.indexOf('|') + 1));
        return key.startsWith("builder|") ? BuildableStructureRegistry.SERVER.get(id) != null
                : StructureManager.STRUCTURES.containsKey(id);
    }
}
