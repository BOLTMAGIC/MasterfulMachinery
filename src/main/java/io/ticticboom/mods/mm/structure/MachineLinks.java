package io.ticticboom.mods.mm.structure;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/** Which formed machine a helper block (Input Gateway, Pattern Port) belongs to. */
public final class MachineLinks {
    private MachineLinks() {
    }

    /**
     * The machine the block at {@code self} is built into (standing in for a casing or glass block), or one whose
     * input port it touches; null if none.
     */
    @Nullable
    public static MachineControllerBlockEntity find(ServerLevel level, BlockPos self) {
        var builtInto = PortSides.findController(level, self, controller -> controller.getStructure() != null
                && controller.getStructure().getPositions(level, controller.getBlockPos()).contains(self));
        if (builtInto != null) {
            return builtInto;
        }
        for (Direction dir : Direction.values()) {
            BlockPos portPos = self.relative(dir);
            if (level.getExistingBlockEntity(portPos) instanceof IPortBlockEntity port && port.isInput()) {
                var controller = PortSides.findController(level, portPos, port.getStorage());
                if (controller != null && controller.getStructure() != null) {
                    return controller;
                }
            }
        }
        return null;
    }
}
