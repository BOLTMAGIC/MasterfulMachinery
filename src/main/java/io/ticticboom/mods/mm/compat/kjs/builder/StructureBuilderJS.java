package io.ticticboom.mods.mm.compat.kjs.builder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import io.ticticboom.mods.mm.model.IdList;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.attachment.StructureAttachments;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class StructureBuilderJS {

    private final List<ResourceLocation> controllers = new ArrayList<>();
    private final ResourceLocation id;
    private String name;
    private Consumer<StructureLayoutBuilderJS> layoutConsumer;
    private int maxParallelRecipes = -1;
    private JsonElement geoModel;

    public StructureBuilderJS(String id) {
        this.id = ResourceLocation.tryParse(id);
    }

    public StructureBuilderJS name(String name) {
        this.name = name;
        return this;
    }

    public StructureBuilderJS controllerId(String id) {
        controllers.add(ResourceLocation.tryParse(id));
        return this;
    }

    public StructureBuilderJS layout(Consumer<StructureLayoutBuilderJS> consumer) {
        this.layoutConsumer = consumer;
        return this;
    }

    public StructureBuilderJS maxParallelRecipes(int maxParallelRecipes) {
        if (maxParallelRecipes < 0) this.maxParallelRecipes = -1;
        else this.maxParallelRecipes = Math.min(maxParallelRecipes, 100);
        return this;
    }

    /** GeckoLib model for this structure; replaces the controller's. See ControllerBuilderJS#geoModel. */
    public StructureBuilderJS geoModel(JsonObject geoModel) {
        this.geoModel = geoModel;
        return this;
    }

    /** 'none': no GeckoLib model while this structure is formed, even if the controller has one. */
    public StructureBuilderJS geoModel(String none) {
        this.geoModel = new JsonPrimitive(none);
        return this;
    }

    @HideFromJS
    public StructureModel build() {
        var event = new StructureLayoutBuilderJS(this);
        layoutConsumer.accept(event);
        IdList controllerIds = new IdList(controllers);
        StructureLayout layout = event.build();
        // TODO: create real structure attachment js builders
        var json = StructureModel.paramsToJson(id, name, controllerIds, layout, maxParallelRecipes);
        if (geoModel != null) json.add(GeoModelConfig.KEY, geoModel);
        return new StructureModel(id, name, controllerIds, layout, new StructureAttachments(List.of()), json);
    }
}
