package io.ticticboom.mods.mm.port.replication.link;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IControllerAwareStorage;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class ReplicationLinkPortStorage implements IPortStorage, IControllerAwareStorage {

    private static final long CONTROLLER_TIMEOUT = 40;

    private final ReplicationLinkPortStorageModel model;
    private final UUID uid = UUID.randomUUID();
    private WeakReference<MachineControllerBlockEntity> controller = new WeakReference<>(null);
    private long lastSeen = Long.MIN_VALUE / 2;

    public ReplicationLinkPortStorage(ReplicationLinkPortStorageModel model) {
        this.model = model;
    }

    @Override
    public void attachController(MachineControllerBlockEntity controller, long gameTime) {
        if (this.controller.get() != controller) {
            this.controller = new WeakReference<>(controller);
        }
        lastSeen = gameTime;
    }

    public @Nullable MachineControllerBlockEntity getController(long gameTime) {
        var found = controller.get();
        if (found == null || found.isRemoved() || gameTime - lastSeen > CONTROLLER_TIMEOUT) {
            return null;
        }
        return found;
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        return null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
    }

    @Override
    public IPortStorageModel getStorageModel() {
        return model;
    }

    @Override
    public UUID getStorageUid() {
        return uid;
    }

    @Override
    public JsonObject debugDump() {
        var json = new JsonObject();
        json.addProperty("uid", uid.toString());
        json.addProperty("linked", controller.get() != null);
        return json;
    }
}
