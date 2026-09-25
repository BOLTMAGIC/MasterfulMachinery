package io.ticticboom.mods.mm.port.common.autoio;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Transfer strategies for {@link PortAutoIO}, all built on the standard Forge capabilities
 * so they work with any neighbor (chests, tanks, pipes, cables, other ports).
 */
public final class PortTransfers {

    private PortTransfers() {
    }

    public static IPortTransfer items(Supplier<IItemHandler> self) {
        return between(MMCapabilities.ITEM, self, PortTransfers::moveItems);
    }

    public static IPortTransfer fluids(Supplier<IFluidHandler> self) {
        return between(MMCapabilities.FLUID, self, PortTransfers::moveFluids);
    }

    public static IPortTransfer energy(Supplier<IEnergyStorage> self) {
        return (neighbor, face, pull, ticks) -> neighbor.getCapability(MMCapabilities.ENERGY, face).ifPresent(other -> {
            IEnergyStorage own = self.get();
            // energy handlers are rate limited per call, so repeat once per elapsed tick
            for (int i = 0; i < ticks; i++) {
                if (!(pull ? moveEnergy(other, own) : moveEnergy(own, other))) {
                    break;
                }
            }
        });
    }

    public static <H> IPortTransfer between(Capability<H> cap, Supplier<H> self, BiConsumer<H, H> mover) {
        return (neighbor, face, pull, ticks) -> neighbor.getCapability(cap, face).ifPresent(other -> {
            H own = self.get();
            if (pull) {
                mover.accept(other, own);
            } else {
                mover.accept(own, other);
            }
        });
    }

    public static void moveItems(IItemHandler from, IItemHandler to) {
        for (int slot = 0; slot < from.getSlots(); slot++) {
            // port slots can hold more than a stack (slotCapacity), so move in stack-sized chunks:
            // not every inventory accepts oversized stacks
            int budget = from.getSlotLimit(slot);
            while (budget > 0) {
                ItemStack inSlot = from.getStackInSlot(slot);
                if (inSlot.isEmpty()) {
                    break;
                }
                int moved = moveItemChunk(from, to, slot, Math.min(budget, inSlot.getMaxStackSize()));
                if (moved <= 0) {
                    break;
                }
                budget -= moved;
            }
        }
    }

    private static int moveItemChunk(IItemHandler from, IItemHandler to, int slot, int amount) {
        ItemStack available = from.extractItem(slot, amount, true);
        if (available.isEmpty()) {
            return 0;
        }
        ItemStack notAccepted = ItemHandlerHelper.insertItemStacked(to, available, true);
        int count = available.getCount() - notAccepted.getCount();
        if (count <= 0) {
            return 0;
        }
        ItemStack extracted = from.extractItem(slot, count, false);
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(to, extracted, false);
        if (!leftover.isEmpty()) {
            // the target changed between simulate and execute, give the rest back
            from.insertItem(slot, leftover, false);
        }
        return extracted.getCount() - leftover.getCount();
    }

    public static void moveFluids(IFluidHandler from, IFluidHandler to) {
        for (int tank = 0; tank < from.getTanks(); tank++) {
            FluidStack fluid = from.getFluidInTank(tank);
            if (!fluid.isEmpty()) {
                // tryFluidTransfer(destination, source, resource, doTransfer): "to" comes first, and true actually moves it
                FluidUtil.tryFluidTransfer(to, from, fluid.copy(), true);
            }
        }
    }

    public static boolean moveEnergy(IEnergyStorage from, IEnergyStorage to) {
        int canExtract = from.extractEnergy(Integer.MAX_VALUE, true);
        if (canExtract <= 0) {
            return false;
        }
        int canReceive = to.receiveEnergy(canExtract, true);
        if (canReceive <= 0) {
            return false;
        }
        int extracted = from.extractEnergy(canReceive, true);
        if (extracted <= 0) {
            return false;
        }
        int inserted = to.receiveEnergy(extracted, false);
        if (inserted <= 0) {
            return false;
        }
        from.extractEnergy(inserted, false);
        return true;
    }
}
