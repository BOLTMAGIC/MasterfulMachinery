package io.ticticboom.mods.mm.port.fluid;

import io.ticticboom.mods.mm.cap.transfer.TransferSnapshot;
import com.mojang.serialization.Codec;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import net.minecraft.nbt.*;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FluidPortHandler implements IFluidHandler, TransferSnapshot {

    private final int tanks;
    private final int capacity;
    @Getter
    private final INotifyChangeFunction changed;

    private final ArrayList<FluidStack> stacks;
    private final Fluid[] lockedFluids;
    @Getter
    private boolean locked = false;
    private Predicate<Fluid> filter = fluid -> true;
    private boolean oneFluidPerTank = false;

    // OPTIONAL_CODEC, not CODEC: the strict codec rejects empty stacks, and empty tanks are
    // serialised on every block update. Same split as ItemStack in 1.20.5.
    public static final Codec<List<FluidStack>> STACKS_CODEC = Codec.list(FluidStack.OPTIONAL_CODEC);

    public FluidPortHandler(int tanks, int capacity, INotifyChangeFunction changed) {
        this.tanks = tanks;
        this.capacity = capacity;
        this.changed = changed;
        stacks = new ArrayList<>();
        for (int i = 0; i < tanks; i++) {
            stacks.add(FluidStack.EMPTY);
        }
        lockedFluids = new Fluid[tanks];
    }

    public void setFilter(Predicate<Fluid> filter) {
        this.filter = filter;
    }

    public void setOneFluidPerTank(boolean oneFluidPerTank) {
        this.oneFluidPerTank = oneFluidPerTank;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        for (int i = 0; i < tanks; i++) {
            FluidStack stack = stacks.get(i);
            lockedFluids[i] = locked && !stack.isEmpty() ? stack.getFluid() : null;
        }
        changed.call();
    }

    @Nullable
    public Fluid getLockedFluid(int tank) {
        return lockedFluids[tank];
    }

    public void loadLock(boolean locked, Fluid[] fluids) {
        this.locked = locked;
        for (int i = 0; i < tanks; i++) {
            lockedFluids[i] = locked && i < fluids.length ? fluids[i] : null;
        }
    }

    public void clearAll() {
        for (int i = 0; i < tanks; i++) {
            stacks.set(i, FluidStack.EMPTY);
        }
        changed.call();
    }

    private void rememberLockedFluid(int tank, Fluid fluid) {
        if (locked && lockedFluids[tank] == null) {
            lockedFluids[tank] = fluid;
        }
    }

    @Override
    public int getTanks() {
        return tanks;
    }

    private record Snapshot(FluidStack[] stacks, Fluid[] lockedFluids) {
    }

    @Override
    public Object createSnapshot() {
        var copies = new FluidStack[stacks.size()];
        for (int i = 0; i < copies.length; i++) {
            copies[i] = stacks.get(i).copy();
        }
        return new Snapshot(copies, lockedFluids.clone());
    }

    @Override
    public void restoreSnapshot(Object snapshot) {
        var saved = (Snapshot) snapshot;
        for (int i = 0; i < stacks.size() && i < saved.stacks().length; i++) {
            stacks.set(i, saved.stacks()[i].copy());
        }
        System.arraycopy(saved.lockedFluids(), 0, lockedFluids, 0, Math.min(lockedFluids.length, saved.lockedFluids().length));
        changed.call();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int i) {
        return stacks.get(i);
    }

    public void setFluidInTank(int i, FluidStack fluidStack) {
        stacks.set(i, fluidStack);
        if (!fluidStack.isEmpty()) {
            rememberLockedFluid(i, fluidStack.getFluid());
        }
        changed.call();
    }

    @Override
    public int getTankCapacity(int i) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int i, @NotNull FluidStack fluidStack) {
        if (!fluidStack.isEmpty() && !filter.test(fluidStack.getFluid())) {
            return false;
        }
        Fluid lockedFluid = lockedFluids[i];
        if (lockedFluid != null && lockedFluid != fluidStack.getFluid()) {
            return false;
        }
        FluidStack slotStack = stacks.get(i);
        return slotStack.isEmpty() || FluidStack.isSameFluidSameComponents(slotStack, fluidStack);
    }

    @Override
    public int fill(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) {
            return 0;
        }

        int filled = 0;
        if (oneFluidPerTank) {
            int slot = tankFor(stack);
            if (slot >= 0) {
                filled = innerFill(slot, stack.getFluid(), stack.getAmount(), action.simulate());
            }
        } else {
            for (int slot = 0; slot < stacks.size() && filled < stack.getAmount(); slot++) {
                filled += innerFill(slot, stack.getFluid(), stack.getAmount() - filled, action.simulate());
            }
        }
        if (action.execute() && filled > 0) {
            changed.call();
        }
        return filled;
    }

    private int tankFor(FluidStack stack) {
        for (int slot = 0; slot < stacks.size(); slot++) {
            if (!stacks.get(slot).isEmpty() && FluidStack.isSameFluidSameComponents(stacks.get(slot), stack)) {
                return slot;
            }
        }
        for (int slot = 0; slot < stacks.size(); slot++) {
            if (stacks.get(slot).isEmpty() && isFluidValid(slot, stack)) {
                return slot;
            }
        }
        return -1;
    }

    private int innerFill(int slot, Fluid fluid, int amount, boolean simulate) {
        FluidStack slotStack = stacks.get(slot);
        int storedAmount = slotStack.getAmount();
        if (!isFluidValid(slot, new FluidStack(fluid, amount))) {
            return 0;
        }

        var canBeFilled = Math.min(capacity - storedAmount, amount);

        if (!simulate && canBeFilled > 0) {
            FluidStack stack = stacks.get(slot);
            if (stack.isEmpty()) {
                stacks.set(slot, new FluidStack(fluid, canBeFilled));
            } else {
                stack.setAmount(storedAmount + canBeFilled);
            }
            rememberLockedFluid(slot, fluid);
        }
        return canBeFilled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }

        int drained = 0;
        for (int slot = 0; slot < stacks.size() && drained < stack.getAmount(); slot++) {
            drained += innerDrain(slot, stack.getFluid(), stack.getAmount() - drained, action.simulate()).getAmount();
        }
        if (action.execute() && drained > 0) {
            changed.call();
        }
        return drained == 0 ? FluidStack.EMPTY : new FluidStack(stack.getFluid(), drained);
    }

    public FluidStack innerDrain(int slot, Fluid fluid, int amount, boolean simulate) {
        FluidStack slotStack = stacks.get(slot);
        int storedAmount = slotStack.getAmount();
        if (!isFluidValid(slot, new FluidStack(fluid, amount))) {
            return FluidStack.EMPTY;
        }

        var canBeDrained = Math.min(storedAmount, amount);
        if (!simulate) {
            FluidStack stack = stacks.get(slot);
            if (!stack.isEmpty()) {
                stack.setAmount(storedAmount - canBeDrained);
            }
        }
        return new FluidStack(fluid, canBeDrained);
    }

    @Override
    public @NotNull FluidStack drain(int i, FluidAction action) {
        Fluid fluid = findFirstFluid();
        if (fluid == null) {
            return FluidStack.EMPTY;
        }

        int drained = 0;
        for (int slot = 0; slot < stacks.size() && drained < i; slot++) {
            drained += innerDrain(slot, fluid, i - drained, action.simulate()).getAmount();
        }
        if (action.execute() && drained > 0) {
            changed.call();
        }
        return drained == 0 ? FluidStack.EMPTY : new FluidStack(fluid, drained);
    }

    private Fluid findFirstFluid() {
        for (int slot = 0; slot < stacks.size(); slot++) {
            FluidStack stack = stacks.get(slot);
            if (!stack.isEmpty()) {
                return stack.getFluid();
            }
        }
        return null;
    }

    public Tag serializeNBT() {
        var dataResult = NbtOps.INSTANCE.withEncoder(STACKS_CODEC).apply(stacks);
        var result = dataResult.getOrThrow(__msg -> { Ref.LOG.error(__msg); return new IllegalStateException(__msg); });
        return result;
    }

    public void deserializeNBT(Tag nbt) {
        var dataResult = NbtOps.INSTANCE.withDecoder(STACKS_CODEC).apply(nbt);
        var result = dataResult.getOrThrow(__msg -> { Ref.LOG.error(__msg); return new IllegalStateException(__msg); });
        var saved = result.getFirst();
        for (int i = 0; i < tanks; i++) {
            stacks.set(i, i < saved.size() ? saved.get(i) : FluidStack.EMPTY);
        }
    }
}
