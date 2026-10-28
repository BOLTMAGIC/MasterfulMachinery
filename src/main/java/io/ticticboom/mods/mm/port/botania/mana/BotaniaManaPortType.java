package io.ticticboom.mods.mm.port.botania.mana;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.PortType;
import io.ticticboom.mods.mm.port.botania.mana.compat.BotaniaManaPortBuilderJS;
import io.ticticboom.mods.mm.port.botania.mana.register.BotaniaManaPortBlock;
import io.ticticboom.mods.mm.port.botania.mana.register.BotaniaManaPortBlockEntity;
import io.ticticboom.mods.mm.port.botania.mana.register.BotaniaManaPortBlockItem;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Consumer;

public class BotaniaManaPortType extends PortType {

    @Override
    public IPortParser getParser() {
        return new BotaniaManaPortParser();
    }

    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(), () -> new BlockEntityType<>((a, b) -> new BotaniaManaPortBlockEntity(model, groupHolder, a, b), groupHolder.getBlock().get()));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new BotaniaManaPortBlock(model, groupHolder));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new BotaniaManaPortBlockItem(model, groupHolder));
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
        var b = new BotaniaManaPortBuilderJS();
        consumer.accept(b);
        return new BotaniaManaPortStorageFactory((BotaniaManaPortStorageModel)b.build());
    }
}
