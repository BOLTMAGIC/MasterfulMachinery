package io.ticticboom.mods.mm.piece.type.anyof;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.piece.type.StructurePiece;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

public class AnyOfStructurePiece extends StructurePiece {

    private final List<StructurePiece> options;
    private List<Block> blocks = List.of();

    public AnyOfStructurePiece(List<StructurePiece> options) {
        this.options = List.copyOf(options);
    }

    @Override
    public void validateSetup(StructurePieceSetupMetadata meta) {
        for (StructurePiece option : options) {
            option.validateSetup(meta);
        }
        blocks = options.stream()
                .flatMap(option -> option.createBlocksSupplier().get().stream())
                .distinct()
                .toList();
    }

    @Override
    public boolean formed(Level level, BlockPos pos, StructureModel model) {
        for (StructurePiece option : options) {
            if (option.formed(level, pos, model)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Supplier<List<Block>> createBlocksSupplier() {
        return () -> blocks;
    }

    @Override
    public Component createDisplayComponent() {
        return Component.translatable("piece.mm.any_of.description", options.size()).withStyle(ChatFormatting.DARK_AQUA);
    }

    @Override
    public JsonObject debugExpected(Level level, BlockPos pos, StructureModel model, JsonObject json) {
        var optionsJson = new JsonArray();
        for (StructurePiece option : options) {
            optionsJson.add(option.debugExpected(level, pos, model, new JsonObject()));
        }
        json.add("anyOf", optionsJson);
        return json;
    }

    @Override
    public JsonObject debugFound(Level level, BlockPos pos, StructureModel model, JsonObject json) {
        json.addProperty("block", BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).formed(level, pos, model)) {
                json.addProperty("matchedOption", i);
                break;
            }
        }
        return json;
    }
}
