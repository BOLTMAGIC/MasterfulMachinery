package io.ticticboom.mods.mm.port.nuclear.radiation.register;

import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortItem;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class NuclearRadiationPortBlockItem extends BlockItem implements IPortItem {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;

    public NuclearRadiationPortBlockItem(PortModel model, RegistryGroupHolder groupHolder) {
        super(groupHolder.getBlock().get(), BlockUtils.blockItemProperties(groupHolder.getItem().getId()));
        this.model = model;
        this.groupHolder = groupHolder;
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("port.mm.nuclear_radiation.name").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public Component getName(ItemStack stack) {
        var name = getModel().displayName();
        return name != null ? name : super.getName(stack);
    }
}
