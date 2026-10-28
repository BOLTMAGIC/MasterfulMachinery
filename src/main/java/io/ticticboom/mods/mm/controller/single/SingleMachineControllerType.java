package io.ticticboom.mods.mm.controller.single;

import io.ticticboom.mods.mm.controller.ControllerType;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockItem;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerMenu;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerScreen;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineBlock;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineBlockEntity;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SingleMachineControllerType extends ControllerType {

    @Override
    public RegistryGroupHolder register(ControllerModel model) {
        SingleMachines.slots(model);
        return super.register(model);
    }

    @Override
    public DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntity(ControllerModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCK_ENTITIES.register(model.id(), () -> new BlockEntityType<>((p, s) -> new SingleMachineBlockEntity(model, groupHolder, p, s), groupHolder.getBlock().get()));
    }

    @Override
    public DeferredHolder<Block, Block> registerBlock(ControllerModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.BLOCKS.register(model.id(), () -> new SingleMachineBlock(model, groupHolder));
    }

    @Override
    public DeferredHolder<Item, Item> registerItem(ControllerModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.ITEMS.register(model.id(), () -> new MachineControllerBlockItem(model, groupHolder));
    }

    @Override
    public DeferredHolder<MenuType<?>, MenuType<?>> registerMenu(ControllerModel model, RegistryGroupHolder groupHolder) {
        return MMRegisters.MENUS.register(model.id(), () -> IMenuTypeExtension.create((i, o, u) -> new MachineControllerMenu(model, groupHolder, i, o, u)));
    }

    @Override
    public void registerScreen(RegistryGroupHolder groupHolder) {
        MenuScreens.register((MenuType<MachineControllerMenu>) groupHolder.getMenu().get(), MachineControllerScreen::new);
    }
}
