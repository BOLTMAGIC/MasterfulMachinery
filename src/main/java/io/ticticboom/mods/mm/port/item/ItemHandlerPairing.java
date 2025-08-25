package io.ticticboom.mods.mm.port.item;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerPairing {

    @Getter
    private final LazyOptional<IItemHandler> fromHandler;

    @Getter
    @Setter
    private LazyOptional<IItemHandler> toHandler;

    public ItemHandlerPairing(LazyOptional<IItemHandler> fromHandler, LazyOptional<IItemHandler> toHandler) {
        this.fromHandler = fromHandler;
        this.toHandler = toHandler;
    }

    public void attemptTransfer() {
        toHandler.ifPresent((handler) -> {
            fromHandler.ifPresent((from) -> {
                attemptTransfer(from, handler);
            });
        });
    }

    private void attemptTransfer(IItemHandler from, IItemHandler to) {
        for (int fromSlot = 0; fromSlot < from.getSlots(); fromSlot++) {
            ItemStack extracted = from.extractItem(fromSlot, from.getSlotLimit(fromSlot), true);
            if (extracted.isEmpty()) {
                continue;
            }

            var remaining = attemptInsert(from, to, extracted, fromSlot);
            var shouldExtractCount = extracted.getCount() - remaining.getCount();
            if (shouldExtractCount > 0) {
                from.extractItem(fromSlot, shouldExtractCount, false);
            }
        }
    }

    private ItemStack attemptInsert(IItemHandler from, IItemHandler to, ItemStack toInsert, int fromSlot) {
        var remaining = toInsert;
        for (int toSlot = 0; toSlot < to.getSlots(); toSlot++) {
            remaining = to.insertItem(toSlot, toInsert, false);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }
}
