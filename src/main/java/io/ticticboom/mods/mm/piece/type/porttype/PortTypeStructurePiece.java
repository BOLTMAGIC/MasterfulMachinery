package io.ticticboom.mods.mm.piece.type.porttype;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.piece.type.StructurePiece;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.WorldUtil;
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
public class PortTypeStructurePiece extends StructurePiece {

    private final ResourceLocation portTypeId;
    private final Optional<Boolean> input;
    private final int minTier;
    private final int maxTier;

    private final List<Block> blocks = new ArrayList<>();

    public PortTypeStructurePiece(ResourceLocation portTypeId, Optional<Boolean> input, int minTier, int maxTier) {
        this.portTypeId = portTypeId;
        this.input = input;
        this.minTier = minTier;
        this.maxTier = maxTier;
    }

    // Backwards-compatible constructor: if maxTier wasn't provided, accept any upper tier
    public PortTypeStructurePiece(ResourceLocation portTypeId, Optional<Boolean> input, int minTier) {
        this(portTypeId, input, minTier, Integer.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    public static PortTypeStructurePiece create(ResourceLocation portTypeId, Optional<Boolean> input, int minTier, int maxTier) {
        return new PortTypeStructurePiece(portTypeId, input, minTier, maxTier);
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
                // treat unspecified/zero tier as 1 (backwards-compatible default for ports without tierRank)
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
        var be = WorldUtil.getBlockEntity(pos, (ServerLevel) level);
        if (be instanceof IPortBlockEntity pbe) {
            if (!pbe.getModel().type().equals(portTypeId)) {
                return false;
            }
            if (input.isPresent() && !input.get().equals(pbe.getModel().input())) {
                return false;
            }
            // check tier compatibility
            var storageModel = pbe.getModel().config().getModel();
            int candidateRank = storageModel.getTierRank();
            try {
                if (candidateRank <= 0 && pbe.getModel().jsonConfig() != null && pbe.getModel().jsonConfig().has("tierRank")) {
                    candidateRank = pbe.getModel().jsonConfig().get("tierRank").getAsInt();
                }
            } catch (Exception ignored) {}
            // treat unspecified/zero tier as 1 (backwards-compatible default for ports without tierRank)
            if (candidateRank <= 0) candidateRank = 1;
            if (candidateRank < minTier) return false;
            return maxTier == Integer.MAX_VALUE || candidateRank <= maxTier;
        }
        return false;
    }

    @Override
    public Supplier<List<Block>> createBlocksSupplier() {
        return () -> blocks;
    }

    @Override
    public Component createDisplayComponent() {
        return Component.literal("Port Type: ").append(Component.literal(portTypeId.toString()).withStyle(ChatFormatting.DARK_AQUA));
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
