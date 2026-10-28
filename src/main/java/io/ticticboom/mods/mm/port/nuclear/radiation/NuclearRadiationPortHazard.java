package io.ticticboom.mods.mm.port.nuclear.radiation;

import igentuman.nr.api.RadiationProfile;
import igentuman.nr.radiation.source.BlockRadSource;
import igentuman.nr.radiation.source.WorldSourceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class NuclearRadiationPortHazard {

    private final UUID id = UUID.randomUUID();

    public void update(ServerLevel level, BlockPos pos, RadiationProfile profile) {
        var registry = WorldSourceRegistry.get(level);
        registry.remove(id);
        if (!profile.isEmpty()) {
            registry.register(new BlockRadSource(id, level.dimension(), pos.immutable(), profile, level.getGameTime(), false));
        }
    }

    public void remove(ServerLevel level) {
        WorldSourceRegistry.get(level).remove(id);
    }
}
