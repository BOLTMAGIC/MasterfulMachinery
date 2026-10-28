package io.ticticboom.mods.mm.structure;

import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record StructureDiagnosis(int total, List<Missing> missing) {
    public static final int MAX_SENT = 32;
    public static final StructureDiagnosis NONE = new StructureDiagnosis(0, List.of());

    public record Missing(BlockPos pos, ItemStack icon, Component required, @Nullable Component found, List<Component> options) {
    }

    public static StructureDiagnosis diagnose(Level level, BlockPos controllerPos, Iterable<StructureModel> structures) {
        List<PositionedLayoutPiece> best = null;
        for (StructureModel structure : structures) {
            var missing = structure.layout().closestMissing(level, controllerPos, structure);
            if (best == null || missing.size() < best.size()) {
                best = missing;
            }
        }
        if (best == null || best.isEmpty()) {
            return NONE;
        }
        var result = new ArrayList<Missing>();
        for (PositionedLayoutPiece piece : best.subList(0, Math.min(MAX_SENT, best.size()))) {
            BlockPos pos = piece.findAbsolutePos(controllerPos);
            List<Block> blocks = piece.piece().piece().createBlocksSupplier().get();
            ItemStack icon = blocks.isEmpty() ? ItemStack.EMPTY : new ItemStack(blocks.get(0));
            Component required;
            if (blocks.isEmpty()) {
                required = piece.piece().piece().createDisplayComponent();
            } else if (blocks.size() == 1) {
                required = blocks.get(0).getName();
            } else {
                required = Component.translatable("gui.mm.controller.missing.one_of", blocks.get(0).getName(), blocks.size() - 1);
            }
            List<Component> options = blocks.size() > 1 ? blocks.stream().map(b -> (Component) b.getName()).toList() : List.of();
            var state = level.getBlockState(pos);
            result.add(new Missing(pos, icon, required, state.isAir() ? null : state.getBlock().getName(), options));
        }
        return new StructureDiagnosis(best.size(), result);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(total);
        buf.writeVarInt(missing.size());
        for (Missing m : missing) {
            buf.writeBlockPos(m.pos());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, m.icon());
            ComponentSerialization.STREAM_CODEC.encode(buf, m.required());
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC).encode(buf, java.util.Optional.ofNullable(m.found()));
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, m.options());
        }
    }

    public static StructureDiagnosis read(RegistryFriendlyByteBuf buf) {
        int total = buf.readVarInt();
        int size = buf.readVarInt();
        var missing = new ArrayList<Missing>(size);
        for (int i = 0; i < size; i++) {
            missing.add(new Missing(buf.readBlockPos(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ComponentSerialization.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC).decode(buf).orElse(null),
                    ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf)));
        }
        return new StructureDiagnosis(total, missing);
    }
}
