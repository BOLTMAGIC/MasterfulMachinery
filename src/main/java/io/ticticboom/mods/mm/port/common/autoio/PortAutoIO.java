package io.ticticboom.mods.mm.port.common.autoio;

import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Per-side automatic transfer for a port.
 * Output ports push their contents into neighbours, input ports pull from neighbours.
 * Enabled sides are stored as a bitmask (bit = {@link Direction#get3DDataValue()}).
 */
public class PortAutoIO {
    public static final String NBT_KEY = "MMAutoIOSides";
    private static final String NBT_FRONT_KEY = "MMAutoIOFront";
    private static final byte ALL_SIDES = 0b111111;

    private final BlockEntity owner;
    private final boolean pull;
    private final IPortTransfer transfer;
    private byte sides;
    // front face of the multiblock this port belongs to, used to name sides; null = not part of a formed multiblock
    @Nullable
    private Direction front;

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

    public void toggleSide(Direction side) {
        sides ^= (byte) (1 << side.get3DDataValue());
    }

    @Nullable
    public Direction getFront() {
        return front;
    }

    /**
     * Looks up the controller of the multiblock this port belongs to and remembers its front,
     * so the GUI can name sides relative to the machine. Server side only.
     *
     * @return true if the front changed
     */
    public boolean refreshFront(ServerLevel level) {
        if (!(owner instanceof IPortBlockEntity port)) {
            return false;
        }
        Direction found = PortSides.findControllerFront(level, owner.getBlockPos(), port.getStorage());
        if (found == front) {
            return false;
        }
        front = found;
        return true;
    }

    public void tick() {
        var level = owner.getLevel();
        if (sides == 0 || level == null || level.isClientSide()) {
            return;
        }

        int interval = MMConfig.PORT_AUTO_IO_INTERVAL;
        // offset by position so that ports placed together don't all transfer on the same tick
        if (Math.floorMod(level.getGameTime() + owner.getBlockPos().asLong(), interval) != 0) {
            return;
        }

        List<Neighbor> neighbors = new ArrayList<>(6);
        for (Direction side : Direction.values()) {
            if (!isSideEnabled(side)) {
                continue;
            }
            BlockPos pos = owner.getBlockPos().relative(side);
            // never force-load a chunk just to look for a neighbour
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null && canTransferWith(be)) {
                neighbors.add(new Neighbor(be, side.getOpposite(), priorityOf(be)));
            }
        }

        // keep the priority setter meaningful: higher priority neighbours are served first
        neighbors.sort(Comparator.comparingInt(Neighbor::priority).reversed());
        for (Neighbor n : neighbors) {
            transfer.transfer(n.be(), n.face(), pull, interval);
        }
    }

    private boolean canTransferWith(BlockEntity neighbor) {
        if (neighbor instanceof IPortBlockEntity port) {
            // Outputs may feed another machine's input port (machine chaining).
            // Inputs never pull from other ports, so two adjacent machines can't ping-pong contents.
            return !pull && port.isInput();
        }
        return true;
    }

    private static int priorityOf(BlockEntity be) {
        if (be instanceof IPortBlockEntity port) {
            return port.getStorage().getPriority();
        }
        return 0;
    }

    public void save(CompoundTag tag) {
        tag.putByte(NBT_KEY, sides);
        tag.putByte(NBT_FRONT_KEY, (byte) (front == null ? -1 : front.get3DDataValue()));
    }

    public void load(CompoundTag tag) {
        // ports saved before this feature existed keep their default
        if (tag.contains(NBT_KEY)) {
            sides = (byte) (tag.getByte(NBT_KEY) & ALL_SIDES);
        }
        int frontValue = tag.contains(NBT_FRONT_KEY) ? tag.getByte(NBT_FRONT_KEY) : -1;
        Direction loaded = frontValue < 0 || frontValue >= 6 ? null : Direction.from3DDataValue(frontValue);
        front = loaded != null && loaded.getAxis().isHorizontal() ? loaded : null;
    }

    private record Neighbor(BlockEntity be, Direction face, int priority) {
    }
}
