package io.ticticboom.mods.mm.structure;

import io.ticticboom.mods.mm.event.DeferredDataLoad;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import io.ticticboom.mods.mm.controller.single.SingleMachines;
import com.google.gson.JsonElement;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// No @EventBusSubscriber here: this class has no @SubscribeEvent methods and never did. Forge
// ignored the empty annotation, NeoForge rejects it outright. It is registered as a reload
// listener from ForgeEventsListener.registerReloadListeners instead.
public class StructureManager extends SimpleJsonResourceReloadListener<JsonElement> {

    public StructureManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("mm/structures"));
    }

    public static final Map<Identifier, StructureModel> STRUCTURES = new HashMap<>();
    public static final Map<Identifier, ItemStack> STRUCTURE_BLUEPRINTS = new HashMap<>();
    public static final Map<Identifier, List<StructureModel>> STRUCTURES_BY_CONTROLLER = new HashMap<>();

    public static List<StructureModel> getStructuresForController(Identifier controllerId) {
        if (controllerId == null) return List.of();
        var list = STRUCTURES_BY_CONTROLLER.get(controllerId);
        if (list == null) return List.of();
        return List.copyOf(list);
    }

    public static void validateAllPieces() {
        StructureProblems.clear();
        for (StructureModel value : STRUCTURES.values()) {
            value.validate();
        }
        if (!StructureProblems.isEmpty()) {
            Ref.LOG.error("Masterful Machinery: {} structure(s) use ports that do not exist, so those machines "
                    + "will never form. Add the missing port configs or correct the names: {}",
                    StructureProblems.structureCount(), StructureProblems.summary());
        }
    }

    @Override
    protected void apply(@NotNull Map<Identifier, JsonElement> jsons, @NotNull ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        profilerFiller.push("MM Structures");
        // parsed once item components are bound (DeferredDataLoad)
        DeferredDataLoad.structures(jsons);
        profilerFiller.pop();
    }

    public static void receiveStructures(Map<Identifier, JsonElement> jsons) {
        STRUCTURES.clear();
        STRUCTURES_BY_CONTROLLER.clear();
        try {
            Ref.LCTX.reset("Structure Loading");
            // Parsed independently for the same reason as recipes: one structure referencing a
            // port from an uninstalled integration mod should not make the world unloadable.
            int skipped = 0;
            for (Map.Entry<Identifier, JsonElement> entry : jsons.entrySet()) {
                if (SingleMachines.isImplicitStructure(entry.getKey())) {
                    continue;
                }
                Ref.LCTX.push(String.format("Loading Structure: %s", entry.getKey().toString()));
                try {
                    var model = StructureModel.parse(entry.getValue().getAsJsonObject(), entry.getKey());
                    storeStructure(entry.getKey(), model);
                } catch (Exception e) {
                    skipped++;
                    Ref.LOG.error("Skipping structure {}: {}", entry.getKey(), e.getMessage());
                }
                Ref.LCTX.pop();
            }
            if (skipped > 0) {
                Ref.LOG.error("{} MM structure(s) failed to load and were skipped.", skipped);
            }
            if (MMInteropManager.KUBEJS.isPresent()) {
                Ref.LCTX.push("Loading KubeJS Structures");
                for (StructureModel structureModel : MMInteropManager.KUBEJS.get().postCreateStructures()) {
                    Ref.LCTX.push(String.format("Loading KubeJS Structure: %s", structureModel.id()));

                    storeStructure(structureModel.id(), structureModel);
                    Ref.LCTX.pop();
                }
                Ref.LCTX.pop();
            }
            for (var entry : SingleMachines.implicitStructures().entrySet()) {
                STRUCTURES.put(entry.getKey(), entry.getValue());
                indexByController(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            Ref.LCTX.doThrow(e);
        }
    }

    public static List<StructureModel> buildableStructures() {
        return STRUCTURES.values().stream().filter(s -> !SingleMachines.isImplicitStructure(s.id())).toList();
    }

    private static void storeStructure(Identifier id, StructureModel structure) {
        STRUCTURES.put(id, structure);
        STRUCTURE_BLUEPRINTS.put(id, MMRegisters.BLUEPRINT.get().getStructureInstance(id));
        for (Identifier controllerId : structure.controllerIds().getIds()) {
            indexByController(controllerId, structure);
        }
    }

    private static void indexByController(Identifier controllerId, StructureModel structure) {
        var list = STRUCTURES_BY_CONTROLLER.computeIfAbsent(controllerId, x -> new ArrayList<>());
        list.removeIf(existing -> existing.id().equals(structure.id()));
        list.add(structure);
    }

}
