package io.ticticboom.mods.mm.port.mekanism.heat.feature;

import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.mekanism.heat.MekanismHeatPortHandler;
import io.ticticboom.mods.mm.port.mekanism.heat.MekanismHeatPortStorage;
import io.ticticboom.mods.mm.port.mekanism.heat.register.MekanismHeatPortBlockEntity;
import mekanism.api.heat.IHeatHandler;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

import java.util.EnumMap;
import java.util.Map;

public class MekanismHeatPortTransferFeature {

    private final MekanismHeatPortBlockEntity portBlockEntity;
    private final Map<Direction, BlockCapabilityCache<IHeatHandler, Direction>> neighbours = new EnumMap<>(Direction.class);
    private boolean resolved = false;

    public MekanismHeatPortTransferFeature(MekanismHeatPortBlockEntity portBlockEntity) {
        this.portBlockEntity = portBlockEntity;
    }

    public void onLoad() {
        neighborsChanged();
    }

    public void neighborsChanged() {
        neighbours.clear();
        resolved = false;
    }

    public void tick() {
        if (!(portBlockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!resolved) {
            resolve(level);
        }
        var storage = portBlockEntity.getStorage();
        if (!(storage instanceof MekanismHeatPortStorage heatStorage)) {
            return;
        }
        MekanismHeatPortHandler handler = heatStorage.getHandler();
        boolean pushing = !portBlockEntity.isInput();
        if (pushing && handler.getStored() <= 0) {
            return;
        }
        for (var cache : neighbours.values()) {
            var adjacent = cache.getCapability();
            if (adjacent == null) {
                continue;
            }
            handler.exchangeWith(adjacent, pushing);
        }
    }

    private void resolve(ServerLevel level) {
        resolved = true;
        neighbours.clear();
        var pos = portBlockEntity.getBlockPos();
        for (Direction direction : Direction.values()) {
            var neighbourPos = pos.relative(direction);
            var neighbourBe = level.getBlockEntity(neighbourPos);
            if (neighbourBe instanceof IPortBlockEntity port && port.isInput() == portBlockEntity.isInput()) {
                continue;
            }
            if (level.getCapability(MekCapabilities.HEAT, neighbourPos, direction.getOpposite()) == null) {
                continue;
            }
            neighbours.put(direction, BlockCapabilityCache.create(MekCapabilities.HEAT, level, neighbourPos, direction.getOpposite()));
        }
    }
}
