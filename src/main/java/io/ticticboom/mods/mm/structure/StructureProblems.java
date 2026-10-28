package io.ticticboom.mods.mm.structure;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StructureProblems {

    private static final Map<Identifier, List<Identifier>> MISSING_PORTS = new LinkedHashMap<>();

    public static synchronized void clear() {
        MISSING_PORTS.clear();
    }

    public static synchronized void missingPort(Identifier structureId, Identifier portId) {
        var ports = MISSING_PORTS.computeIfAbsent(structureId, x -> new ArrayList<>());
        if (!ports.contains(portId)) {
            ports.add(portId);
        }
    }

    public static synchronized List<Identifier> missingPorts(Identifier structureId) {
        return List.copyOf(MISSING_PORTS.getOrDefault(structureId, List.of()));
    }

    public static synchronized boolean isEmpty() {
        return MISSING_PORTS.isEmpty();
    }

    public static synchronized int structureCount() {
        return MISSING_PORTS.size();
    }

    public static synchronized String summary() {
        var parts = new ArrayList<String>();
        for (var entry : MISSING_PORTS.entrySet()) {
            parts.add(entry.getKey() + " needs " + entry.getValue());
        }
        return String.join("; ", parts);
    }
}
