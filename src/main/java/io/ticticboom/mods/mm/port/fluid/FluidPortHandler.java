package io.ticticboom.mods.mm.port.fluid;

import com.mojang.serialization.Codec;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import net.minecraft.nbt.*;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FluidPortHandler implements IFluidHandler {

    private final int tanks;
    private final int capacity;
    @Getter
    private final INotifyChangeFunction changed;

    private final ArrayList<FluidStack> stacks;

    // fluid remembered while the port is locked, null = not locked to a fluid yet
    @Nullable
    private Fluid lockedFluid;
    @Getter
    private boolean locked = false;

    public static final Codec<List<FluidStack>> STACKS_CODEC = Codec.list(FluidStack.CODEC);

    public FluidPortHandler(int tanks, int capacity, INotifyChangeFunction changed) {
        this.tanks = tanks;
        this.capacity = capacity;
        this.changed = changed;
        stacks = new ArrayList<>();
        for (int i = 0; i < tanks; i++) {
            stacks.add(FluidStack.EMPTY);
        }
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        lockedFluid = locked ? storedFluid() : null;
        changed.call();
    }

    @Nullable
    public Fluid getLockedFluid() {
        return lockedFluid;
    }

    /**
     * Restores lock state from NBT without re-deriving it from the current contents.
     */
    public void loadLock(boolean locked, @Nullable Fluid fluid) {
        this.locked = locked;
        lockedFluid = locked ? fluid : null;
    }

    /**
     * @return the fluid this port currently holds; a port holds a single fluid type at a time
     */
    @Nullable
    public Fluid storedFluid() {
        for (FluidStack stack : stacks) {
            if (!stack.isEmpty()) {
                return stack.getFluid();
            }
        }
        return null;
    }

    public int getTotalAmount() {
        int total = 0;
        for (FluidStack stack : stacks) {
            total += stack.getAmount();
        }
        return total;
    }

    public int getTotalCapacity() {
        return tanks * capacity;
    }

    public void clearAll() {
        for (int i = 0; i < tanks; i++) {
            stacks.set(i, FluidStack.EMPTY);
        }
        changed.call();
    }

    private void rememberLockedFluid(Fluid fluid) {
        if (locked && lockedFluid == null) {
            lockedFluid = fluid;
        }
    }

    @Override
    public int getTanks() {
        return tanks;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int i) {
        return stacks.get(i);
    }

    public void setFluidInTank(int i, FluidStack fluidStack) {
        stacks.set(i, fluidStack);
        if (!fluidStack.isEmpty()) {
            rememberLockedFluid(fluidStack.getFluid());
        }
        changed.call();
    }

    @Override
    public int getTankCapacity(int i) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int i, @NotNull FluidStack fluidStack) {
        Fluid fluid = fluidStack.getFluid();
        if (lockedFluid != null && lockedFluid != fluid) {
            return false;
        }
        // one fluid type per port: a different fluid goes to another port
        Fluid stored = storedFluid();
        if (stored != null && stored != fluid) {
            return false;
        }
        FluidStack slotStack = stacks.get(i);
        return slotStack.isEmpty() || slotStack.isFluidEqual(fluidStack);
    }

    @Override
    public int fill(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) {
            return 0;
        }

        int filled = 0;
        for (int slot = 0; slot < stacks.size(); slot++) {
            filled += innerFill(slot, stack.getFluid(), stack.getAmount() - filled, action.simulate());
        }
        // only a real change needs saving and a client update; simulations are frequent (pipes)
        if (action.execute() && filled > 0) {
            changed.call();
        }
        return filled;
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
            rememberLockedFluid(fluid);
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
            // ask each tank only for what is still missing, never more than requested in total
            drained += innerDrain(slot, stack.getFluid(), stack.getAmount() - drained, action.simulate()).getAmount();
        }
        if (action.execute() && drained > 0) {
            changed.call();
        }
        return new FluidStack(stack.getFluid(), drained);
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
        return new FluidStack(fluid, drained);
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
        var result = dataResult.getOrThrow(false, Ref.LOG::error);
        return result;
    }

    public void deserializeNBT(Tag nbt) {
        var dataResult = NbtOps.INSTANCE.withDecoder(STACKS_CODEC).apply(nbt);
        var result = dataResult.getOrThrow(false, Ref.LOG::error);
        stacks.clear();
        stacks.addAll(result.getFirst());
    }
}
