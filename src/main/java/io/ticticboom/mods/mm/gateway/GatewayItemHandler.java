package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.transfer.TransferSnapshot;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GatewayItemHandler implements IItemHandler, TransferSnapshot {
    private final Supplier<List<IPortStorage>> inputs;
    private final Supplier<List<IPortStorage>> outputs;

    public GatewayItemHandler(Supplier<List<IPortStorage>> inputs) {
        this(inputs, List::of);
    }

    public GatewayItemHandler(Supplier<List<IPortStorage>> inputs, Supplier<List<IPortStorage>> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    private static List<IItemHandler> handlers(List<IPortStorage> storages) {
        var result = new ArrayList<IItemHandler>();
        for (IPortStorage storage : storages) {
            var handler = storage.getCapability(MMCapabilities.ITEM);
            if (handler != null) {
                result.add(handler);
            }
        }
        return result;
    }

    private List<IItemHandler> all() {
        var result = handlers(inputs.get());
        result.addAll(handlers(outputs.get()));
        return result;
    }

    @Override
    public Object createSnapshot() {
        return GatewaySnapshots.create(all());
    }

    @Override
    public void restoreSnapshot(Object snapshot) {
        GatewaySnapshots.restore(snapshot);
    }

    @Override
    public int getSlots() {
        int slots = 1;
        for (IItemHandler handler : all()) {
            slots += handler.getSlots();
        }
        return slots;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        for (IItemHandler handler : all()) {
            if (slot < handler.getSlots()) {
                return handler.getStackInSlot(slot);
            }
            slot -= handler.getSlots();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (IItemHandler handler : handlers(inputs.get())) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = handler instanceof ItemPortHandler port
                    ? port.insertStackFast(remaining, simulate)
                    : ItemHandlerHelper.insertItemStacked(handler, remaining, simulate);
        }
        return remaining;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        for (IItemHandler handler : handlers(inputs.get())) {
            slot -= handler.getSlots();
        }
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        for (IItemHandler handler : handlers(outputs.get())) {
            if (slot < handler.getSlots()) {
                return handler.extractItem(slot, amount, simulate);
            }
            slot -= handler.getSlots();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        for (IItemHandler handler : all()) {
            if (slot < handler.getSlots()) {
                return handler.getSlotLimit(slot);
            }
            slot -= handler.getSlots();
        }
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }
}
