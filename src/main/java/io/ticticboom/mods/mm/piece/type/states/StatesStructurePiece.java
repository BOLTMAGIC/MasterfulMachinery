package io.ticticboom.mods.mm.piece.type.states;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.piece.type.StructurePiece;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.attachment.states.NamedStateList;
import io.ticticboom.mods.mm.structure.attachment.states.StateListPieceFormedResult;
import io.ticticboom.mods.mm.structure.attachment.states.StateListsStructureAttachment;
import io.ticticboom.mods.mm.util.Lazy;
import io.ticticboom.mods.mm.util.StructurePieceUtils;
import lombok.SneakyThrows;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class StatesStructurePiece extends StructurePiece {
    private final String listName;
    private final String translationKey;
    private StateListPieceFormedResult activeState = StateListPieceFormedResult.NOT_FORMED;
    private List<Block> blockList;

    public StatesStructurePiece(String listName, String translationKey) {
        this.listName = listName;
        this.translationKey = translationKey;
    }

    @SneakyThrows
    private StateListsStructureAttachment getAttachment(StructurePieceSetupMetadata meta) {
        if (!meta.model().getAttachments().has(Ref.StructureAttachments.STATE_LISTS)) {
            throw new RuntimeException(StructurePieceUtils.errorMessageFor("states piece uses, but no stateList found in structure json", meta));
        }
        var attachment = meta.model().getAttachments().get(Ref.StructureAttachments.STATE_LISTS, StateListsStructureAttachment.class);
        if (attachment == null) {
            throw new RuntimeException(StructurePieceUtils.errorMessageFor("states piece uses, but no stateList found in structure json", meta));
        }

        return attachment;
    }

    private NamedStateList getNamedStateList(StructureModel model) {
        var attachment = model.getAttachments().get(Ref.StructureAttachments.STATE_LISTS, StateListsStructureAttachment.class);
        return attachment.getStateList(this.listName);
    }

    @Override
    public void validateSetup(StructurePieceSetupMetadata meta) {
        var attachment = getAttachment(meta);
        if (!attachment.hasStateList(this.listName)) {
            throw new RuntimeException(StructurePieceUtils.errorMessageFor(String.format("stateList with name [%s] was not found for structure", this.listName), meta));
        }

        var stateList = attachment.getStateList(this.listName);
        for (Map.Entry<String, StructurePiece> entry : stateList.pieces().entrySet()) {
            entry.getValue().validateSetup(meta);
        }

        this.blockList = stateList.pieces().values().stream().map(StructurePiece::createBlocksSupplier).flatMap(a -> a.get().stream()).toList();
    }

    @Override
    public boolean formed(Level level, BlockPos pos, StructureModel model) {
        var stateList = getNamedStateList(model);
        var result = stateList.formed(level, pos, model);

        this.activeState = result;
        return result.formed();
    }

    @Override
    public Supplier<List<Block>> createBlocksSupplier() {
        return () -> this.blockList;
    }

    @Override
    public Component createDisplayComponent() {
        return Component.translatable(this.translationKey).withStyle(ChatFormatting.DARK_AQUA);
    }

    @Override
    public JsonObject debugExpected(Level level, BlockPos pos, StructureModel model, JsonObject json) {
        // TODO: add info breaking down each option with pieces
        json.addProperty("stateList", this.listName);
        json.addProperty("nameTranslationKey", this.translationKey);
        return json;
    }

    @Override
    public JsonObject debugFound(Level level, BlockPos pos, StructureModel model, JsonObject json) {
        // TODO: add info showing which state was found if any
        return json;
    }
}
