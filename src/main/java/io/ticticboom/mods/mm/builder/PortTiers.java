package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Tier helpers shared by structure matching and assembly. */
public final class PortTiers {
    private PortTiers() {
    }

    /** One port a tiered position accepts. */
    public record RankedPort<T>(int rank, boolean input, T block) {
    }

    /** The port's tier; ports without a tier count as 1 (same default structure matching always used). */
    public static int rankOf(PortModel model) {
        int rank = model.config().getModel().getTierRank();
        try {
            if (rank <= 0 && model.jsonConfig() != null && model.jsonConfig().has("tierRank")) {
                rank = model.jsonConfig().get("tierRank").getAsInt();
            }
        } catch (Exception ignored) {
        }
        return rank <= 0 ? 1 : rank;
    }

    public static String key(ResourceLocation portTypeId, boolean input) {
        return portTypeId + (input ? "/input" : "/output");
    }

    /** Every tier preference key the structures use, with the highest tier any of their positions offers for it. */
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

    /**
     * Every tier preference key of the registered ports that come in more than one tier, in registration order,
     * with tier -> the first registered port block of that tier.
     */
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

    /** {@link #registeredTiered()} keys with their highest tier, as {@link TierPrefs#validate} takes them. */
    public static Map<String, Integer> registeredMaxTiers() {
        var result = new HashMap<String, Integer>();
        registeredTiered().forEach((key, tiers) -> result.put(key, tiers.lastKey()));
        return result;
    }

    /**
     * Tier -> the port assembly puts down for that tier: the first input port of the tier, else the first
     * output port (a tier may only have outputs), in registration order.
     */
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
