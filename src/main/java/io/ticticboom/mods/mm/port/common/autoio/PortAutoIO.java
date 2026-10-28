package io.ticticboom.mods.mm.port.common.autoio;

import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PortAutoIO {
    private static final String NBT_KEY = "MMAutoIOSides";
    private static final byte ALL_SIDES = 0b111111;

    private final BlockEntity owner;
    private final boolean pull;
    private final IPortTransfer transfer;
    private byte sides;

    public PortAutoIO(BlockEntity owner, boolean pull, boolean enabledByDefault, IPortTransfer transfer) {
        this.owner = owner;
        this.pull = pull;
        this.transfer = transfer;
        this.sides = enabledByDefault ? ALL_SIDES : 0;
    }

    public boolean isPull() {
        return pull;
    }

    public boolean isSideEnabled(Direction side) {
        return (sides & (1 << side.get3DDataValue())) != 0;
    }

    public int enabledSideCount() {
        return Integer.bitCount(sides);
    }

    public void toggleSide(Direction side) {
        sides ^= (byte) (1 << side.get3DDataValue());
    }

    public void tick() {
        if (sides == 0 || !(owner.getLevel() instanceof ServerLevel level)) {
            return;
        }
        int interval = MMConfig.PORT_AUTO_IO_INTERVAL;
        if (Math.floorMod(level.getGameTime() + owner.getBlockPos().asLong(), interval) != 0) {
            return;
        }
        List<Neighbor> neighbors = new ArrayList<>(6);
        for (Direction side : Direction.values()) {
            if (!isSideEnabled(side)) {
                continue;
            }
            BlockPos pos = owner.getBlockPos().relative(side);
            if (!level.isLoaded(pos)) {
                continue;
            }
            var be = level.getBlockEntity(pos);
            int priority = 0;
            if (be instanceof IPortBlockEntity port) {
                if (pull || !port.isInput()) {
                    continue;
                }
                priority = port.getStorage().getPriority();
            }
            neighbors.add(new Neighbor(pos, side.getOpposite(), priority));
        }
        neighbors.sort(Comparator.comparingInt(Neighbor::priority).reversed());
        for (Neighbor n : neighbors) {
            transfer.transfer(level, n.pos(), n.face(), pull, interval);
        }
    }

    public void save(CompoundTag tag) {
        tag.putByte(NBT_KEY, sides);
    }

    public void load(CompoundTag tag) {
        if (tag.contains(NBT_KEY)) {
            sides = (byte) (tag.getByteOr(NBT_KEY, (byte) 0) & ALL_SIDES);
        }
    }

    private record Neighbor(BlockPos pos, Direction face, int priority) {
    }
}
