package io.ticticboom.mods.mm.port.replication.matter;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.PortType;
import io.ticticboom.mods.mm.port.replication.matter.compat.ReplicationMatterConfigBuilderJS;
import io.ticticboom.mods.mm.port.replication.matter.register.*;
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

public class ReplicationMatterPortType extends PortType {
    @Override
    public IPortParser getParser() {
        return new ReplicationMatterPortParser();
    }

    /**
     * Replication sorts network members by the interfaces their block entity implements, so input
     * and output ports need separate classes rather than one class with a flag.
     */
    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(), () -> new BlockEntityType<>((p, s) -> model.input()
                ? new ReplicationMatterInputPortBlockEntity(model, groupHolder, p, s)
                : new ReplicationMatterOutputPortBlockEntity(model, groupHolder, p, s), groupHolder.getBlock().get()));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new ReplicationMatterPortBlock(model, groupHolder, model.input()));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new ReplicationMatterPortBlockItem(model, groupHolder));
    }

    @Override
    public DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.MENUS.register(model.id(), () -> IMenuTypeExtension.create((i, o, u) -> new ReplicationMatterPortMenu(model, groupHolder, model.input(), i, o, u)));
    }

    @Override
    public void registerScreen(RegistryGroupHolder groupHolder) {
        MenuScreens.register((MenuType<ReplicationMatterPortMenu>) groupHolder.getMenu().get(), ReplicationMatterPortScreen::new);
    }

    @Override
    public IPortStorageFactory createStorageFactory(Consumer<PortConfigBuilderJS> consumer) {
        var builder = new ReplicationMatterConfigBuilderJS();
        consumer.accept(builder);
        return new ReplicationMatterPortStorageFactory(((ReplicationMatterPortStorageModel) builder.build()));
    }
}
