package io.ticticboom.mods.mm.port.projecte.emc;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.PortType;
import io.ticticboom.mods.mm.port.projecte.emc.compat.ProjectEEmcConfigBuilderJS;
import io.ticticboom.mods.mm.port.projecte.emc.register.ProjectEEmcPortBlock;
import io.ticticboom.mods.mm.port.projecte.emc.register.ProjectEEmcPortBlockEntity;
import io.ticticboom.mods.mm.port.projecte.emc.register.ProjectEEmcPortBlockItem;
import io.ticticboom.mods.mm.port.projecte.emc.register.ProjectEEmcPortMenu;
import io.ticticboom.mods.mm.port.projecte.emc.register.ProjectEEmcPortScreen;
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

public class ProjectEEmcPortType extends PortType {

    @Override
    public IPortParser getParser() {
        return new ProjectEEmcPortParser();
    }

    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(), () -> new BlockEntityType<>((p, s) -> new ProjectEEmcPortBlockEntity(model, groupHolder, p, s), groupHolder.getBlock().get()));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new ProjectEEmcPortBlock(model, groupHolder));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new ProjectEEmcPortBlockItem(model, groupHolder));
    }

    @Override
    public DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.MENUS.register(model.id(), () -> IMenuTypeExtension.create((i, o, u) -> new ProjectEEmcPortMenu(model, groupHolder, i, o, u)));
    }

    @Override
    public void registerScreen(RegistryGroupHolder groupHolder) {
        MenuScreens.register((MenuType<ProjectEEmcPortMenu>) groupHolder.getMenu().get(), ProjectEEmcPortScreen::new);
    }

    @Override
    public IPortStorageFactory createStorageFactory(Consumer<PortConfigBuilderJS> consumer) {
        var builder = new ProjectEEmcConfigBuilderJS();
        consumer.accept(builder);
        return new ProjectEEmcPortStorageFactory((ProjectEEmcPortStorageModel) builder.build());
    }
}
