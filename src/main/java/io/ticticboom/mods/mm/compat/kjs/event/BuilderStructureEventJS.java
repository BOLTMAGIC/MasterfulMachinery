package io.ticticboom.mods.mm.compat.kjs.event;

import dev.latvian.mods.kubejs.event.EventJS;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MMEvents.builderStructures: change the multiblock tool's list of other mods' structures.
 * <pre>
 * MMEvents.builderStructures(event => {
 *   event.remove('mbtool:woot/woot_tier_1_copper')   // one structure
 *   event.removeNamespace('mbtool')                    // every structure of a namespace
 *   event.add('pack:my_reactor', 'pack:structures/my_reactor.nbt')  // id, structure .nbt resource
 * })
 * </pre>
 */
public class BuilderStructureEventJS extends EventJS {
    @Getter
    private final List<String> removed = new ArrayList<>();
    @Getter
    private final List<String> removedNamespaces = new ArrayList<>();
    // id -> resource location of the .nbt file
    @Getter
    private final Map<String, String> added = new LinkedHashMap<>();

    public void remove(String id) {
        removed.add(id);
    }

    public void removeNamespace(String namespace) {
        removedNamespaces.add(namespace);
    }

    public void add(String id, String file) {
        added.put(id, file);
    }
}
