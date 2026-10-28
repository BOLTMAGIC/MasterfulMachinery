package io.ticticboom.mods.mm.port.projecte.emc.feature;

import io.ticticboom.mods.mm.cap.ProjectECapabilities;
import io.ticticboom.mods.mm.port.projecte.emc.ProjectEEmcPortStorage;
import io.ticticboom.mods.mm.port.projecte.emc.register.ProjectEEmcPortBlockEntity;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

import java.util.ArrayList;
import java.util.List;

public class ProjectEEmcPushFeature {

    private static final Direction[] DIRECTIONS = Direction.values();

    private final ProjectEEmcPortBlockEntity port;
    private final List<BlockCapabilityCache<IEmcStorage, Direction>> neighbours = new ArrayList<>(DIRECTIONS.length);
    private final List<IEmcStorage> acceptors = new ArrayList<>(DIRECTIONS.length);

    public ProjectEEmcPushFeature(ProjectEEmcPortBlockEntity port) {
        this.port = port;
    }

    public void onLoad() {
        neighbours.clear();
        if (port.isInput() || !(port.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (Direction dir : DIRECTIONS) {
            neighbours.add(BlockCapabilityCache.create(ProjectECapabilities.EMC_STORAGE, level,
                    port.getBlockPos().relative(dir), dir.getOpposite()));
        }
    }

    public void tick() {
        ProjectEEmcPortStorage storage = port.getEmcStorage();
        long available = storage.getStored();
        if (available <= 0 || neighbours.isEmpty()) {
            return;
        }
        acceptors.clear();
        for (var cache : neighbours) {
            IEmcStorage target = cache.getCapability();
            if (target != null && target.insertEmc(1, IEmcStorage.EmcAction.SIMULATE) > 0) {
                acceptors.add(target);
            }
        }
        for (int i = 0; i < acceptors.size() && available > 0; i++) {
            long share = Math.max(1, available / (acceptors.size() - i));
            long given = acceptors.get(i).insertEmc(share, IEmcStorage.EmcAction.EXECUTE);
            if (given > 0) {
                available -= storage.extract(given, false);
            }
        }
        acceptors.clear();
    }
}
