package io.ticticboom.mods.mm.compat.kjs.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.model.IdList;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.attachment.StructureAttachments;
import io.ticticboom.mods.mm.structure.attachment.visuals.FluidVolumeVisual;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class StructureBuilderJS {

    private final List<Identifier> controllers = new ArrayList<>();
    private final Identifier id;
    private String name;
    private Consumer<StructureLayoutBuilderJS> layoutConsumer;
    private int maxParallelRecipes = -1;
    private final List<FluidVolumeVisual> fluidVolumes = new ArrayList<>();
    private JsonElement geoModel;

    public StructureBuilderJS(String id) {
        this.id = Identifier.tryParse(id);
    }

    public StructureBuilderJS name(String name) {
        this.name = name;
        return this;
    }

    public StructureBuilderJS controllerId(String id) {
        controllers.add(Identifier.tryParse(id));
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

    public StructureBuilderJS fluidVolume(List<Number> from, List<Number> to) {
        return fluidVolume(from, to, null);
    }

    public StructureBuilderJS fluidVolume(List<Number> from, List<Number> to, String port) {
        fluidVolumes.add(new FluidVolumeVisual(toPos(from), toPos(to), null, port));
        return this;
    }

    public StructureBuilderJS geoModel(JsonElement geoModel) {
        this.geoModel = geoModel;
        return this;
    }

    public StructureBuilderJS fluidArea(String area) {
        return fluidArea(area, null);
    }

    public StructureBuilderJS fluidArea(String area, String port) {
        fluidVolumes.add(new FluidVolumeVisual(null, null, area, port));
        return this;
    }

    private static BlockPos toPos(List<Number> values) {
        return new BlockPos(values.get(0).intValue(), values.get(1).intValue(), values.get(2).intValue());
    }

    @HideFromJS
    public StructureModel build() {
        var event = new StructureLayoutBuilderJS(this);
        layoutConsumer.accept(event);
        IdList controllerIds = new IdList(controllers);
        StructureLayout layout = event.build();
        var json = StructureModel.paramsToJson(id, name, controllerIds, layout, maxParallelRecipes);
        if (!fluidVolumes.isEmpty()) {
            var visuals = new JsonArray();
            fluidVolumes.forEach(volume -> visuals.add(volume.toJson()));
            json.add("visuals", visuals);
        }
        if (geoModel != null) {
            json.add(GeoModelConfig.KEY, geoModel);
        }
        return new StructureModel(id, name, controllerIds, layout, StructureAttachments.parse(json), json);
    }
}
