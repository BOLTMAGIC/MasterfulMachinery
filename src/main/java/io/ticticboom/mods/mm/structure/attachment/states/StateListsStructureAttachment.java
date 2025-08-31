package io.ticticboom.mods.mm.structure.attachment.states;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.structure.attachment.StructureAttachment;
import net.minecraft.resources.ResourceLocation;

//TODO: add rendering object add rendering extras to controller screen.
public class StateListsStructureAttachment extends StructureAttachment {
    private final StateLists lists;

    public StateListsStructureAttachment(
            StateLists stateLists
    ) {
        this.lists = stateLists;
    }

    public boolean hasStateList(String name) {
        return lists.has(name);
    }
    public NamedStateList getStateList(String name) {
        return lists.get(name);
    }

    @Override
    public ResourceLocation getId() {
        return Ref.StructureAttachments.STATE_LISTS;
    }

    @Override
    public void validate(StructurePieceSetupMetadata meta) {
        for (NamedStateList list : lists.states().values()) {
            list.pieces().values().forEach(x -> x.validateSetup(meta));
        }
    }
}

