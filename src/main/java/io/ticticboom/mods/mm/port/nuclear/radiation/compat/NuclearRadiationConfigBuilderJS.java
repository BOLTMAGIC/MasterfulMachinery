package io.ticticboom.mods.mm.port.nuclear.radiation.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.nuclear.radiation.NuclearRadiationPortStorageModel;

import java.util.ArrayList;
import java.util.List;

public class NuclearRadiationConfigBuilderJS extends PortConfigBuilderJS {

    private double capacity;
    private final List<String> isotopes = new ArrayList<>();
    private boolean decay = true;
    private boolean shielded = true;
    private final List<String> carriers = new ArrayList<>();
    private double loadPerItem = NuclearRadiationPortStorageModel.DEFAULT_LOAD_PER_ITEM;

    public NuclearRadiationConfigBuilderJS capacity(double capacity) {
        this.capacity = capacity;
        return this;
    }

    public NuclearRadiationConfigBuilderJS isotope(String isotope) {
        this.isotopes.add(isotope);
        return this;
    }

    public NuclearRadiationConfigBuilderJS decay(boolean decay) {
        this.decay = decay;
        return this;
    }

    public NuclearRadiationConfigBuilderJS shielded(boolean shielded) {
        this.shielded = shielded;
        return this;
    }

    public NuclearRadiationConfigBuilderJS carrier(String carrier) {
        this.carriers.add(carrier);
        return this;
    }

    public NuclearRadiationConfigBuilderJS loadPerItem(double loadPerItem) {
        this.loadPerItem = loadPerItem;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new NuclearRadiationPortStorageModel(capacity, List.copyOf(isotopes), decay, shielded,
                List.copyOf(carriers), loadPerItem);
    }
}
