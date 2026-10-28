package io.ticticboom.mods.mm.port.ae2.pattern.register;

import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortItem;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class Ae2PatternPortBlockItem extends BlockItem implements IPortItem {

    private final PortModel model;

    public Ae2PatternPortBlockItem(PortModel model, RegistryGroupHolder groupHolder) {
        super(groupHolder.getBlock().get(), BlockUtils.blockItemProperties(groupHolder.getItem().getId()));
        this.model = model;
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("port.mm.ae2_pattern.name").withStyle(ChatFormatting.BOLD, ChatFormatting.BLUE);
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
