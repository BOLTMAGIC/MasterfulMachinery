package io.ticticboom.mods.mm.compat.waila;

import net.minecraft.world.level.block.Block;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlock;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.pattern.PatternPortBlock;
import io.ticticboom.mods.mm.pattern.PatternPortBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.replication.matter.register.ReplicationMatterPortBlock;
import net.neoforged.fml.ModList;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
// [26.1-pending] import: import io.ticticboom.mods.mm.port.ars.source.register.ArsSourcePortBlock;
// [26.1-pending] import: import io.ticticboom.mods.mm.port.projecte.emc.register.ProjectEEmcPortBlock;
import io.ticticboom.mods.mm.port.entity.register.EntityPortBlock;
// [26.1-pending] import: import io.ticticboom.mods.mm.port.mekanism.chemical.register.MekanismChemicalPortBlock;
// [26.1-pending] import: import io.ticticboom.mods.mm.port.mekanism.heat.register.MekanismHeatPortBlock;
import io.ticticboom.mods.mm.port.nuclear.radiation.register.NuclearRadiationPortBlock;


@WailaPlugin
public class MMWailaPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ControllerDataProvider.INSTANCE, MachineControllerBlockEntity.class);
        registration.registerBlockDataProvider(PortPriorityDataProvider.INSTANCE, AbstractPortBlockEntity.class);
        registration.registerBlockDataProvider(PortContentsDataProvider.INSTANCE, AbstractPortBlockEntity.class);
        registration.registerBlockDataProvider(PortMachineDataProvider.INSTANCE, AbstractPortBlockEntity.class);
        registration.registerBlockDataProvider(PatternPortDataProvider.INSTANCE, PatternPortBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ControllerDataProvider.INSTANCE, MachineControllerBlock.class);
        registration.registerBlockComponent(PatternPortDataProvider.INSTANCE, PatternPortBlock.class);
        // every block, so optional ports (Mekanism...) are covered without loading their classes; only MM ports get server data
        registration.registerBlockComponent(PortMachineDataProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(PortPriorityDataProvider.INSTANCE, Block.class);

        registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, EntityPortBlock.class);
        // [26.1-pending] Mekanism has no 26.1.2 release yet
        // if (ModList.get().isLoaded("mekanism")) {
        // registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, MekanismChemicalPortBlock.class);
        // registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, MekanismHeatPortBlock.class);
        // }
        // Unlike the other port blocks this one implements a Titanium interface, so naming the
        // class at all resolves it. Behind the check it stays unloaded when Replication is absent.
        if (ModList.get().isLoaded("replication")) {
            registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, ReplicationMatterPortBlock.class);
        }
        // [26.1-pending] Ars Nouveau has no 26.1.2 release yet
        // if (ModList.get().isLoaded("ars_nouveau")) {
        // registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, ArsSourcePortBlock.class);
        // }
        // [26.1-pending] ProjectE has no 26.1.2 release yet
        // if (ModList.get().isLoaded("projecte")) {
        // registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, ProjectEEmcPortBlock.class);
        // }
        if (ModList.get().isLoaded("nuclear_radiation")) {
            registration.registerBlockComponent(PortContentsDataProvider.INSTANCE, NuclearRadiationPortBlock.class);
        }
    }
}
