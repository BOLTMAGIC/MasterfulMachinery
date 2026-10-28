package io.ticticboom.mods.mm.builder;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class PortTiers {
    private PortTiers() {
    }

    public record RankedPort<T>(int rank, boolean input, T block) {
    }

    public static int rankOf(PortModel model) {
        int rank = model.config().getModel().getTierRank();
        var json = model.jsonConfig();
        if (rank <= 0 && json != null) {
            if (json.has("config") && json.get("config").isJsonObject()) {
                rank = readRank(json.getAsJsonObject("config"));
            }
            if (rank <= 0) {
                rank = readRank(json);
            }
        }
        return rank <= 0 ? 1 : rank;
    }

    private static int readRank(JsonObject json) {
        try {
            return json.has("tierRank") ? json.get("tierRank").getAsInt() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static String key(Identifier portTypeId, boolean input) {
        return portTypeId + (input ? "/input" : "/output");
    }

    public static Map<String, Integer> maxTiers(Collection<StructureModel> structures) {
        var result = new HashMap<String, Integer>();
        for (StructureModel structure : structures) {
            for (PositionedLayoutPiece positioned : structure.layout().getPositionedPieces()) {
                if (positioned.piece().piece() instanceof TieredPortPiece tiered) {
                    var options = tiered.tierOptions();
                    if (!options.isEmpty()) {
                        result.merge(tiered.tierKey(), options.lastKey(), Math::max);
                    }
                }
            }
        }
        return result;
    }

    public static Map<String, NavigableMap<Integer, Block>> registeredTiered() {
        var result = new LinkedHashMap<String, NavigableMap<Integer, Block>>();
        for (RegistryGroupHolder holder : MMPortRegistry.PORTS) {
            Block block = holder.getBlock().get();
            if (block instanceof IPortBlock port) {
                PortModel model = port.getModel();
                result.computeIfAbsent(key(model.type(), model.input()), k -> new TreeMap<>()).putIfAbsent(rankOf(model), block);
            }
        }
        result.values().removeIf(tiers -> tiers.size() < 2);
        return result;
    }

    public static Map<String, Integer> registeredMaxTiers() {
        var result = new HashMap<String, Integer>();
        registeredTiered().forEach((key, tiers) -> result.put(key, tiers.lastKey()));
        return result;
    }

    public static <T> NavigableMap<Integer, T> byRank(List<RankedPort<T>> ports) {
        var result = new TreeMap<Integer, T>();
        for (RankedPort<T> port : ports) {
            if (port.input()) {
                result.putIfAbsent(port.rank(), port.block());
            }
        }
        for (RankedPort<T> port : ports) {
            if (!port.input()) {
                result.putIfAbsent(port.rank(), port.block());
            }
        }
        return result;
    }
}
