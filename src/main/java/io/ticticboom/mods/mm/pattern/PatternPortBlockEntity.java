package io.ticticboom.mods.mm.pattern;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.MachineLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Pattern Port: finds the machine it belongs to (like an Input Gateway) and remembers why the last push was
 * refused, for Jade. The pattern runs themselves live in the controller.
 */
public class PatternPortBlockEntity extends BlockEntity {
    /** How long the found machine is trusted before looking again, in ticks. */
    private static final int REFRESH_TICKS = 20;

    @Nullable
    private BlockPos controllerPos;
    private long refreshAt = Long.MIN_VALUE;
    @Nullable
    private PatternStartResult lastResult;

    public PatternPortBlockEntity(BlockPos pos, BlockState state) {
        super(MMRegisters.PATTERN_PORT_BE.get(), pos, state);
    }

    /** The formed machine this port belongs to, or null. Looked up again at most once a second. */
    @Nullable
    public MachineControllerBlockEntity machine() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        long now = serverLevel.getGameTime();
        if (now >= refreshAt) {
            refreshAt = now + REFRESH_TICKS;
            var found = MachineLinks.find(serverLevel, getBlockPos());
            controllerPos = found == null ? null : found.getBlockPos();
        }
        if (controllerPos != null && serverLevel.getExistingBlockEntity(controllerPos) instanceof MachineControllerBlockEntity controller
                && controller.getStructure() != null) {
            return controller;
        }
        return null;
    }

    @Nullable
    public PatternStartResult lastResult() {
        return lastResult;
    }

    public void setLastResult(@Nullable PatternStartResult result) {
        this.lastResult = result;
    }
}
