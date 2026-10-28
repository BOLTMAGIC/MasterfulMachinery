package io.ticticboom.mods.mm.compat.ae2;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class Ae2KeyBridges {

    public static final Set<String> TYPES = Set.of("energy", "chemical", "source", "air", "matter");

    private static List<Ae2KeyBridge> loaded;

    private Ae2KeyBridges() {
    }

    public static List<Ae2KeyBridge> loaded() {
        if (loaded == null) {
            var found = new ArrayList<Ae2KeyBridge>();
            if (present("appflux")) {
                found.add(new EnergyBridge());
            }
            // 26.1: Applied Mekanistics, Ars Energistique, Applied Pneumatics and the Replication AE2 bridge have no
            // 26.1.2 release yet; their bridges (ChemicalBridge, SourceBridge, AirBridge, MatterBridge) are left out of
            // the build. Put their branches back when they ship:
            // if (present("appmek", "mekanism")) found.add(new ChemicalBridge());
            // if (present("arseng", "ars_nouveau")) found.add(new SourceBridge());
            // if (present("appliedpneumatics", "pneumaticcraft")) found.add(new AirBridge());
            // if (present("rep_ae2_bridge", "replication")) found.add(new MatterBridge());
            loaded = List.copyOf(found);
        }
        return loaded;
    }

    public static List<Ae2KeyBridge> enabled(Collection<String> excluded) {
        if (excluded.isEmpty()) {
            return loaded();
        }
        return loaded().stream().filter(bridge -> !excluded.contains(bridge.type())).toList();
    }

    public static void warnUnknown(Collection<String> excluded) {
        for (String type : excluded) {
            if (!TYPES.contains(type)) {
                Ref.LOG.warn("An ME connector excludes '{}', which isn't a type it knows. Use any of: {}", type, TYPES);
            }
        }
    }

    private static boolean present(String... mods) {
        var list = ModList.get();
        for (String mod : mods) {
            if (!list.isLoaded(mod)) {
                return false;
            }
        }
        return true;
    }
}
