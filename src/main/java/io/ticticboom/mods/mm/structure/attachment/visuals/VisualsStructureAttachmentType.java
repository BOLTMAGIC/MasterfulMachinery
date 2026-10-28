package io.ticticboom.mods.mm.structure.attachment.visuals;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.structure.attachment.MMStructureAttachmentType;
import io.ticticboom.mods.mm.structure.attachment.StructureAttachment;

import java.util.ArrayList;

public class VisualsStructureAttachmentType extends MMStructureAttachmentType {
    @Override
    public boolean identify(JsonObject json) {
        return json.has("visuals");
    }

    @Override
    public StructureAttachment parse(JsonObject json) {
        var fluidVolumes = new ArrayList<FluidVolumeVisual>();
        for (var element : json.getAsJsonArray("visuals")) {
            var visual = element.getAsJsonObject();
            String type = visual.get("type").getAsString();
            if (type.equals("fluid_volume")) {
                fluidVolumes.add(FluidVolumeVisual.parse(visual));
            } else {
                throw new IllegalArgumentException("Unknown structure visual type: " + type);
            }
        }
        return new VisualsStructureAttachment(fluidVolumes);
    }
}
