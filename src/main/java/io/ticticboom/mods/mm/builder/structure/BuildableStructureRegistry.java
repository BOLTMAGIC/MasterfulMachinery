package io.ticticboom.mods.mm.builder.structure;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The non-MM structures the Multiblock Tool can build, sorted by id. {@link #SERVER} is filled by
 * {@link BuildableStructureLoader} on datapack (re)load; {@link #CLIENT} by {@link BuildableStructureSync}.
 * They are separate because both sides share one JVM in singleplayer.
 */
public final class BuildableStructureRegistry {
    public static final BuildableStructureRegistry SERVER = new BuildableStructureRegistry();
    public static final BuildableStructureRegistry CLIENT = new BuildableStructureRegistry();

    private record Snapshot(Map<ResourceLocation, BuildableStructure> byId, List<BuildableStructure> sorted) {
    }

    private volatile Snapshot snapshot = new Snapshot(Map.of(), List.of());

    private BuildableStructureRegistry() {
    }

    public List<BuildableStructure> all() {
        return snapshot.sorted();
    }

    @Nullable
    public BuildableStructure get(ResourceLocation id) {
        return snapshot.byId().get(id);
    }

    public void replace(Collection<BuildableStructure> next) {
        var map = new TreeMap<ResourceLocation, BuildableStructure>();
        for (BuildableStructure structure : next) {
            map.put(structure.id(), structure);
        }
        snapshot = new Snapshot(map, List.copyOf(map.values()));
    }

    public void clear() {
        replace(List.of());
    }
}
