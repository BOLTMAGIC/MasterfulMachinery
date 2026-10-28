package io.ticticboom.mods.mm.builder;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;

public interface TieredPortPiece {
    Identifier getPortTypeId();

    Optional<Boolean> getInput();

    int getMinTier();

    int getMaxTier();

    NavigableMap<Integer, Block> getBlocksByRank();

    default String tierKey() {
        return PortTiers.key(getPortTypeId(), getInput().orElse(true));
    }

    default NavigableMap<Integer, Block> tierOptions() {
        if (getMinTier() > getMaxTier()) {
            return Collections.emptyNavigableMap();
        }
        return getBlocksByRank().subMap(getMinTier(), true, getMaxTier(), true);
    }

    default int resolveTier(int preferred) {
        return TierResolver.resolve(preferred, getMinTier(), getMaxTier(), getBlocksByRank().navigableKeySet());
    }
}
