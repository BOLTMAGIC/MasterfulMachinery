package io.ticticboom.mods.mm.port.kinetic.register;

import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortItem;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class CreateKineticPortBlockItem extends BlockItem implements IPortItem {
    private final PortModel model;
    private final RegistryGroupHolder groupHolder;

    public CreateKineticPortBlockItem(PortModel model, RegistryGroupHolder groupHolder) {
        super(groupHolder.getBlock().get(), BlockUtils.blockItemProperties(groupHolder.getItem().getId()));
        this.model = model;
        this.groupHolder = groupHolder;
    }

    @Override
    public Component getTypeName() {
        return Component.translatable("port.mm.create_kinetic.name").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW);
    }

    @Override
    public PortModel getModel() {
        return model;
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
