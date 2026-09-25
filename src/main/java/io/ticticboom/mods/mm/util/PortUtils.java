package io.ticticboom.mods.mm.util;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
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
        var overlay = isInput ? inputOverlay : outputOverlay;
        if (groupHolder.getBlock().get().defaultBlockState().hasProperty(ControllerState.PROPERTY)) {
            provider.portModel(groupHolder.getBlock().getId(), Ref.Textures.BASE_BLOCK, overlay, Ref.Textures.PORT_LIGHT);
        } else {
            provider.dynamicBlock(groupHolder.getBlock().getId(), Ref.Textures.BASE_BLOCK, overlay);
        }
        provider.simpleBlock(groupHolder.getBlock().get());
    }
}
