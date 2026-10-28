package io.ticticboom.mods.mm.piece.type.porttype;

import io.ticticboom.mods.mm.builder.PortTiers;
import io.ticticboom.mods.mm.builder.TieredPortPiece;
import java.util.NavigableMap;
import java.util.TreeMap;
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
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import io.ticticboom.mods.mm.util.PortUtils;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PortTypeStructurePiece extends StructurePiece implements TieredPortPiece {

    @Getter
    private final Identifier portTypeId;
    @Getter
    private final Optional<Boolean> input;
    @Getter
    private final int minTier;
    @Getter
    private final int maxTier;

    private final List<Block> blocks = new ArrayList<>();
    @Getter
    private final NavigableMap<Integer, Block> blocksByRank = new TreeMap<>();

    public PortTypeStructurePiece(Identifier portTypeId, Optional<Boolean> input, int minTier, int maxTier) {
        this.portTypeId = portTypeId;
        this.input = input;
        this.minTier = minTier;
        this.maxTier = maxTier;
    }

    // Backwards-compatible constructor: if maxTier wasn't provided, accept any upper tier
    @SuppressWarnings("unused")
    public PortTypeStructurePiece(Identifier portTypeId, Optional<Boolean> input, int minTier) {
        this(portTypeId, input, minTier, Integer.MAX_VALUE);
    }

    @SuppressWarnings("unused")
    public static PortTypeStructurePiece create(Identifier portTypeId, Optional<Boolean> input, int minTier, int maxTier) {
        return new PortTypeStructurePiece(portTypeId, input, minTier, maxTier);
    }

    @Override
    public void validateSetup(StructurePieceSetupMetadata meta) {
        List<PortTiers.RankedPort<Block>> ranked = new ArrayList<>();
        for (RegistryGroupHolder port : MMPortRegistry.PORTS) {
            if (port.getBlock().get() instanceof IPortBlock pb) {
                PortModel model = pb.getModel();
                if (!model.type().equals(portTypeId)) {
                    continue;
                }
                if (!PortUtils.sideMatches(model.type(), model.input(), input)) {
                    continue;
                }
                int candidateRank = PortTiers.rankOf(model);
                if (candidateRank < minTier) continue;
                if (maxTier != Integer.MAX_VALUE && candidateRank > maxTier) continue;
                var blk = port.getBlock().get();
                if (!blocks.contains(blk)) {
                    blocks.add(blk);
                }
                ranked.add(new PortTiers.RankedPort<>(candidateRank, model.input(), blk));
            }
        }
        blocksByRank.clear();
        blocksByRank.putAll(PortTiers.byRank(ranked));
    }

    @Override
    public boolean formed(Level level, BlockPos pos, StructureModel model) {
        var be = WorldUtil.getBlockEntity(pos, (ServerLevel) level);
        if (be instanceof IPortBlockEntity pbe) {
            if (!pbe.getModel().type().equals(portTypeId)) {
                return false;
            }
            if (!PortUtils.sideMatches(pbe.getModel().type(), pbe.getModel().input(), input)) {
                return false;
            }
            int candidateRank = PortTiers.rankOf(pbe.getModel());
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
        return Component.translatable("piece.mm.port_type.description", Component.literal(portTypeId.toString()).withStyle(ChatFormatting.DARK_AQUA));
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
        var foundBlockId = BuiltInRegistries.BLOCK.getKey(foundBlock);
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
