package io.ticticboom.mods.mm.compat.kjs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import io.ticticboom.mods.mm.compat.kjs.builder.StructureBuilderJS;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class StructureEventJS implements KubeEvent {
    @Getter
    private final List<StructureBuilderJS> builders = new ArrayList<>();

    public StructureBuilderJS create(String id) {
        var res = new StructureBuilderJS(id);
        builders.add(res);
        return res;
    }
}
