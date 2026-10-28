package io.ticticboom.mods.mm.pattern;

import com.mojang.serialization.MapCodec;
import io.ticticboom.mods.mm.util.BlockUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Placed like an Input Gateway, it is a crafting machine for an AE2 Pattern Provider next to it: pushed patterns run
 * on the machine straight away, without going through the input ports. Stores nothing itself and has no GUI.
 */
public class PatternPortBlock extends BaseEntityBlock {
    private static final MapCodec<PatternPortBlock> CODEC = simpleCodec(PatternPortBlock::new);

    public PatternPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PatternPortBlockEntity(pos, state);
    }

}
