package io.ticticboom.mods.mm.port.energy.feature;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortAutoPushFeature;
import io.ticticboom.mods.mm.port.energy.register.EnergyPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyPortAutoPushFeature extends AbstractPortAutoPushFeature<EnergyHandlerCoupling> {

    public EnergyPortAutoPushFeature(EnergyPortBlockEntity portBlockEntity, PortModel model) {
        super(portBlockEntity, model);
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

        LazyOptional<IEnergyStorage> neighborCap = neighborBe.getCapability(MMCapabilities.ENERGY, neighborFace);
        if (!neighborCap.isPresent()) {
            return;
        }

        if (autoPushNeighbors.containsKey(neighborPos)) {
            EnergyHandlerCoupling pairing = autoPushNeighbors.get(neighborPos);
            pairing.setToHandler(neighborCap);
        } else {
            LazyOptional<IEnergyStorage> capability = this.portBlockEntity.getCapability(MMCapabilities.ENERGY);
            autoPushNeighbors.put(neighborPos, new EnergyHandlerCoupling(capability, neighborCap));
        }
    }

    private boolean canAddAsNeighbor(BlockPos pos, BlockEntity be) {
        if (be instanceof IPortBlockEntity pbe) {
            return pbe.isInput();
        }
        return true;
    }
}
