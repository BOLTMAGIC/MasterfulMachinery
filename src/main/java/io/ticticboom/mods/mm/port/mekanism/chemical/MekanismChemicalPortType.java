package io.ticticboom.mods.mm.port.mekanism.chemical;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortParser;
import io.ticticboom.mods.mm.port.IPortStorageFactory;
import io.ticticboom.mods.mm.port.PortType;
import io.ticticboom.mods.mm.port.mekanism.chemical.compat.MekanismChemicalConfigBuilderJS;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortBlock;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortBlockEntity;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortBlockItem;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortMenu;
import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortScreen;
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

public class MekanismChemicalPortType extends PortType {
    @Override
    public IPortParser getParser() {
        return new MekanismChemicalPortParser();
    }

    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(), () -> new BlockEntityType<>(
                (i, o) -> new MekanismChemicalPortBlockEntity(model, groupHolder, model.input(), i, o),
                groupHolder.getBlock().get()));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new MekanismChemicalPortBlock(model, groupHolder, model.input()));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new MekanismChemicalPortBlockItem(model, groupHolder));
    }

    @Override
    public DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(PortModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.MENUS.register(model.id(), () -> IMenuTypeExtension.create((i, o, u) -> new MekanismChemicalPortMenu(model, groupHolder, i, o, u)));
    }

    @Override
    public void registerScreen(RegistryGroupHolder groupHolder) {
        MenuScreens.<MekanismChemicalPortMenu, MekanismChemicalPortScreen<MekanismChemicalPortMenu>>register(
                (MenuType<MekanismChemicalPortMenu>) groupHolder.getMenu().get(),
                (menu, inv, title) -> new MekanismChemicalPortScreen<>(menu, inv, title));
    }

    @Override
    public IPortStorageFactory createStorageFactory(Consumer<PortConfigBuilderJS> consumer) {
        var builder = new MekanismChemicalConfigBuilderJS();
        consumer.accept(builder);
        return new MekanismChemicalPortStorageFactory(((MekanismChemicalPortStorageModel) builder.build()));
    }
}
