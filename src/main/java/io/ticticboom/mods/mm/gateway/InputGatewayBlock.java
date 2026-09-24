package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.util.BlockUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Placed next to one of a multiblock's input ports, it takes items and fluids from pipes / cables
 * and hands them straight to that machine's input ports. Stores nothing and has no GUI.
 */
public class InputGatewayBlock extends Block implements EntityBlock {

    public InputGatewayBlock() {
        super(BlockUtils.createBlockProperties());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InputGatewayBlockEntity(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.mm.input_gateway.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
