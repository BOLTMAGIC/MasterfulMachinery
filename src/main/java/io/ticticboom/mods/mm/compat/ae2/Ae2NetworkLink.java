package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.features.GridLinkables;
import appeng.api.networking.security.IActionSource;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.MultiblockLookup;
import io.ticticboom.mods.mm.networklink.NetworkLinkProtection;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;

/**
 * AE2 side of network linking. Only loaded when AE2 is installed.
 */
public final class Ae2NetworkLink {

    private Ae2NetworkLink() {
    }

    public static RegistryObject<Item> init() {
        RegistryObject<Item> linker = MMRegisters.ITEMS.register("network_linker", () -> new LinkerItem(new Item.Properties().stacksTo(1)));
        @SuppressWarnings("removal") var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        // like AE2's wireless terminals, the linker can be linked in a Wireless Access Point's slot
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> GridLinkables.register(linker.get(), new LinkerGridLinkable())));
        MinecraftForge.EVENT_BUS.register(new NetworkLinkProtection());
        return linker;
    }

    public static void exportOutputs(ServerLevel level, MachineControllerBlockEntity controller, LinkData link) {
        var storages = MultiblockLookup.storagesOf(level, controller);
        if (storages == null || storages.outputStorages().isEmpty()) {
            return;
        }
        var network = NetworkAccess.storage(level.getServer(), link.network());
        if (network == null) {
            return;
        }
        for (IPortStorage output : storages.outputStorages()) {
            PortDrainer.drain(output, network, IActionSource.empty());
        }
    }

    public static void sendPortContents(ServerLevel level, BlockPos pos, IPortBlockEntity port) {
        try {
            var controller = MultiblockLookup.findLinkedController(level, pos, port.getStorage());
            if (controller == null) {
                return;
            }
            var network = NetworkAccess.storage(level.getServer(), controller.getNetworkLink().network());
            if (network == null) {
                // network unreachable: the port drops its contents as usual
                return;
            }
            PortDrainer.drain(port.getStorage(), network, IActionSource.empty());
        } catch (RuntimeException e) {
            // never let a failure here break block removal
            Ref.LOG.error("Failed to send port contents at {} to the AE2 network", pos, e);
        }
    }
}
