package io.ticticboom.mods.mm.port.energy.feature;

import io.ticticboom.mods.mm.port.common.IHandlerCoupling;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyHandlerCoupling implements IHandlerCoupling {
    @Getter
    private final LazyOptional<IEnergyStorage> fromHandler;
    @Getter
    @Setter
    private LazyOptional<IEnergyStorage> toHandler;

    public EnergyHandlerCoupling(LazyOptional<IEnergyStorage> fromHandler, LazyOptional<IEnergyStorage> toHandler) {
        this.fromHandler = fromHandler;
        this.toHandler = toHandler;
    }


    @Override
    public void attemptTransfer() {
        toHandler.ifPresent((handler) -> {
            fromHandler.ifPresent((from) -> {
                attemptTransfer(from, handler);
            });
        });
    }

    private void attemptTransfer(IEnergyStorage from, IEnergyStorage to) {
        int extracted = from.extractEnergy(from.getMaxEnergyStored(), true);
        if (extracted > 0) {
            int inserted = to.receiveEnergy(extracted, false);
            if (inserted > 0) {
                from.extractEnergy(inserted, false);
            }
        }
    }
}
