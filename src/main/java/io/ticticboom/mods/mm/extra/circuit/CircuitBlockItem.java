package io.ticticboom.mods.mm.extra.circuit;

import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.extra.ExtraBlockModel;
import io.ticticboom.mods.mm.extra.IExtraBlockPart;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.network.chat.Component;
import io.ticticboom.mods.mm.util.DisplayNameUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class CircuitBlockItem extends BlockItem implements IExtraBlockPart {

    private final ExtraBlockModel model;
    private final RegistryGroupHolder groupHolder;

    public CircuitBlockItem(ExtraBlockModel model, RegistryGroupHolder groupHolder) {
        super(groupHolder.getBlock().get(), BlockUtils.blockItemProperties(groupHolder.getItem().getId()));
        this.model = model;
        this.groupHolder = groupHolder;
    }

    @Override
    public ExtraBlockModel getModel() {
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
