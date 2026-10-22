package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlock;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;


@WailaPlugin
public class MMWailaPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ControllerDataProvider.INSTANCE, MachineControllerBlockEntity.class);
        registration.registerBlockDataProvider(PortPriorityDataProvider.INSTANCE, AbstractPortBlockEntity.class);
        registration.registerBlockDataProvider(PortMachineDataProvider.INSTANCE, AbstractPortBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ControllerDataProvider.INSTANCE, MachineControllerBlock.class);

        // every block, so optional ports (Mekanism...) are covered without loading their classes; only MM ports get server data
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(PortMachineDataProvider.INSTANCE, Block.class);
    }
}
