package io.ticticboom.mods.mm.port.common.autoio;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface IPortTransfer {
    /**
     * Moves contents between the port and one neighbour.
     *
     * @param neighbor     the neighbouring block entity
     * @param neighborFace the face of the neighbour that touches the port
     * @param pull         true to move neighbour -> port, false for port -> neighbour
     * @param ticks        ticks since the last transfer, for rate-limited handlers (energy)
     */
    void transfer(BlockEntity neighbor, Direction neighborFace, boolean pull, int ticks);
}
