package io.ticticboom.mods.mm.piece.type.porttype;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.piece.type.StructurePiece;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.WorldUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PortTypeAnywhereStructurePiece extends StructurePiece {

    @Getter
    private final ResourceLocation portTypeId;
    @Getter
    private final Optional<Boolean> input;
    @Getter
    private final int minTier;
    @Getter
    private final int maxTier;

    private final List<Block> blocks = new ArrayList<>();

    public PortTypeAnywhereStructurePiece(ResourceLocation portTypeId, Optional<Boolean> input, int minTier, int maxTier) {
        this.portTypeId = portTypeId;
        this.input = input;
        this.minTier = minTier;
        this.maxTier = maxTier;
    }

    public PortTypeAnywhereStructurePiece(ResourceLocation portTypeId, Optional<Boolean> input, int minTier) {
        this(portTypeId, input, minTier, Integer.MAX_VALUE);
    }

    public static PortTypeAnywhereStructurePiece create(ResourceLocation portTypeId, Optional<Boolean> input, int minTier, int maxTier) {
        return new PortTypeAnywhereStructurePiece(portTypeId, input, minTier, maxTier);
    }

    @Override
    public void validateSetup(StructurePieceSetupMetadata meta) {
        for (RegistryGroupHolder port : MMPortRegistry.PORTS) {
            if (port.getBlock().get() instanceof IPortBlock pb) {
                PortModel model = pb.getModel();
                if (!model.type().equals(portTypeId)) {
                    continue;
                }
                if (input.isPresent() && !input.get().equals(model.input())) {
                    continue;
                }
                // check tier compatibility
                var storageModel = model.config().getModel();
                int candidateRank = storageModel.getTierRank();
                try {
                    if (candidateRank <= 0 && model.jsonConfig() != null && model.jsonConfig().has("tierRank")) {
                        candidateRank = model.jsonConfig().get("tierRank").getAsInt();
                    }
                } catch (Exception ignored) {}
                if (candidateRank <= 0) candidateRank = 1;
                if (candidateRank < minTier) continue;
                if (maxTier != Integer.MAX_VALUE && candidateRank > maxTier) continue;
                var blk = port.getBlock().get();
                if (!blocks.contains(blk)) {
                    blocks.add(blk);
                }
            }
        }
    }

    @Override
    public boolean formed(Level level, BlockPos pos, StructureModel model) {
        // Matched in StructureLayout special pass
        return true;
    }

    @Override
    public Supplier<List<Block>> createBlocksSupplier() {
        return () -> blocks;
    }

    @Override
    public Component createDisplayComponent() {
        return Component.translatable("mm.structure.piece.port_type_anywhere").append(Component.literal(portTypeId.toString()).withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public JsonObject debugExpected(Level level, BlockPos pos, StructureModel model, JsonObject json) {
        json.addProperty("portTypeId", portTypeId.toString());
        json.addProperty("requiresIOCheck", input.isPresent());
        input.ifPresent(aBoolean -> json.addProperty("isInput", aBoolean));
        json.addProperty("minTier", minTier);
        json.addProperty("maxTier", maxTier == Integer.MAX_VALUE ? -1 : maxTier);
        return json;
    }

    @Override
    public JsonObject debugFound(Level level, BlockPos pos, StructureModel model, JsonObject json) {
        var foundBlock = WorldUtil.getBlockState(pos, (ServerLevel) level).getBlock();
        var foundBlockId = ForgeRegistries.BLOCKS.getKey(foundBlock);
        assert foundBlockId != null;
        json.addProperty("block", foundBlockId.toString());
        if (foundBlock instanceof IPortBlock pb) {
            json.addProperty("isPort", true);
            var portType = pb.getModel().type();
            json.addProperty("portTypeId", portType.toString());
            json.addProperty("portId", Ref.id(pb.getModel().id()).toString());
            json.addProperty("isInput", pb.getModel().input());
        } else {
            json.addProperty("isPort", false);
        }
        return json;
    }
}


