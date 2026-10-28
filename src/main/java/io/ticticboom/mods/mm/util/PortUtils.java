package io.ticticboom.mods.mm.util;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.Objects;

public class PortUtils {

    public static String id(String id, boolean input) {
        var res = id + "_" + (input ? "input" : "output");
        return res;
    }

    public static String id(String id, boolean input, boolean sided) {
        return sided ? id(id, input) : id;
    }

    public static boolean sided(Identifier type) {
        var portType = MMPortRegistry.get(type);
        return portType == null || portType.hasSides();
    }

    public static boolean sideMatches(Identifier type, boolean modelInput, java.util.Optional<Boolean> wanted) {
        if (!sided(type)) {
            return true;
        }
        return wanted.isEmpty() || wanted.get() == modelInput;
    }

    public static boolean matchesId(String modelId, boolean modelInput, String rawId) {
        return modelId.equals(rawId) || modelId.equals(id(rawId, modelInput));
    }

    public static String name(String name, boolean input) {
        var res = name + " " + (input ? "Input" : "Output");
        return res;
    }

    /**
     * Component form of {@link #name(String, boolean)}. The Input/Output suffix goes through the
     * lang file so translators control both the wording and its position relative to the name.
     */
    public static Component name(Component name, boolean input) {
        return Component.translatable(input ? "port.mm.name_format.input" : "port.mm.name_format.output", name);
    }

    public static void commonGenerateModel(MMBlockstateProvider provider, RegistryGroupHolder groupHolder,
            boolean isInput, Identifier inputOverlay, Identifier outputOverlay) {
        var block = groupHolder.getBlock().get();
        var base = Ref.Textures.BASE_BLOCK;
        var overlay = isInput ? inputOverlay : outputOverlay;
        Identifier custom = null;
        if (block instanceof IPortBlock portBlock) {
            var model = portBlock.getModel();
            base = Objects.requireNonNullElse(model.baseTexture(), base);
            overlay = Objects.requireNonNullElse(model.overlayTexture(), overlay);
            custom = model.customModel();
        }
        if (custom != null) {
            var mdl = provider.customBlock(groupHolder.getBlock().getId(), custom);
            provider.simpleBlock(block, mdl);
            return;
        }
        provider.dynamicBlock(groupHolder.getBlock().getId(), base, overlay);
        provider.simpleBlock(block);
    }

    public static void fillStageGenerateModel(MMBlockstateProvider provider, RegistryGroupHolder groupHolder,
            boolean isInput, Identifier inputOverlay, Identifier outputOverlay,
            IntegerProperty fill, int maxFill) {
        var block = groupHolder.getBlock().get();
        var packOverridesTextures = block instanceof IPortBlock portBlock
                && (portBlock.getModel().overlayTexture() != null || portBlock.getModel().customModel() != null);
        if (packOverridesTextures) {
            commonGenerateModel(provider, groupHolder, isInput, inputOverlay, outputOverlay);
            return;
        }
        var base = Ref.Textures.BASE_BLOCK;
        if (block instanceof IPortBlock portBlock) {
            base = Objects.requireNonNullElse(portBlock.getModel().baseTexture(), base);
        }
        var overlay = isInput ? inputOverlay : outputOverlay;
        var id = groupHolder.getBlock().getId();

        var models = new Identifier[maxFill + 1];
        models[maxFill] = provider.dynamicBlock(id, base, stageTexture(overlay, maxFill));
        for (int stage = 0; stage < maxFill; stage++) {
            var stageId = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_fill" + stage);
            models[stage] = provider.dynamicBlock(stageId, base, stageTexture(overlay, stage));
        }
        provider.variants(block, state -> new MMBlockstateProvider.Variant(models[state.getValue(fill)], 0));
    }

    private static Identifier stageTexture(Identifier overlay, int stage) {
        return Identifier.fromNamespaceAndPath(overlay.getNamespace(), overlay.getPath() + "_fill" + stage);
    }
}
