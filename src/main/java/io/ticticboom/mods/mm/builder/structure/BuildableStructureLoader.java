package io.ticticboom.mods.mm.builder.structure;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code data/<ns>/structure_builder/structures/**.nbt} into {@link BuildableStructureRegistry#SERVER}; MM's
 * own files are under {@code data/mm/structure_builder/structures/}. The old Multi Builder Tool folders
 * ({@code mbtool_structures}, {@code spatial_structures}) are not read: packs often still carry outdated copies there.
 * The id is the file path below the folder without {@code .nbt}. Files are read off-thread, blocks resolved on apply.
 */
public class BuildableStructureLoader extends SimplePreparableReloadListener<Map<Identifier, CompoundTag>> {
    public static final List<String> FOLDERS = List.of("structure_builder/structures");

    @Override
    protected @NotNull Map<Identifier, CompoundTag> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        Map<Identifier, CompoundTag> files = new LinkedHashMap<>();
        for (String folder : FOLDERS) {
            Map<Identifier, Resource> found = resourceManager.listResources(folder, location -> location.getPath().endsWith(".nbt"));
            for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
                Identifier file = entry.getKey();
                String path = file.getPath();
                Identifier id = Identifier.tryBuild(file.getNamespace(),
                        path.substring(folder.length() + 1, path.length() - ".nbt".length()));
                if (id == null || files.containsKey(id)) {
                    continue;
                }
                try (InputStream in = entry.getValue().open()) {
                    files.put(id, NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
                } catch (Exception e) {
                    Ref.LOG.warn("Could not read builder structure {}: {}", file, e.toString());
                }
            }
        }
        return files;
    }

    @Override
    protected void apply(@NotNull Map<Identifier, CompoundTag> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        profiler.push("MM Builder Structures");
        List<BuildableStructure> loaded = parseAll(files);
        if (MMInteropManager.KUBEJS.isPresent()) {
            loaded = MMInteropManager.KUBEJS.get().postBuilderStructures(loaded, file -> read(resourceManager, file));
        }
        BuildableStructureRegistry.SERVER.replace(loaded);
        profiler.pop();
    }

    @Nullable
    private static CompoundTag read(ResourceManager resourceManager, Identifier file) {
        var resource = resourceManager.getResource(file);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream in = resource.get().open()) {
            return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            Ref.LOG.warn("Could not read builder structure {}: {}", file, e.toString());
            return null;
        }
    }

    public static List<BuildableStructure> parseAll(Map<Identifier, CompoundTag> files) {
        List<BuildableStructure> loaded = new ArrayList<>();
        int skipped = 0;
        for (Map.Entry<Identifier, CompoundTag> entry : files.entrySet()) {
            BuildableStructureParser.Result result;
            try {
                result = BuildableStructureParser.parse(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                result = new BuildableStructureParser.Result(null, e.toString());
            }
            if (result.structure() != null) {
                loaded.add(result.structure());
            } else {
                skipped++;
                Ref.LOG.debug("Skipping builder structure {}: {}", entry.getKey(), result.problem());
            }
        }
        Ref.LOG.info("Loaded {} builder structures ({} skipped)", loaded.size(), skipped);
        return loaded;
    }
}
