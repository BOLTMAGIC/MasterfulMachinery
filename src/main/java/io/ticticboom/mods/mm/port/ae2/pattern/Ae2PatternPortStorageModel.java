package io.ticticboom.mods.mm.port.ae2.pattern;

import io.ticticboom.mods.mm.port.IPortStorageModel;

import java.util.Set;

public record Ae2PatternPortStorageModel(
        int patternPriority,
        Set<String> exclude
) implements IPortStorageModel {
}
