package io.ticticboom.mods.mm.controller;

import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

public abstract class ControllerType {

    public abstract DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(ControllerModel model, RegistryGroupHolder groupHolder);

    public abstract DeferredHolder<Block, Block> registerBlock(ControllerModel model, RegistryGroupHolder groupHolder);

    public abstract DeferredHolder<Item, Item> registerItem(ControllerModel model, RegistryGroupHolder groupHolder);

    public abstract DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(ControllerModel model, RegistryGroupHolder groupHolder);

    public abstract void registerScreen(RegistryGroupHolder groupHolder);

    public RegistryGroupHolder register(ControllerModel model) {
        RegistryGroupHolder groupHolder = new RegistryGroupHolder();
        groupHolder.setMenu(registerMenu(model, groupHolder));
        groupHolder.setBlock(registerBlock(model, groupHolder));
        groupHolder.setBe(registerBlockEntity(model, groupHolder));
        groupHolder.setItem(registerItem(model, groupHolder));
        groupHolder.setRegistryId(model.type());
        MMControllerRegistry.CONTROLLERS.add(groupHolder);
        return groupHolder;
    }
}
