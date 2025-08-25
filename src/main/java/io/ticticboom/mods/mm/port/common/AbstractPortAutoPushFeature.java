package io.ticticboom.mods.mm.port.common;

import io.ticticboom.mods.mm.model.PortModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;

public abstract class AbstractPortAutoPushFeature<TCoupling extends IHandlerCoupling> {

    protected final BlockEntity portBlockEntity;
    protected final PortModel model;
    protected final HashMap<BlockPos, TCoupling> autoPushNeighbors = new HashMap<>();

    public AbstractPortAutoPushFeature(BlockEntity portBlockEntity, PortModel model) {
        this.portBlockEntity = portBlockEntity;
        this.model = model;
    }

    public void tick() {
        var level = portBlockEntity.getLevel();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        for (var cap : autoPushNeighbors.values()) {
            cap.attemptTransfer();
        }
    }

    public void tryAddNeighboringHandlers() {
        if (!portBlockEntity.hasLevel()) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockPos otherPos = portBlockEntity.getBlockPos().relative(direction, 1);
            tryAddNeighborHandler(otherPos, direction.getOpposite());
        }
    }

    public void onLoad() {
        tryAddNeighboringHandlers();
    }

    protected abstract void tryAddNeighborHandler(BlockPos neighborPos, Direction neighborFace);
}
