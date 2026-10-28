package io.ticticboom.mods.mm.port.mekanism.chemical.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;

public class MekanismChemicalConfigBuilderJS extends PortConfigBuilderJS {

    private long amount;
    private boolean isAutoPushSet = false;
    private boolean autoPush = false;

    public MekanismChemicalConfigBuilderJS amount(long amount) {
        this.amount = amount;
        return this;
    }

    public MekanismChemicalConfigBuilderJS capacity(long amount) {
        this.amount = amount;
        return this;
    }

    public MekanismChemicalConfigBuilderJS autoPush(boolean autoPush) {
        this.autoPush = autoPush;
        this.isAutoPushSet = true;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new MekanismChemicalPortStorageModel(amount,
                isAutoPushSet ? () -> autoPush : () -> MMConfig.DEFAULT_PORT_AUTO_PUSH);
    }
}
