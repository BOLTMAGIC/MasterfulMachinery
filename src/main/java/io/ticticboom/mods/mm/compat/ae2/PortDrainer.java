package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.compat.appmek.ChemicalDrainer;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;

/**
 * Moves a port's contents into an ME storage. Every transfer first asks the network how much
 * it would accept (SIMULATE) and only takes that much out of the port, so nothing is ever
 * removed from a port that the network can't hold.
 */
public final class PortDrainer {
    private static final boolean CHEMICALS = ModList.get().isLoaded("appmek") && ModList.get().isLoaded("mekanism");

    private PortDrainer() {
    }

    public static void drain(IPortStorage storage, MEStorage network, IActionSource source) {
        storage.getCapability(MMCapabilities.ITEM).ifPresent(items -> drainItems(items, network, source));
        storage.getCapability(MMCapabilities.FLUID).ifPresent(fluids -> drainFluids(fluids, network, source));
        if (CHEMICALS) {
            ChemicalDrainer.drain(storage, network, source);
        }
    }

    private static void drainItems(IItemHandler handler, MEStorage network, IActionSource source) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            // port slots may hold more than a stack, so ask for everything in the slot
            ItemStack available = handler.extractItem(slot, Integer.MAX_VALUE, true);
            if (available.isEmpty()) {
                continue;
            }
            AEItemKey key = AEItemKey.of(available);
            long accepted = network.insert(key, available.getCount(), Actionable.SIMULATE, source);
            if (accepted > 0) {
                ItemStack extracted = handler.extractItem(slot, (int) accepted, false);
                network.insert(key, extracted.getCount(), Actionable.MODULATE, source);
            }
        }
    }

    private static void drainFluids(IFluidHandler handler, MEStorage network, IActionSource source) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stored = handler.getFluidInTank(tank);
            if (stored.isEmpty()) {
                continue;
            }
            AEFluidKey key = AEFluidKey.of(stored);
            long accepted = network.insert(key, stored.getAmount(), Actionable.SIMULATE, source);
            if (accepted > 0) {
                FluidStack drained = handler.drain(new FluidStack(stored, (int) accepted), IFluidHandler.FluidAction.EXECUTE);
                network.insert(key, drained.getAmount(), Actionable.MODULATE, source);
            }
        }
    }
}
