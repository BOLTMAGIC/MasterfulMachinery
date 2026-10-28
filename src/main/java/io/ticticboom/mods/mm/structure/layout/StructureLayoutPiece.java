package io.ticticboom.mods.mm.structure.layout;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.structure.GuiStructureLayoutPiece;
import io.ticticboom.mods.mm.piece.MMStructurePieceRegistry;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.piece.modifier.StructurePieceModifier;
import io.ticticboom.mods.mm.piece.type.StructurePiece;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

/**
 * @param piece Expose underlying StructurePiece for special handling (e.g. anywhere-port matching)
 */
public record StructureLayoutPiece(StructurePiece piece, List<StructurePieceModifier> modifiers,
                                   GuiStructureLayoutPiece guiPiece, String valueId, JsonObject json) {

    public void validate(StructurePieceSetupMetadata meta) {
        piece.validateSetup(meta);
        List<Block> blocks = piece.createBlocksSupplier().get();
        if (blocks.isEmpty()) {
            Ref.LOG.error("MM ERROR: Render setup failed to validate for: {}", this.valueId);
        }
        for (StructurePieceModifier modifier : modifiers) {
            modifier.validateSetup(meta, blocks);
        }
    }

    public boolean formed(Level level, BlockPos pos, StructureModel model, Rotation rot) {
        var formed = piece.formed(level, pos, model);
        if (!formed) {
            return false;
        }
        for (StructurePieceModifier modifier : modifiers) {
            formed = modifier.formed(level, pos, model, rot);
            if (!formed) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    public static StructureLayoutPiece parse(JsonObject json, Identifier structureId, String keyChar) {
        var piece = MMStructurePieceRegistry.findPieceType(json);
        var modifiers = MMStructurePieceRegistry.findModifierTypes(json);
        assert piece != null;
        var guiPiece = new GuiStructureLayoutPiece(piece.createBlocksSupplier(), piece.createDisplayComponent(), modifiers);
        return new StructureLayoutPiece(piece, modifiers, guiPiece, keyChar, json);
    }

    public void setup(StructureModel model) {
        var meta = new StructurePieceSetupMetadata(model.id(), model);
        piece.validateSetup(meta);
        var blocks = piece.createBlocksSupplier().get();
        for (StructurePieceModifier modifier : modifiers) {
            modifier.validateSetup(meta, blocks);
        }
    }

    public JsonObject debugFormed(Level level, BlockPos pos, StructureModel model, Rotation rot) {
        var json = new JsonObject();
        json.addProperty("formed", formed(level, pos, model, rot));
        var expected = piece.debugExpected(level, pos, model, new JsonObject());
        var found = piece.debugFound(level, pos, model, new JsonObject());
        json.add("expected", expected);
        json.add("found", found);

        // TODO audit modifiers
        var modifiersJson = new JsonArray();
        for (StructurePieceModifier modifier : modifiers) {
            var expectedModifier = modifier.debugExpected(level, pos, model, rot, new JsonObject());
            var foundModifier = modifier.debugFound(level, pos, model, rot, new JsonObject());
            var modifierInnerJson = new JsonObject();
            modifierInnerJson.addProperty("id", modifier.getId());
            modifierInnerJson.add("expected", expectedModifier);
            modifierInnerJson.add("found", foundModifier);
            modifiersJson.add(modifierInnerJson);
        }
        json.add("modifiers", modifiersJson);
        return json;
    }
}
