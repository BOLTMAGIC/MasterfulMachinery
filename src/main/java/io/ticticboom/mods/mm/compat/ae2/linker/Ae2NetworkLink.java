package io.ticticboom.mods.mm.compat.ae2.linker;

import io.ticticboom.mods.mm.builder.me.MeAccessFactory;
import io.ticticboom.mods.mm.compat.ae2.Ae2MeAccess;
import appeng.api.features.GridLinkables;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.NetworkLinkProtection;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;

public final class Ae2NetworkLink {

    private Ae2NetworkLink() {
    }

    public static DeferredHolder<Item, Item> init(IEventBus modBus) {
        DeferredHolder<Item, Item> linker = MMRegisters.ITEMS.registerItem("network_linker", properties -> new LinkerItem(properties.stacksTo(1)));
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            var handler = new LinkerGridLinkable();
            GridLinkables.register(linker.get(), handler);
            GridLinkables.register(MMRegisters.MULTIBLOCK_TOOL.get(), handler);
        }));
        NeoForge.EVENT_BUS.register(new NetworkLinkProtection());
        MeAccessFactory.setLookup(Ae2MeAccess::forTool, Ae2MeAccess::problem);
        return linker;
    }

    public static boolean exportOutputs(ServerLevel level, MachineControllerBlockEntity controller, LinkData link) {
        var storages = controller.getPortStorages();
        if (storages == null || storages.outputStorages().isEmpty()) {
            return false;
        }
        var network = NetworkAccess.storage(level.getServer(), link.network());
        if (network == null) {
            return false;
        }
        var kept = new HashMap<AEItemKey, Integer>();
        boolean moved = false;
        for (IPortStorage output : storages.outputStorages()) {
            moved |= PortDrainer.drain(output, network, IActionSource.empty(), controller::reservedOutputCount, kept);
        }
        return moved;
    }

    public static void sendPortContents(ServerLevel level, BlockPos pos, IPortBlockEntity port, LinkData link) {
        try {
            var network = NetworkAccess.storage(level.getServer(), link.network());
            if (network != null) {
                PortDrainer.drain(port.getStorage(), network, IActionSource.empty());
            }
        } catch (RuntimeException e) {
            Ref.LOG.error("Failed to send port contents at {} to the AE2 network", pos, e);
        }
    }
}
