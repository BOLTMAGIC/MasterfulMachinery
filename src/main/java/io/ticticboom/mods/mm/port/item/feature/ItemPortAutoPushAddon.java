package io.ticticboom.mods.mm.port.item.feature;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.item.register.ItemPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.HashMap;

public class ItemPortAutoPushAddon {

    private final HashMap<BlockPos, ItemHandlerPairing> autoPushNeighbors = new HashMap<>();
    private final ItemPortBlockEntity portBlockEntity;

    public ItemPortAutoPushAddon(ItemPortBlockEntity portBlockEntity, PortModel model) {
        this.portBlockEntity = portBlockEntity;
    }

    public void tick() {
        var level = portBlockEntity.getLevel();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            return;
        }

        for (var cap : autoPushNeighbors.values()) {
            cap.attemptTransfer();
        }
    }

    public void tryAddNeighboringHandlers() {
        if (!portBlockEntity.hasLevel()) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockPos otherPos = portBlockEntity.getBlockPos().relative(direction, 1);
            tryAddNeighborHandler(otherPos);
        }
    }

    public void onLoad() {
        tryAddNeighboringHandlers();
    }

    private void tryAddNeighborHandler(BlockPos neighborPos) {
        BlockEntity neighborBe = portBlockEntity.getLevel().getBlockEntity(neighborPos);
        if (neighborBe == null) {
            return;
        }

        var valid = canAddAsNeighbor(neighborPos, neighborBe);
        if (!valid) {
            return;
        }

        LazyOptional<IItemHandler> neighborCap = neighborBe.getCapability(MMCapabilities.ITEM);
        if (!neighborCap.isPresent()) {
            return;
        }

        if (autoPushNeighbors.containsKey(neighborPos)) {
            ItemHandlerPairing pairing = autoPushNeighbors.get(neighborPos);
            pairing.setToHandler(neighborCap);
        } else {
            LazyOptional<IItemHandler> capability = this.portBlockEntity.getCapability(MMCapabilities.ITEM);
            autoPushNeighbors.put(neighborPos, new ItemHandlerPairing(capability, neighborCap));
        }
    }

    private boolean canAddAsNeighbor(BlockPos pos, BlockEntity be) {
        if (be instanceof IPortBlockEntity pbe) {
            return pbe.isInput();
        }
        return true;
    }
}
