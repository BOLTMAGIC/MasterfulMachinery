package io.ticticboom.mods.mm.structure.attachment.states;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.structure.attachment.StructureAttachment;
import io.ticticboom.mods.mm.structure.attachment.MMStructureAttachmentType;

public class StateListsStructureAttachmentType extends MMStructureAttachmentType {
    @Override
    public boolean identify(JsonObject json) {
        return json.has("stateLists");
    }

    @Override
    public StructureAttachment parse(JsonObject json) {
        var stateListsObject = json.getAsJsonObject("stateLists");
        var lists = StateLists.parse(stateListsObject);
        return new StateListsStructureAttachment(lists);
    }
}
