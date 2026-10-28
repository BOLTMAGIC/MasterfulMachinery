package io.ticticboom.mods.mm.structure.attachment.visuals;

import io.ticticboom.mods.mm.util.LegacyNbt;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public final class FluidVolumeState {
    private final long[] cells;
    private final FluidStack fluid;
    private final float fill;
    private final LongOpenHashSet cellSet;
    private final boolean gas;
    private final float level;
    private final AABB bounds;

    public FluidVolumeState(long[] cells, FluidStack fluid, float fill) {
        this.cells = cells;
        this.fluid = fluid;
        this.fill = fill;
        this.cellSet = new LongOpenHashSet(cells);
        this.gas = !fluid.isEmpty() && fluid.getFluid().getFluidType().isLighterThanAir();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long cell : cells) {
            minX = Math.min(minX, BlockPos.getX(cell));
            minY = Math.min(minY, BlockPos.getY(cell));
            minZ = Math.min(minZ, BlockPos.getZ(cell));
            maxX = Math.max(maxX, BlockPos.getX(cell));
            maxY = Math.max(maxY, BlockPos.getY(cell));
            maxZ = Math.max(maxZ, BlockPos.getZ(cell));
        }
        this.bounds = cells.length == 0 ? null : new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        this.level = cells.length == 0 ? 0 : computeLevel(minY, maxY);
    }

    private float computeLevel(int minY, int maxY) {
        int[] counts = new int[maxY - minY + 1];
        for (long cell : cells) {
            counts[BlockPos.getY(cell) - minY]++;
        }
        float remaining = fill * cells.length;
        for (int i = 0; i < counts.length; i++) {
            int layer = gas ? counts.length - 1 - i : i;
            int count = counts[layer];
            if (count == 0) {
                continue;
            }
            if (remaining < count) {
                float part = remaining / count;
                return gas ? minY + layer + 1 - part : minY + layer + part;
            }
            remaining -= count;
        }
        return gas ? minY : maxY + 1;
    }

    public boolean sameAs(FluidVolumeState other) {
        return fill == other.fill && Arrays.equals(cells, other.cells)
                && FluidStack.isSameFluidSameComponents(fluid, other.fluid);
    }

    public static boolean same(List<FluidVolumeState> a, List<FluidVolumeState> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).sameAs(b.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static ListTag saveAll(List<FluidVolumeState> states, HolderLookup.Provider registries) {
        var list = new ListTag();
        for (var state : states) {
            var tag = new CompoundTag();
            tag.putLongArray("Cells", state.cells);
            tag.put("Fluid", LegacyNbt.saveFluid(state.fluid, registries));
            tag.putFloat("Fill", state.fill);
            list.add(tag);
        }
        return list;
    }

    public static List<FluidVolumeState> loadAll(ListTag list, HolderLookup.Provider registries) {
        var states = new ArrayList<FluidVolumeState>();
        for (int i = 0; i < list.size(); i++) {
            var tag = list.getCompoundOrEmpty(i);
            states.add(new FluidVolumeState(
                    tag.getLongArray("Cells").orElse(new long[0]),
                    LegacyNbt.parseFluid(registries, tag.getCompoundOrEmpty("Fluid")),
                    tag.getFloatOr("Fill", 0f)));
        }
        return states;
    }
}
