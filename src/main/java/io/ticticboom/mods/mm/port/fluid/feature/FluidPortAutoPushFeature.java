package io.ticticboom.mods.mm.port.fluid.feature;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortAutoPushFeature;
import io.ticticboom.mods.mm.port.item.feature.ItemHandlerCoupling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

public class FluidPortAutoPushFeature extends AbstractPortAutoPushFeature<FluidHandlerCoupling> {

    public FluidPortAutoPushFeature(BlockEntity portBlockEntity, PortModel model) {
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

        LazyOptional<IFluidHandler> neighborCap = neighborBe.getCapability(MMCapabilities.FLUID, neighborFace);
        if (!neighborCap.isPresent()) {
            return;
        }

        if (autoPushNeighbors.containsKey(neighborPos)) {
            FluidHandlerCoupling pairing = autoPushNeighbors.get(neighborPos);
            pairing.setToHandler(neighborCap);
        } else {
            LazyOptional<IFluidHandler> capability = this.portBlockEntity.getCapability(MMCapabilities.FLUID);
            autoPushNeighbors.put(neighborPos, new FluidHandlerCoupling(capability, neighborCap));
        }
    }

    private boolean canAddAsNeighbor(BlockPos pos, BlockEntity be) {
        if (be instanceof IPortBlockEntity pbe) {
            return pbe.isInput();
        }
        return true;
    }
}
