package io.ticticboom.mods.mm.port.fluid.register;

import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortItem;
import io.ticticboom.mods.mm.port.IPortPart;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class FluidPortBlockItem extends BlockItem implements IPortItem {

    private PortModel model;

    public FluidPortBlockItem(PortModel model, RegistryGroupHolder groupHolder, boolean isInput) {
        super(groupHolder.getBlock().get(), BlockUtils.blockItemProperties(groupHolder.getItem().getId()));
        this.model = model;
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("port.mm.fluid.name").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA);
    }

    /**
     * Ports always display their model name rather than the generated block.mm.<id> entry. That
     * entry is built from the raw string name, which both loses the translation key when a pack
     * supplies one and carries a hardcoded English Input/Output suffix; displayName() resolves the
     * pack name and runs the suffix through port.mm.name_format.*.
     */
    @Override
    public Component getName(ItemStack stack) {
        var name = getModel().displayName();
        return name != null ? name : super.getName(stack);
    }
}
