package io.ticticboom.mods.mm.compat.kjs.builder;

import io.ticticboom.mods.mm.extra.ExtraBlockModel;
import net.minecraft.network.chat.Component;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import lombok.Getter;
import net.minecraft.resources.Identifier;

@Getter
public class ExtraBlockBuilderJS {
    private final String id;
    private Identifier type;
    private String name;
    private String connectGroup;

    public ExtraBlockBuilderJS(String id) {
        this.id = id;
    }

    public ExtraBlockBuilderJS name(String name) {
        this.name = name;
        return this;
    }

    public ExtraBlockBuilderJS type(Identifier type) {
        this.type = type;
        return this;
    }

    public ExtraBlockBuilderJS connectGroup(String connectGroup) {
        this.connectGroup = connectGroup;
        return this;
    }

    public ExtraBlockModel build() {
        return new ExtraBlockModel(id, name, Component.literal(name), type, connectGroup);
    }
}
