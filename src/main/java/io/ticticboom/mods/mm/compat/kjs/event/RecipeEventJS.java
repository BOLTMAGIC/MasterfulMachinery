package io.ticticboom.mods.mm.compat.kjs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import io.ticticboom.mods.mm.compat.kjs.builder.RecipeBuilderJS;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class RecipeEventJS implements KubeEvent {
    @Getter
    private final List<RecipeBuilderJS> builders = new ArrayList<>();

    public RecipeBuilderJS create(String id) {
        var res = new RecipeBuilderJS(id);
        builders.add(res);
        return res;
    }
}
