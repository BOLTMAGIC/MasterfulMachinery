package io.ticticboom.mods.mm.builder.structure;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
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
 * Loads {@code data/<ns>/mm_builder_structures/**.nbt} plus, for packs made for the old Multi Builder Tool,
 * {@code mbtool_structures/**} and {@code spatial_structures/**} into {@link BuildableStructureRegistry#SERVER}.
 * The id is the file path below the folder without {@code .nbt}; when two folders hold the same id the earlier
 * folder wins. Files are read off-thread, blocks resolved on apply.
 */
public class BuildableStructureLoader extends SimplePreparableReloadListener<Map<ResourceLocation, CompoundTag>> {
    public static final List<String> FOLDERS = List.of("mm_builder_structures", "mbtool_structures", "spatial_structures");

    @Override
    protected @NotNull Map<ResourceLocation, CompoundTag> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        Map<ResourceLocation, CompoundTag> files = new LinkedHashMap<>();
        for (String folder : FOLDERS) {
            Map<ResourceLocation, Resource> found = resourceManager.listResources(folder, location -> location.getPath().endsWith(".nbt"));
            for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                ResourceLocation file = entry.getKey();
                String path = file.getPath();
                ResourceLocation id = ResourceLocation.tryBuild(file.getNamespace(),
                        path.substring(folder.length() + 1, path.length() - ".nbt".length()));
                if (id == null || files.containsKey(id)) {
                    continue;
                }
                try (InputStream in = entry.getValue().open()) {
                    files.put(id, NbtIo.readCompressed(in));
                } catch (Exception e) {
                    Ref.LOG.warn("Could not read builder structure {}: {}", file, e.toString());
                }
            }
        }
        return files;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, CompoundTag> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        profiler.push("MM Builder Structures");
        List<BuildableStructure> loaded = parseAll(files);
        if (MMInteropManager.KUBEJS.isPresent()) {
            // scripts may remove structures and add more from other .nbt files (MMEvents.builderStructures)
            loaded = MMInteropManager.KUBEJS.get().postBuilderStructures(loaded, file -> read(resourceManager, file));
        }
        BuildableStructureRegistry.SERVER.replace(loaded);
        profiler.pop();
    }

    /** A structure .nbt by its resource location, or null when it is missing or unreadable. */
    @Nullable
    private static CompoundTag read(ResourceManager resourceManager, ResourceLocation file) {
        var resource = resourceManager.getResource(file);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream in = resource.get().open()) {
            return NbtIo.readCompressed(in);
        } catch (Exception e) {
            Ref.LOG.warn("Could not read builder structure {}: {}", file, e.toString());
            return null;
        }
    }

    public static List<BuildableStructure> parseAll(Map<ResourceLocation, CompoundTag> files) {
        List<BuildableStructure> loaded = new ArrayList<>();
        int skipped = 0;
        for (Map.Entry<ResourceLocation, CompoundTag> entry : files.entrySet()) {
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
                // usually a block from a mod that isn't installed; expected for bundled files, so debug only
                Ref.LOG.debug("Skipping builder structure {}: {}", entry.getKey(), result.problem());
            }
        }
        Ref.LOG.info("Loaded {} builder structures ({} skipped)", loaded.size(), skipped);
        return loaded;
    }
}
