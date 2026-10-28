package io.ticticboom.mods.mm.port.replication.link.register;

import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortItem;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class ReplicationLinkPortBlockItem extends BlockItem implements IPortItem {

    private final PortModel model;

    public ReplicationLinkPortBlockItem(PortModel model, RegistryGroupHolder groupHolder) {
        super(groupHolder.getBlock().get(), BlockUtils.blockItemProperties(groupHolder.getItem().getId()));
        this.model = model;
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("port.mm.replication_link.name").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA);
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
