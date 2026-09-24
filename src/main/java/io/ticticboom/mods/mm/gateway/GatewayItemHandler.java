package io.ticticboom.mods.mm.gateway;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * Looks like one always-empty slot; whatever is inserted goes on to the machine's item input ports,
 * filling them in order. Nothing can be extracted.
 */
public class GatewayItemHandler implements IItemHandler {
    private final Supplier<List<IPortStorage>> inputs;

    public GatewayItemHandler(Supplier<List<IPortStorage>> inputs) {
        this.inputs = inputs;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
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
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }
}
