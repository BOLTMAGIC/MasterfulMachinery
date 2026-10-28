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

public final class StructureTierRows {
    private final Map<String, NavigableMap<Integer, Block>> rows = new LinkedHashMap<>();
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

    public Map<String, NavigableMap<Integer, Block>> rows() {
        return Collections.unmodifiableMap(rows);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public int chosen(String key, int preferred) {
        var tiers = rows.get(key);
        if (tiers == null) {
            return -1;
        }
        return TierResolver.resolve(preferred, Integer.MIN_VALUE, Integer.MAX_VALUE, tiers.navigableKeySet());
    }

    public @Nullable Block chosenBlock(String key, int preferred) {
        var tiers = rows.get(key);
        return tiers == null ? null : tiers.get(chosen(key, preferred));
    }

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
