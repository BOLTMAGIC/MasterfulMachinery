package io.ticticboom.mods.mm.structure;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/** Which formed machine a helper block (Input Gateway, Pattern Port) belongs to. */
public final class MachineLinks {
    private static final int SEARCH_RADIUS = 16;

    private MachineLinks() {
    }

    /**
     * The formed machine whose input port the block at {@code self} touches, or the one it is built into (standing
     * in for a casing or glass block); null if none. Only looks in loaded chunks.
     */
    @Nullable
    public static MachineControllerBlockEntity find(ServerLevel level, BlockPos self) {
        for (Direction dir : Direction.values()) {
            BlockPos portPos = self.relative(dir);
            if (level.isLoaded(portPos) && level.getBlockEntity(portPos) instanceof AbstractPortBlockEntity port
                    && port.isInput() && port.getControllerPos() != null
                    && level.getBlockEntity(port.getControllerPos()) instanceof MachineControllerBlockEntity controller
                    && controller.getPortStorages() != null) {
                return controller;
            }
        }
        for (int cx = (self.getX() - SEARCH_RADIUS) >> 4; cx <= (self.getX() + SEARCH_RADIUS) >> 4; cx++) {
            for (int cz = (self.getZ() - SEARCH_RADIUS) >> 4; cz <= (self.getZ() + SEARCH_RADIUS) >> 4; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof MachineControllerBlockEntity controller && be.getBlockPos().closerThan(self, SEARCH_RADIUS + 1)
                            && controller.getPortStorages() != null && controller.getFormedRotation() != null
                            && controller.getStructure().layout().contains(controller.getBlockPos(), controller.getFormedRotation(), self)) {
                        return controller;
                    }
                }
            }
        }
        return null;
    }
}
