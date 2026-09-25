package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shows the slots of the machine's item input ports (read only), followed by one always-empty slot to insert into;
 * whatever is inserted, into any slot, goes on to the input ports, filling them in order. Nothing can be extracted.
 * Showing the ports' contents lets an AE2 Pattern Provider in blocking mode wait until the machine has used its inputs.
 */
public class GatewayItemHandler implements IItemHandler {
    private final Supplier<List<IPortStorage>> inputs;

    public GatewayItemHandler(Supplier<List<IPortStorage>> inputs) {
        this.inputs = inputs;
    }

    private List<IItemHandler> handlers() {
        var result = new ArrayList<IItemHandler>();
        for (IPortStorage storage : inputs.get()) {
            storage.getCapability(MMCapabilities.ITEM).resolve().ifPresent(result::add);
        }
        return result;
    }

    @Override
    public int getSlots() {
        int slots = 1;
        for (IItemHandler handler : handlers()) {
            slots += handler.getSlots();
        }
        return slots;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        for (IItemHandler handler : handlers()) {
            if (slot < handler.getSlots()) {
                return handler.getStackInSlot(slot);
            }
            slot -= handler.getSlots();
        }
        // the trailing entry slot
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (IPortStorage storage : inputs.get()) {
            if (remaining.isEmpty()) {
                break;
            }
            var handler = storage.getCapability(MMCapabilities.ITEM).resolve();
            if (handler.isPresent()) {
                remaining = ItemHandlerHelper.insertItemStacked(handler.get(), remaining, simulate);
            }
        }
        return remaining;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        for (IItemHandler handler : handlers()) {
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
