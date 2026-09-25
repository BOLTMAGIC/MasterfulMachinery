package io.ticticboom.mods.mm.port.common;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortPart;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import net.minecraft.core.BlockPos;
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

public abstract class AbstractPortBlockEntity extends BlockEntity implements IPortBlockEntity, IPortPart {

    protected long lastTick = 0;
    @Nullable
    protected PortAutoIO autoIO;
    // status light colors of the machine this port belongs to (unformed, idle, working; -1 = client config),
    // set by the controller; null when the machine doesn't set its own colors
    @Nullable
    private int[] machineColors;
    private static final String MACHINE_COLORS_KEY = "MMMachineColors";

    public AbstractPortBlockEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    /**
     * @return the auto push/pull feature of this port, or null if the port type doesn't support it
     */
    @Nullable
    public PortAutoIO getAutoIO() {
        return autoIO;
    }

    /**
     * @param state 0 unformed, 1 idle, 2 working
     * @return the machine's own status light color for that state, or -1 to use the client config
     */
    public int getMachineColor(int state) {
        return machineColors == null ? -1 : machineColors[state];
    }

    /** Set by the controller of the machine the port belongs to; null clears. Sent to clients when it changes. */
    public void setMachineColors(@Nullable int[] colors) {
        if (java.util.Arrays.equals(machineColors, colors)) {
            return;
        }
        machineColors = colors == null ? null : colors.clone();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void tick() {
        if (level == null || lastTick == level.getGameTime()) return;
        lastTick = level.getGameTime();
        if (autoIO != null) {
            autoIO.tick();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put(Ref.NBT_STORAGE_KEY, getStorage().save(new CompoundTag()));
        if (autoIO != null) {
            autoIO.save(tag);
        }
        if (machineColors != null) {
            tag.putIntArray(MACHINE_COLORS_KEY, machineColors);
        }
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        getStorage().load(tag.getCompound(Ref.NBT_STORAGE_KEY));
        if (autoIO != null) {
            autoIO.load(tag);
        }
        int[] loaded = tag.getIntArray(MACHINE_COLORS_KEY);
        machineColors = loaded.length == 3 ? loaded : null;
        super.load(tag);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        if (level == null || level.isClientSide()){
            return;
        }
        super.setChanged();
        level.sendBlockUpdated(getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }
}
