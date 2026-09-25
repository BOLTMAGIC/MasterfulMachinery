package io.ticticboom.mods.mm.structure;

import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Why a controller's multiblock isn't formed: the blocks missing or wrong in the closest matching
 * structure and rotation, so the controller screen can tell the player what to place where.
 *
 * @param total   how many positions are wrong in total; {@code missing} may hold fewer
 * @param missing the first wrong positions
 */
public record StructureDiagnosis(int total, List<Missing> missing) {
    /** At most this many positions are sent to the client. */
    public static final int MAX_SENT = 32;
    public static final StructureDiagnosis NONE = new StructureDiagnosis(0, List.of());

    /**
     * @param pos      world position
     * @param icon     item of the (first) block that belongs there, empty if there is none
     * @param required what belongs there
     * @param found    the block there now, or null if the position is empty
     */
    public record Missing(BlockPos pos, ItemStack icon, Component required, @Nullable Component found) {
    }

    /**
     * Picks, over the given structures and all rotations, the one with the fewest wrong positions.
     */
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
            var state = level.getBlockState(pos);
            result.add(new Missing(pos, icon, required, state.isAir() ? null : state.getBlock().getName()));
        }
        return new StructureDiagnosis(best.size(), result);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(total);
        buf.writeCollection(missing, (b, m) -> {
            b.writeBlockPos(m.pos());
            b.writeItem(m.icon());
            b.writeComponent(m.required());
            b.writeNullable(m.found(), FriendlyByteBuf::writeComponent);
        });
    }

    public static StructureDiagnosis read(FriendlyByteBuf buf) {
        int total = buf.readVarInt();
        List<Missing> missing = buf.readList(b -> new Missing(b.readBlockPos(), b.readItem(), b.readComponent(),
                b.readNullable(FriendlyByteBuf::readComponent)));
        return new StructureDiagnosis(total, missing);
    }
}
