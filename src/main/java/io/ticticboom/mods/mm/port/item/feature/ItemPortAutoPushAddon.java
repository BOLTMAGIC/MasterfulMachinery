package io.ticticboom.mods.mm.port.item.feature;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortAutoPushFeature;
import io.ticticboom.mods.mm.port.item.register.ItemPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class ItemPortAutoPushAddon extends AbstractPortAutoPushFeature<ItemHandlerCoupling> {

    public ItemPortAutoPushAddon(ItemPortBlockEntity portBlockEntity, PortModel model) {
        super(portBlockEntity, model);
    }

    @Override
    protected void tryAddNeighborHandler(BlockPos neighborPos, Direction neighborFace) {
        BlockEntity neighborBe = portBlockEntity.getLevel().getBlockEntity(neighborPos);
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
        if (be instanceof IPortBlockEntity pbe) {
            return pbe.isInput();
        }
        return true;
    }
}
