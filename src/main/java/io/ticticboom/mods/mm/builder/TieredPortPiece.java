package io.ticticboom.mods.mm.builder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;

/** A structure position that takes any port of a type within a tier range (port_type, anywhere or not). */
public interface TieredPortPiece {
    ResourceLocation getPortTypeId();

    Optional<Boolean> getInput();

    int getMinTier();

    int getMaxTier();

    /** Tier -> port block assembly places for it; see {@link PortTiers#byRank}. */
    NavigableMap<Integer, Block> getBlocksByRank();

    /** The {@link TierPrefs} key this position follows. */
    default String tierKey() {
        return PortTiers.key(getPortTypeId(), getInput().orElse(true));
    }

    /** The tiers this position accepts that really exist, with their blocks. */
    default NavigableMap<Integer, Block> tierOptions() {
        if (getMinTier() > getMaxTier()) {
            return Collections.emptyNavigableMap();
        }
        return getBlocksByRank().subMap(getMinTier(), true, getMaxTier(), true);
    }

    /** @return the tier assembly uses here for a preference, or -1 when no tier fits */
    default int resolveTier(int preferred) {
        return TierResolver.resolve(preferred, getMinTier(), getMaxTier(), getBlocksByRank().navigableKeySet());
    }
}
