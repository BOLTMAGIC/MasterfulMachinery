package io.ticticboom.mods.mm.port.mekanism.heat.register;

import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortItem;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class MekanismHeatPortBlockItem extends BlockItem implements IPortItem {
    private final PortModel model;

    public MekanismHeatPortBlockItem(PortModel model, RegistryGroupHolder groupHolder) {
        super(groupHolder.getBlock().get(), BlockUtils.blockItemProperties(groupHolder.getItem().getId()));
        this.model = model;
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("port.mm.mekanism_heat.name").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
    }

    @Override
    public Component getName(ItemStack stack) {
        var name = getModel().displayName();
        return name != null ? name : super.getName(stack);
    }
}
