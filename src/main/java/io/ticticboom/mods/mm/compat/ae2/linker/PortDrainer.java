package io.ticticboom.mods.mm.compat.ae2.linker;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

public final class PortDrainer {
    private static final boolean CHEMICALS = ModList.get().isLoaded("appmek") && ModList.get().isLoaded("mekanism");

    private PortDrainer() {
    }

    public static boolean drain(IPortStorage storage, MEStorage network, IActionSource source) {
        return drain(storage, network, source, stack -> 0, new HashMap<>());
    }

    public static boolean drain(IPortStorage storage, MEStorage network, IActionSource source, ToIntFunction<ItemStack> reserved, Map<AEItemKey, Integer> kept) {
        boolean moved = false;
        var items = storage.getCapability(MMCapabilities.ITEM);
        if (items != null) {
            moved |= drainItems(items, network, source, reserved, kept);
        }
        var fluids = storage.getCapability(MMCapabilities.FLUID);
        if (fluids != null) {
            moved |= drainFluids(fluids, network, source);
        }
        // [26.1-pending] Mekanism has no 26.1.2 release yet
        // if (CHEMICALS) {
        // moved |= ChemicalDrainer.drain(storage, network, source);
        // }
        return moved;
    }

    private static boolean drainItems(IItemHandler handler, MEStorage network, IActionSource source, ToIntFunction<ItemStack> reserved, Map<AEItemKey, Integer> kept) {
        boolean moved = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack available = handler.extractItem(slot, Integer.MAX_VALUE, true);
            if (available.isEmpty()) {
                continue;
            }
            AEItemKey key = AEItemKey.of(available);
            int count = available.getCount();
            int keep = Math.min(count, reserved.applyAsInt(available) - kept.getOrDefault(key, 0));
            if (keep > 0) {
                kept.merge(key, keep, Integer::sum);
                count -= keep;
            }
            if (count <= 0) {
                continue;
            }
            long accepted = network.insert(key, count, Actionable.SIMULATE, source);
            if (accepted > 0) {
                ItemStack extracted = handler.extractItem(slot, (int) accepted, false);
                moved |= network.insert(key, extracted.getCount(), Actionable.MODULATE, source) > 0;
            }
        }
        return moved;
    }

    private static boolean drainFluids(IFluidHandler handler, MEStorage network, IActionSource source) {
        boolean moved = false;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stored = handler.getFluidInTank(tank);
            if (stored.isEmpty()) {
                continue;
            }
            AEFluidKey key = AEFluidKey.of(stored);
            long accepted = network.insert(key, stored.getAmount(), Actionable.SIMULATE, source);
            if (accepted > 0) {
                FluidStack drained = handler.drain(stored.copyWithAmount((int) accepted), IFluidHandler.FluidAction.EXECUTE);
                moved |= network.insert(key, drained.getAmount(), Actionable.MODULATE, source) > 0;
            }
        }
        return moved;
    }
}
