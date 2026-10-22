package io.ticticboom.mods.mm.pattern;

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
 * Placed like an Input Gateway, it is a crafting machine for an AE2 Pattern Provider next to it: pushed patterns run
 * on the machine straight away, without going through the input ports. Stores nothing itself and has no GUI.
 */
public class PatternPortBlock extends Block implements EntityBlock {

    public PatternPortBlock() {
        super(BlockUtils.createBlockProperties());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PatternPortBlockEntity(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        for (int line = 1; line <= 4; line++) {
            tooltip.add(Component.translatable("block.mm.pattern_port.tooltip." + line).withStyle(ChatFormatting.GRAY));
        }
    }
}
