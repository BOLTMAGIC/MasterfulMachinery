package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlock;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.item.register.ItemPortBlock;
import io.ticticboom.mods.mm.port.fluid.register.FluidPortBlock;
import io.ticticboom.mods.mm.port.energy.register.EnergyPortBlock;
import io.ticticboom.mods.mm.port.kinetic.register.CreateKineticPortBlock;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.register.PneumaticAirPortBlock;
import io.ticticboom.mods.mm.port.botania.mana.register.BotaniaManaPortBlock;
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
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ControllerDataProvider.INSTANCE, MachineControllerBlock.class);

        // register port priority tooltip for known port block classes
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, ItemPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, FluidPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, EnergyPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, CreateKineticPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, PneumaticAirPortBlock.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, BotaniaManaPortBlock.class);
    }
}
