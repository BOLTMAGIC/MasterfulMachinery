package io.ticticboom.mods.mm.port.replication.link;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.PortType;
import io.ticticboom.mods.mm.port.replication.link.compat.ReplicationLinkConfigBuilderJS;
import io.ticticboom.mods.mm.port.replication.link.register.ReplicationLinkPortBlock;
import io.ticticboom.mods.mm.port.replication.link.register.ReplicationLinkPortBlockEntity;
import io.ticticboom.mods.mm.port.replication.link.register.ReplicationLinkPortBlockItem;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Consumer;

public class ReplicationLinkPortType extends PortType {

    @Override
    public boolean hasSides() {
        return false;
    }

    @Override
    public IPortParser getParser() {
        return new ReplicationLinkPortParser();
    }

    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(), () -> new BlockEntityType<>((p, s) -> new ReplicationLinkPortBlockEntity(model, groupHolder, p, s), groupHolder.getBlock().get()));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new ReplicationLinkPortBlock(model, groupHolder));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new ReplicationLinkPortBlockItem(model, groupHolder));
    }

    @Override
    public DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(PortModel model, RegistryGroupHolder groupHolder) {
        return null;
    }

    @Override
    public void registerScreen(RegistryGroupHolder groupHolder) {
    }

    @Override
    public IPortStorageFactory createStorageFactory(Consumer<PortConfigBuilderJS> consumer) {
        var builder = new ReplicationLinkConfigBuilderJS();
        consumer.accept(builder);
        return new ReplicationLinkPortStorageFactory((ReplicationLinkPortStorageModel) builder.build());
    }
}
