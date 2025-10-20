package io.ticticboom.mods.mm.piece;

import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.resources.ResourceLocation;

public record StructurePieceSetupMetadata(
        ResourceLocation structureId,
        StructureModel model
) {
}
