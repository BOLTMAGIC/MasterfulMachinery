package io.ticticboom.mods.mm.piece;

import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.resources.Identifier;

public record StructurePieceSetupMetadata(
        Identifier structureId,
        StructureModel model
) {
}
