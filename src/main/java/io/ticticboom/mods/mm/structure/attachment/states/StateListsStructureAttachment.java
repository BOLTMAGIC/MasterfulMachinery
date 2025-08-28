package io.ticticboom.mods.mm.structure.attachment.states;

import io.ticticboom.mods.mm.structure.attachment.MMStructureAttachment;

public record StateListsStructureAttachment(
        StateLists stateLists
) implements MMStructureAttachment {
    public NamedStateList getStateList(String name) {
        return stateLists.get(name);
    }
}

