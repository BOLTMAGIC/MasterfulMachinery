package io.ticticboom.mods.mm.util;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.resources.ResourceLocation;

public class PortUtils {

    public static String id(String id, boolean input) {
        var res = id + "_" + (input ? "input" : "output");
        return res;
    }

    public static String name(String name, boolean input) {
        var res = name + " " + (input ? "Input" : "Output");
        return res;
    }

    public static void commonGenerateModel(MMBlockstateProvider provider, RegistryGroupHolder groupHolder,
            boolean isInput, ResourceLocation inputOverlay, ResourceLocation outputOverlay) {
        // the status light is drawn by PortStatusLightRenderer, so it also shows on packs' own port models
        provider.dynamicBlock(groupHolder.getBlock().getId(), Ref.Textures.BASE_BLOCK, isInput ? inputOverlay : outputOverlay);
        provider.simpleBlock(groupHolder.getBlock().get());
    }
}
