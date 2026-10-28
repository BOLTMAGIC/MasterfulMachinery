package io.ticticboom.mods.mm.port.common.autoio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface IPortTransfer {
    void transfer(ServerLevel level, BlockPos neighbor, Direction neighborFace, boolean pull, int ticks);
}
