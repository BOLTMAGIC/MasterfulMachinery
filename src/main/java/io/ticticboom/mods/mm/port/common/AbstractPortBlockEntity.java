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
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        getStorage().load(tag.getCompound(Ref.NBT_STORAGE_KEY));
        if (autoIO != null) {
            autoIO.load(tag);
        }
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
