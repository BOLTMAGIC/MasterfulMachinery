package io.ticticboom.mods.mm.structure.attachment;

import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import net.minecraft.resources.Identifier;

public abstract class StructureAttachment {

    public abstract Identifier getId();
    public abstract void validate(StructurePieceSetupMetadata meta);
}
