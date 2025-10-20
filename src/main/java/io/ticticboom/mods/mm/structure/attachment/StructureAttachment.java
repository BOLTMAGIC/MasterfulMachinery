package io.ticticboom.mods.mm.structure.attachment;

import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import net.minecraft.resources.ResourceLocation;

public abstract class StructureAttachment {

    public abstract ResourceLocation getId();
    public abstract void validate(StructurePieceSetupMetadata meta);
}
