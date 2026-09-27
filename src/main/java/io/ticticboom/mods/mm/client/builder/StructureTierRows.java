package io.ticticboom.mods.mm.client.builder;

import io.ticticboom.mods.mm.builder.TierResolver;
import io.ticticboom.mods.mm.builder.TieredPortPiece;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The port types of a structure whose positions take a range of tiers, and which tier and block a preference
 * turns into there. Shared by the controller's Assemble screen and the multiblock tool screen.
 */
public final class StructureTierRows {
    /** port type key -> tier -> block, over every port_type position of the structure */
    private final Map<String, NavigableMap<Integer, Block>> rows = new LinkedHashMap<>();
    /** port type key -> its positions, each with its own tier range */
    private final Map<String, List<TieredPortPiece>> positions = new LinkedHashMap<>();

    public StructureTierRows(@Nullable StructureModel structure) {
        if (structure == null) {
            return;
        }
        for (var positioned : structure.layout().getPositionedPieces()) {
            if (positioned.piece().piece() instanceof TieredPortPiece tiered) {
                String key = tiered.tierKey();
                var tiers = rows.computeIfAbsent(key, k -> new TreeMap<>());
                tiered.tierOptions().forEach(tiers::putIfAbsent);
                positions.computeIfAbsent(key, k -> new ArrayList<>()).add(tiered);
            }
        }
        rows.values().removeIf(Map::isEmpty);
    }

    /** Port type key -> tier -> block, only types with at least one existing tier. */
    public Map<String, NavigableMap<Integer, Block>> rows() {
        return Collections.unmodifiableMap(rows);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /** The tier used for a preference over all of this type's tiers, or -1 when the type is not in the structure. */
    public int chosen(String key, int preferred) {
        var tiers = rows.get(key);
        if (tiers == null) {
            return -1;
        }
        return TierResolver.resolve(preferred, Integer.MIN_VALUE, Integer.MAX_VALUE, tiers.navigableKeySet());
    }

    /** The block {@link #chosen} stands for, null when none. */
    public @Nullable Block chosenBlock(String key, int preferred) {
        var tiers = rows.get(key);
        return tiers == null ? null : tiers.get(chosen(key, preferred));
    }

    /** True when some position of this port type cannot take the preferred tier and gets another one. */
    public boolean adjusted(String key, int preferred) {
        if (preferred == TierResolver.LOWEST) {
            return false;
        }
        for (TieredPortPiece piece : positions.getOrDefault(key, List.of())) {
            if (piece.resolveTier(preferred) != preferred) {
                return true;
            }
        }
        return false;
    }
}
