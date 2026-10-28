package io.ticticboom.mods.mm.port.ae2.pattern.compat;

import io.ticticboom.mods.mm.compat.ae2.Ae2KeyBridges;
import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.ae2.pattern.Ae2PatternPortStorageModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Ae2PatternConfigBuilderJS extends PortConfigBuilderJS {

    private int patternPriority = 0;
    private final Set<String> exclude = new HashSet<>();

    public Ae2PatternConfigBuilderJS patternPriority(int patternPriority) {
        this.patternPriority = patternPriority;
        return this;
    }

    public Ae2PatternConfigBuilderJS exclude(String... types) {
        exclude.addAll(List.of(types));
        return this;
    }

    @Override
    public IPortStorageModel build() {
        Ae2KeyBridges.warnUnknown(exclude);
        return new Ae2PatternPortStorageModel(patternPriority, Set.copyOf(exclude));
    }
}
