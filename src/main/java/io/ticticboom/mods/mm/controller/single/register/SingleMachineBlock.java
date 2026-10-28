package io.ticticboom.mods.mm.controller.single.register;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlock;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SingleMachineBlock extends MachineControllerBlock {

    public SingleMachineBlock(ControllerModel model, RegistryGroupHolder groupHolder) {
        super(model, groupHolder);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof SingleMachineBlockEntity machine) {
            machine.openSlots(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
