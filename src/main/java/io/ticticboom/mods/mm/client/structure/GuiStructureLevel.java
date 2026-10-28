package io.ticticboom.mods.mm.client.structure;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class GuiStructureLevel implements BlockAndTintGetter {
    private final Map<BlockPos, BlockState> states = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();

    public void clear() {
        states.clear();
        blockEntities.clear();
    }

    public void put(BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        states.put(pos, state);
        if (be != null) {
            blockEntities.put(pos, be);
        }
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return CardinalLighting.DEFAULT;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getLightEngine() : LevelLightEngine.EMPTY;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getBlockTint(BlockPos.ZERO, resolver) : 0xFFFFFF;
    }

    @Override
    public int getHeight() {
        return 4096;
    }

    @Override
    public int getMinY() {
        return -2048;
    }
}
