package io.ticticboom.mods.mm.port.item;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.PortType;
import io.ticticboom.mods.mm.port.item.compat.ItemPortConfigBuilderJS;
import io.ticticboom.mods.mm.port.item.register.*;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Consumer;

public class ItemPortType extends PortType {
    @Override
    public IPortParser getParser() {
        return new ItemPortParser();
    }

    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(),
                () -> new BlockEntityType<>((p, s) -> new ItemPortBlockEntity(groupHolder, model, model.input(), p, s),
                                groupHolder.getBlock().get()));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new ItemPortBlock(model, groupHolder, model.input()));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new ItemPortBlockItem(model, groupHolder, model.input()));
    }

    @Override
    public DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.MENUS.register(model.id(),
                () -> IMenuTypeExtension.create((i, o, u) -> new ItemPortMenu(model, groupHolder, model.input(), i, o, u)));
    }

    @Override
    public void registerScreen(RegistryGroupHolder groupHolder) {
        MenuScreens.register((MenuType<ItemPortMenu>) groupHolder.getMenu().get(), ItemPortScreen::new);
    }

    @Override
    public IPortStorageFactory createStorageFactory(Consumer<PortConfigBuilderJS> consumer) {
        var builder = new ItemPortConfigBuilderJS();
        consumer.accept(builder);
        return new ItemPortStorageFactory(((ItemPortStorageModel) builder.build()));
    }
}
