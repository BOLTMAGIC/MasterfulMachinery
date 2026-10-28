package io.ticticboom.mods.mm.extra.gearbox;

import io.ticticboom.mods.mm.extra.ExtraBlockModel;
import io.ticticboom.mods.mm.extra.ExtraBlockType;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

public class GearboxBlockType extends ExtraBlockType {
    @Override
    public DeferredHolder<Block, Block> registerBlock(ExtraBlockModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new GearboxBlock(model, groupHolder));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(ExtraBlockModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new GearboxBlockItem(model, groupHolder));
    }
}
