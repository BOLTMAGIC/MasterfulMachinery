package io.ticticboom.mods.mm.port.ars.source;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.PortType;
import io.ticticboom.mods.mm.port.ars.source.compat.ArsSourceConfigBuilderJS;
import io.ticticboom.mods.mm.port.ars.source.register.ArsSourcePortBlock;
import io.ticticboom.mods.mm.port.ars.source.register.ArsSourcePortBlockEntity;
import io.ticticboom.mods.mm.port.ars.source.register.ArsSourcePortBlockItem;
import io.ticticboom.mods.mm.port.ars.source.register.ArsSourcePortMenu;
import io.ticticboom.mods.mm.port.ars.source.register.ArsSourcePortScreen;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Consumer;

public class ArsSourcePortType extends PortType {

    @Override
    public IPortParser getParser() {
        return new ArsSourcePortParser();
    }

    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(), () -> new BlockEntityType<>((p, s) -> new ArsSourcePortBlockEntity(model, groupHolder, p, s), groupHolder.getBlock().get()));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new ArsSourcePortBlock(model, groupHolder));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new ArsSourcePortBlockItem(model, groupHolder));
    }

    @Override
    public DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.MENUS.register(model.id(), () -> IMenuTypeExtension.create((i, o, u) -> new ArsSourcePortMenu(model, groupHolder, i, o, u)));
    }

    @Override
    public void registerScreen(RegistryGroupHolder groupHolder) {
        MenuScreens.register((MenuType<ArsSourcePortMenu>) groupHolder.getMenu().get(), ArsSourcePortScreen::new);
    }

    @Override
    public IPortStorageFactory createStorageFactory(Consumer<PortConfigBuilderJS> consumer) {
        var builder = new ArsSourceConfigBuilderJS();
        consumer.accept(builder);
        return new ArsSourcePortStorageFactory((ArsSourcePortStorageModel) builder.build());
    }
}
