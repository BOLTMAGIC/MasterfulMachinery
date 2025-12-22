package io.ticticboom.mods.mm.port.item.feature;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortAutoPushFeature;
import io.ticticboom.mods.mm.port.item.ItemPortRouting;
import io.ticticboom.mods.mm.port.item.register.ItemPortBlockEntity;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ItemPortAutoPushAddon extends AbstractPortAutoPushFeature<ItemHandlerCoupling> {

    public ItemPortAutoPushAddon(ItemPortBlockEntity portBlockEntity, PortModel model) {
        super(portBlockEntity, model);
    }

    @Override
    protected void tryAddNeighborHandler(BlockPos neighborPos, Direction neighborFace) {
        if (!portBlockEntity.hasLevel()) return;
        var level = portBlockEntity.getLevel();
        if (level == null) return;
        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
        if (neighborBe == null) {
            return;
        }

        var valid = canAddAsNeighbor(neighborPos, neighborBe);
        if (!valid) {
            return;
        }

        LazyOptional<IItemHandler> neighborCap = neighborBe.getCapability(MMCapabilities.ITEM, neighborFace);
        if (!neighborCap.isPresent()) {
            return;
        }

        if (autoPushNeighbors.containsKey(neighborPos)) {
            ItemHandlerCoupling pairing = autoPushNeighbors.get(neighborPos);
            pairing.setToHandler(neighborCap);
        } else {
            LazyOptional<IItemHandler> capability = this.portBlockEntity.getCapability(MMCapabilities.ITEM);
            autoPushNeighbors.put(neighborPos, new ItemHandlerCoupling(capability, neighborCap));
        }
    }

    private boolean canAddAsNeighbor(BlockPos pos, BlockEntity be) {
        Objects.requireNonNull(pos);
        if (be instanceof IPortBlockEntity pbe) {
            return pbe.isInput();
        }
        return true;
    }

    @Override
    public void tick() {
        var level = portBlockEntity.getLevel();
        if (level == null) return;
        if (level.isClientSide) return;

        // Build candidate map of neighboring port storages that are ItemPortStorage
        Map<BlockPos, io.ticticboom.mods.mm.port.item.ItemPortStorage> candidates = new HashMap<>();
        for (BlockPos pos : autoPushNeighbors.keySet()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IPortBlockEntity pbe) {
                var storage = pbe.getStorage();
                if (storage instanceof io.ticticboom.mods.mm.port.item.ItemPortStorage ips) {
                    candidates.put(pos, ips);
                }
            }
        }

        // Source storage: ensure portBlockEntity is an IPortBlockEntity
        if (!(portBlockEntity instanceof IPortBlockEntity sourcePbe)) return;
        var sourceStorage = sourcePbe.getStorage();
        if (!(sourceStorage instanceof io.ticticboom.mods.mm.port.item.ItemPortStorage source)) return;

        // For each slot in the source handler, attempt to push as much as possible
        for (int slot = 0; slot < source.getHandler().getSlots(); slot++) {
            var stack = source.getHandler().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            int amountAvailable = stack.getCount();
            ItemPortRouting.distributeFillThrough(stack.getItem(), amountAvailable, source, candidates);
        }
    }
}
