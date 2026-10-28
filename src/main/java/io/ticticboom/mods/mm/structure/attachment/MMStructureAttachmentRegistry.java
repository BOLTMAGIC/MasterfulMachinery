package io.ticticboom.mods.mm.structure.attachment;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.structure.attachment.states.StateListsStructureAttachmentType;
import io.ticticboom.mods.mm.structure.attachment.visuals.VisualsStructureAttachmentType;

import java.util.ArrayList;
import java.util.List;

public class MMStructureAttachmentRegistry {
    public static final List<MMStructureAttachmentType> TYPES = new ArrayList<>();

    public static void register(MMStructureAttachmentType type) {
        TYPES.add(type);
    }

    public static void init() {
        register(new StateListsStructureAttachmentType());
        register(new VisualsStructureAttachmentType());
    }

    public static List<StructureAttachment> parseStructureAttachments(JsonObject json) {
        var result = new ArrayList<StructureAttachment>();
        for (final var type : TYPES) {
            if (type.identify(json)) {
                var attachment = type.parse(json);
                result.add(attachment);
            }
        }
        return result;
    }
}
