package io.ticticboom.mods.mm.port.botania.mana;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BotaniaManaPortStorage implements IPortStorage {

    private final BotaniaManaPortStorageModel model;
    private final INotifyChangeFunction changed;
    private final UUID uid = UUID.randomUUID();

    @Getter
    private int capacity;
    @Getter
    @Setter
    private int stored = 0;

    public BotaniaManaPortStorage(BotaniaManaPortStorageModel model, INotifyChangeFunction changed) {

        this.model = model;
        this.changed = changed;
        capacity = model.capacity();
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
        tag.putInt("stored", stored);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stored = tag.getIntOr("stored", 0);
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
        return null;
    }

    public int receiveMana(int mana, boolean sim) {
        mana = Math.abs(mana);
        var canBeFilled = Math.min(capacity - stored, mana);
        if (!sim) {
            stored += canBeFilled;
            this.changed.call();
        }
        return canBeFilled;
    }

    public int extractMana(int mana, boolean sim) {
        mana = Math.abs(mana);
        var canBeDrained = Math.min(stored, mana);
        if (!sim) {
            stored -= canBeDrained;
            this.changed.call();
        }
        return canBeDrained;
    }
}
