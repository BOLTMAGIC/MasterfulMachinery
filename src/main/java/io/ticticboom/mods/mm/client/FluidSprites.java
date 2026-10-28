package io.ticticboom.mods.mm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Fluid sprites and tints. Since 26.1 a fluid's textures and tint come from its FluidModel (baked with the block
 * models) rather than IClientFluidTypeExtensions, and atlases are reached through the AtlasManager.
 */
public final class FluidSprites {
    private FluidSprites() {
    }

    public static TextureAtlasSprite still(FluidStack stack) {
        return model(stack).stillMaterial().sprite();
    }

    /** ARGB; opaque white when the fluid is not tinted. */
    public static int tint(FluidStack stack) {
        var source = model(stack).fluidTintSource();
        int color = source == null ? -1 : source.colorAsStack(stack);
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    /** A sprite of the block atlas, as InventoryMenu.BLOCK_ATLAS lookups were. */
    public static TextureAtlasSprite block(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    private static net.minecraft.client.renderer.block.FluidModel model(FluidStack stack) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(stack.getFluid().defaultFluidState());
    }
}
