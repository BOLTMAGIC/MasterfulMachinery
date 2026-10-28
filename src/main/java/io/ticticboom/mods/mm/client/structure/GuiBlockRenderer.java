package io.ticticboom.mods.mm.client.structure;

import io.ticticboom.mods.mm.piece.modifier.StructurePieceModifier;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * One block of a structure preview: its state (and block entity) after the piece's modifiers. Since 26.1 the
 * drawing is done by StructurePreviewRenderer, as a picture-in-picture.
 */
@OnlyIn(Dist.CLIENT)
public class GuiBlockRenderer {
    private final Block block;
    private BlockEntity be;
    private BlockState state;
    private final List<StructurePieceModifier> modifiers;
    private final BlockState fixedState;
    private BlockPos pos;

    public GuiBlockRenderer(Block block, List<StructurePieceModifier> modifiers) {
        this.block = block;
        this.modifiers = modifiers;
        this.fixedState = null;
    }

    public GuiBlockRenderer(BlockState state) {
        this.block = state.getBlock();
        this.modifiers = List.of();
        this.fixedState = state;
    }

    public void setupAt(BlockPos pos) {
        this.pos = pos;
        state = fixedState != null ? fixedState : block.defaultBlockState();
        if (block instanceof EntityBlock eb) {
            be = eb.newBlockEntity(pos, state);
        }
        for (StructurePieceModifier mod : modifiers) {
            state = mod.modifyBlockState(state, be, pos);
            if (be != null) {
                be = mod.modifyBlockEntity(state, be, pos);
            }
        }
    }

    public BlockState getState() {
        return state;
    }

    @Nullable
    public BlockEntity getBlockEntity() {
        return be;
    }

    public boolean isOpaqueCube() {
        return be == null && state != null && state.isSolidRender();
    }
}
