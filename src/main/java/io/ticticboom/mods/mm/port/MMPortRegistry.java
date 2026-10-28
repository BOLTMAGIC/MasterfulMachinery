package io.ticticboom.mods.mm.port;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.ae2.pattern.Ae2PatternPortType;
import io.ticticboom.mods.mm.port.ars.source.ArsSourcePortType;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortType;
import io.ticticboom.mods.mm.port.energy.EnergyPortType;
import io.ticticboom.mods.mm.port.entity.EntityPortType;
import io.ticticboom.mods.mm.port.fluid.FluidPortType;
import io.ticticboom.mods.mm.port.item.ItemPortType;
import io.ticticboom.mods.mm.port.kinetic.CreateKineticPortType;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortType;
import io.ticticboom.mods.mm.port.mekanism.heat.MekanismHeatPortType;
import io.ticticboom.mods.mm.port.nuclear.radiation.NuclearRadiationPortType;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortType;
import io.ticticboom.mods.mm.port.projecte.emc.ProjectEEmcPortType;
import io.ticticboom.mods.mm.port.replication.link.ReplicationLinkPortType;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortType;
import io.ticticboom.mods.mm.port.replication.matter.feature.ReplicationMatterPipeHook;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.ParserUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

import java.util.*;

public class MMPortRegistry {
    private static final Map<Identifier, PortType> PORT_TYPES = new HashMap<>();
    public static List<RegistryGroupHolder> PORTS = new ArrayList<>();
    public static final Map<Identifier, List<PortModel>> PORT_MODELS_BY_CONTROLLER = new HashMap<>();

    public static void init() {
        register(Ref.Ports.ITEM, new ItemPortType());
        register(Ref.Ports.FLUID, new FluidPortType());
        register(Ref.Ports.ENERGY, new EnergyPortType());
        register(Ref.Ports.ENTITY, new EntityPortType());

        if (ModList.get().isLoaded("mekanism")) {
            register(Ref.Ports.MEK_CHEMICAL, new MekanismChemicalPortType());
            register(Ref.Ports.MEK_HEAT, new MekanismHeatPortType());
        }
        if (ModList.get().isLoaded("create")) {
            register(Ref.Ports.CREATE_KINETIC, new CreateKineticPortType());
        }

        if (ModList.get().isLoaded("pneumaticcraft")) {
            register(Ref.Ports.PNEUMATIC_AIR, new PneumaticAirPortType());
        }

        if (ModList.get().isLoaded("replication")) {
            register(Ref.Ports.REPLICATION_MATTER, new ReplicationMatterPortType());
            register(Ref.Ports.REPLICATION_LINK, new ReplicationLinkPortType());
            ReplicationMatterPipeHook.register();
        }

        if (ModList.get().isLoaded("botania")) {
            register(Ref.Ports.BOTANIA_MANA, new BotaniaManaPortType());
        }

        if (ModList.get().isLoaded("ae2")) {
            register(Ref.Ports.AE2_PATTERN, new Ae2PatternPortType());
        }

        if (ModList.get().isLoaded("ars_nouveau")) {
            register(Ref.Ports.ARS_SOURCE, new ArsSourcePortType());
        }

        if (ModList.get().isLoaded("projecte")) {
            register(Ref.Ports.PROJECTE_EMC, new ProjectEEmcPortType());
        }

        if (ModList.get().isLoaded("nuclear_radiation")) {
            register(Ref.Ports.NUCLEAR_RADIATION, new NuclearRadiationPortType());
        }
    }

    public static PortType get(Identifier id) {
        return PORT_TYPES.get(id);
    }

    public static void register(Identifier id, PortType type) {
        PORT_TYPES.put(id, type);
    }

    /**
     * Looks up a port type, failing with something a pack author can act on.
     * <p>
     * Several port types only register when their integration mod is present, so a pack referring
     * to one of those without the mod installed used to surface as a bare NullPointerException
     * deep inside recipe parsing, with nothing naming the port or the mod.
     */
    public static PortType requirePortType(Identifier id) {
        var type = PORT_TYPES.get(id);
        if (type == null) {
            var known = new ArrayList<>(PORT_TYPES.keySet().stream().map(Identifier::toString).sorted().toList());
            throw new RuntimeException("Unknown port type '" + id + "'. Either it is misspelled, or the mod "
                    + "providing it is not installed. Registered port types: " + String.join(", ", known));
        }
        return type;
    }

    public static void rebuildPortCache() {
        PORT_MODELS_BY_CONTROLLER.clear();
        for (RegistryGroupHolder holder : PORTS) {
            try {
                if (holder.getBlock().get() instanceof IPortBlock bp) {
                    var model = bp.getModel();
                    for (Identifier controllerId : model.controllerIds().getIds()) {
                        PORT_MODELS_BY_CONTROLLER.computeIfAbsent(controllerId, x -> new ArrayList<>()).add(model);
                    }
                }
            } catch (Exception ignored) { }
        }
    }

    // New flexible parser: accepts a JsonElement which can be a JsonObject with 'type' (existing),
    // or a KubeJS Item object (with 'id' or 'item' and possibly 'nbt'), or a primitive string like 'mod:item{...}'.
    public static IPortIngredient parseIngredient(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new RuntimeException("Ingredient element is null");
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            // If it already has a 'type' field, use normal dispatch
            if (obj.has("type")) {
                var type = ParserUtils.parseId(obj, "type");
                return requirePortType(type).getParser().parseRecipeIngredient(obj);
            }
            // If it's a KubeJS Item object or similar, try to normalize to item-ingredient form
            JsonObject normalized = new JsonObject();
            normalized.addProperty("type", Ref.Ports.ITEM.toString());
            if (obj.has("item")) {
                normalized.add("item", obj.get("item"));
            } else if (obj.has("id")) {
                normalized.add("item", obj.get("id"));
            }
            if (obj.has("count")) normalized.add("count", obj.get("count"));
            if (obj.has("nbt")) normalized.add("nbt", obj.get("nbt"));
            if (obj.has("nbt_snbt")) normalized.add("nbt_snbt", obj.get("nbt_snbt"));
            if (obj.has("nbt_match")) normalized.add("nbt_match", obj.get("nbt_match"));
            return PORT_TYPES.get(Ref.Ports.ITEM).getParser().parseRecipeIngredient(normalized);
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            // support strings like "mod:item" or "mod:item{...snbt...}"
            String s = element.getAsString();
            String itemId = s;
            String snbt = null;
            int idx = s.indexOf('{');
            if (idx >= 0) {
                itemId = s.substring(0, idx);
                snbt = s.substring(idx);
            }
            JsonObject normalized = new JsonObject();
            normalized.addProperty("type", Ref.Ports.ITEM.toString());
            var itemEl = new com.google.gson.JsonPrimitive(itemId);
            normalized.add("item", itemEl);
            normalized.addProperty("count", 1);
            if (snbt != null) {
                normalized.addProperty("nbt_snbt", snbt);
            }
            return PORT_TYPES.get(Ref.Ports.ITEM).getParser().parseRecipeIngredient(normalized);
        }
        throw new RuntimeException("Unsupported ingredient format: " + element);
    }

    public static List<PortModel> getPortModelsForControllerId(Identifier id) {
        if (id == null) return List.of();
        var list = PORT_MODELS_BY_CONTROLLER.get(id);
        if (list == null) return List.of();
        return List.copyOf(list);
    }
}
