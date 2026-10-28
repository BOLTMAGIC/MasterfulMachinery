package io.ticticboom.mods.mm.builder.structure;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BuildableStructureRegistry {
    public static final BuildableStructureRegistry SERVER = new BuildableStructureRegistry();
    public static final BuildableStructureRegistry CLIENT = new BuildableStructureRegistry();

    private record Snapshot(Map<Identifier, BuildableStructure> byId, List<BuildableStructure> sorted) {
    }

    private volatile Snapshot snapshot = new Snapshot(Map.of(), List.of());

    private BuildableStructureRegistry() {
    }

    public List<BuildableStructure> all() {
        return snapshot.sorted();
    }

    @Nullable
    public BuildableStructure get(Identifier id) {
        return snapshot.byId().get(id);
    }

    public void replace(Collection<BuildableStructure> next) {
        var map = new TreeMap<Identifier, BuildableStructure>();
        for (BuildableStructure structure : next) {
            map.put(structure.id(), structure);
        }
        snapshot = new Snapshot(map, List.copyOf(map.values()));
    }

    public void clear() {
        replace(List.of());
    }
}
