package io.ticticboom.mods.mm.port.common;

import io.ticticboom.mods.mm.util.LegacyNbt;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortPart;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public abstract class AbstractPortBlockEntity extends BlockEntity implements IPortBlockEntity, IPortPart {

    static final int SYNC_INTERVAL = 10;
    private static final String FRONT_KEY = "MMMachineFront";
    private static final String STATE_KEY = "MMMachineState";
    private static final String COLORS_KEY = "MMMachineColors";

    protected long lastTick = 0;
    @Nullable
    protected PortAutoIO autoIO;
    @Nullable
    private Direction machineFront;
    private ControllerState machineState = ControllerState.UNFORMED;
    @Nullable
    private BlockPos controllerPos;
    @Nullable
    private int[] machineColors;
    private long changeCount = 0;
    private long lastSync = Long.MIN_VALUE / 2;
    private boolean syncPending = false;
    // game time of the last setChanged that told neighbors (comparators)
    private long lastMarkedTick = Long.MIN_VALUE;

    public AbstractPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public PortAutoIO getAutoIO() {
        return autoIO;
    }

    @Nullable
    public Direction getMachineFront() {
        return machineFront;
    }

    public ControllerState getMachineState() {
        return machineState;
    }

    public int getMachineColor(ControllerState state) {
        return machineColors == null ? -1 : machineColors[state.ordinal()];
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setMachineInfo(@Nullable BlockPos controller, @Nullable Direction front, ControllerState state, @Nullable int[] colors) {
        if (java.util.Objects.equals(controller, controllerPos) && front == machineFront && state == machineState && Arrays.equals(colors, machineColors)) {
            return;
        }
        controllerPos = controller;
        machineFront = front;
        machineState = state;
        machineColors = colors;
        if (level != null && !level.isClientSide()) {
            level.blockEntityChanged(getBlockPos());
            requestSync();
        }
    }

    public void tick() {
        if (level == null || lastTick == level.getGameTime()) {
            return;
        }
        lastTick = level.getGameTime();
        if (autoIO != null) {
            autoIO.tick();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var tag = new CompoundTag();
        saveLegacy(tag, LegacyNbt.registries(level));
        LegacyNbt.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadLegacy(LegacyNbt.read(input), input.lookup());
    }

    protected void saveLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(Ref.NBT_STORAGE_KEY, getStorage().save(new CompoundTag(), registries));
        if (autoIO != null) {
            autoIO.save(tag);
        }
        if (machineFront != null) {
            tag.putByte(FRONT_KEY, (byte) machineFront.get2DDataValue());
        }
        tag.putByte(STATE_KEY, (byte) machineState.ordinal());
        if (machineColors != null) {
            tag.putIntArray(COLORS_KEY, machineColors);
        }
    }

    protected void loadLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        getStorage().load(tag.getCompoundOrEmpty(Ref.NBT_STORAGE_KEY), registries);
        if (autoIO != null) {
            autoIO.load(tag);
        }
        machineFront = tag.contains(FRONT_KEY) ? Direction.from2DDataValue(tag.getByteOr(FRONT_KEY, (byte) 0)) : null;
        var states = ControllerState.values();
        int state = tag.getByteOr(STATE_KEY, (byte) 0);
        machineState = state >= 0 && state < states.length ? states[state] : ControllerState.UNFORMED;
        int[] colors = tag.getIntArray(COLORS_KEY).orElse(new int[0]);
        machineColors = colors.length == states.length ? colors : null;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveLegacy(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public long changeCount() {
        return changeCount;
    }

    @Override
    public void setChanged() {
        if (level == null || level.isClientSide()) {
            return;
        }
        // ports change many times a tick (each insert/extract); comparators read the final contents when they
        // update, so neighbors are told once a tick. The chunk is still marked dirty every time (a save may have
        // happened in between).
        long gameTime = level.getGameTime();
        if (gameTime == lastMarkedTick) {
            level.blockEntityChanged(worldPosition);
        } else {
            lastMarkedTick = gameTime;
            super.setChanged();
        }
        changeCount++;
        requestSync();
    }

    private void requestSync() {
        if (syncPending) {
            return;
        }
        if (level.getGameTime() - lastSync >= SYNC_INTERVAL) {
            sendSync();
        } else {
            syncPending = true;
            PortSyncQueue.add(this);
        }
    }

    boolean flushSync() {
        if (level == null || isRemoved()) {
            return true;
        }
        if (level.getGameTime() - lastSync < SYNC_INTERVAL) {
            return false;
        }
        syncPending = false;
        sendSync();
        return true;
    }

    private void sendSync() {
        lastSync = level.getGameTime();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
