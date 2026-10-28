package io.ticticboom.mods.mm.port.common.autoio;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class PortTransfers {

    private PortTransfers() {
    }

    public static IPortTransfer items(Supplier<IItemHandler> self) {
        return between(MMCapabilities::item, self, PortTransfers::moveItems);
    }

    public static IPortTransfer fluids(Supplier<IFluidHandler> self) {
        return between(MMCapabilities::fluid, self, PortTransfers::moveFluids);
    }

    public static IPortTransfer energy(Supplier<IEnergyStorage> self) {
        return (level, pos, face, pull, ticks) -> {
            var other = MMCapabilities.energy(level, pos, face);
            if (other == null) {
                return;
            }
            IEnergyStorage own = self.get();
            for (int i = 0; i < ticks; i++) {
                if (!(pull ? moveEnergy(other, own) : moveEnergy(own, other))) {
                    break;
                }
            }
        };
    }

    /** Finds a handler of one kind on a block, MM's or another mod's. */
    @FunctionalInterface
    public interface HandlerLookup<H> {
        @Nullable H find(Level level, BlockPos pos, @Nullable Direction side);
    }

    public static <H> IPortTransfer between(HandlerLookup<H> lookup, Supplier<H> self, BiConsumer<H, H> mover) {
        return (level, pos, face, pull, ticks) -> {
            var other = lookup.find(level, pos, face);
            if (other == null) {
                return;
            }
            H own = self.get();
            if (pull) {
                mover.accept(other, own);
            } else {
                mover.accept(own, other);
            }
        };
    }

    public static void moveItems(IItemHandler from, IItemHandler to) {
        for (int slot = 0; slot < from.getSlots(); slot++) {
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
            from.insertItem(slot, leftover, false);
        }
        return extracted.getCount() - leftover.getCount();
    }

    public static void moveFluids(IFluidHandler from, IFluidHandler to) {
        for (int tank = 0; tank < from.getTanks(); tank++) {
            FluidStack fluid = from.getFluidInTank(tank);
            if (!fluid.isEmpty()) {
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
