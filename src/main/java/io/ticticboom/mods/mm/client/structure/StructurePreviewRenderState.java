package io.ticticboom.mods.mm.client.structure;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One frame of a structure preview in a screen. Since 1.21.6 a screen can't draw 3D itself; a picture-in-picture
 * renderer draws this into its own texture, which the GUI then blits.
 *
 * @param blocks    the blocks to draw, in structure coordinates
 * @param level     the preview level the blocks look their neighbours up in (connected textures)
 * @param transform pan, rotation and model transform, in blocks
 * @param scale     GUI pixels per block before the transform's own zoom
 */
public record StructurePreviewRenderState(
        List<Block> blocks,
        GuiStructureLevel level,
        Matrix4f transform,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public record Block(BlockPos pos, BlockState state) {
    }

    public StructurePreviewRenderState(List<Block> blocks, GuiStructureLevel level, Matrix4f transform, int x0, int y0, int x1, int y1,
                                       float scale, @Nullable ScreenRectangle scissorArea) {
        this(blocks, level, transform, x0, y0, x1, y1, scale, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
