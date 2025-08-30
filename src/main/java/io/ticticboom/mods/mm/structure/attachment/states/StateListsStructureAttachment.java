package io.ticticboom.mods.mm.structure.attachment.states;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.structure.attachment.MMStructureAttachment;
import net.minecraft.resources.ResourceLocation;

public record StateListsStructureAttachment(
        StateLists stateLists
) implements MMStructureAttachment {
    public NamedStateList getStateList(String name) {
        return stateLists.get(name);
    }

    @Override
    public ResourceLocation getId() {
        return Ref.StructureAttachments.STATE_LISTS;
    }
}

