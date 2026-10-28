package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.network.chat.Component;
import io.ticticboom.mods.mm.util.DisplayNameUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class MachineControllerBlockItem extends BlockItem implements IControllerPart {
    private final ControllerModel model;
    private final RegistryGroupHolder groupHolder;

    public MachineControllerBlockItem(ControllerModel model, RegistryGroupHolder groupHolder) {
        super(groupHolder.getBlock().get(), BlockUtils.blockItemProperties(groupHolder.getItem().getId()));
        this.model = model;
        this.groupHolder = groupHolder;
    }

    @Override
    public ControllerModel getModel() {
        return model;
    }

    /**
     * A plain string name keeps going through the generated block.mm.<id> entry so resource packs
     * can still override it. Only a pack-supplied translation key bypasses it, because the
     * generated entry would otherwise map that key to itself and display as raw text.
     */
    @Override
    public Component getName(ItemStack stack) {
        var name = DisplayNameUtil.packSuppliedName(getModel().displayName());
        return name != null ? name : super.getName(stack);
    }
}
