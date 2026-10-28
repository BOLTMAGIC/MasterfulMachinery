package io.ticticboom.mods.mm.port.projecte.emc;

import io.ticticboom.mods.mm.util.LegacyNbt;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.ProjectECapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.PortContent;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import lombok.Setter;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ProjectEEmcPortStorage implements IPortStorage {

    public static final int FILL_COLOR = 0xFFF0BE28;

    public static final int KLEIN_SLOT_X = 79;
    public static final int KLEIN_SLOT_Y = 28;

    private final ProjectEEmcPortStorageModel model;
    private final INotifyChangeFunction changed;
    private final UUID uid = UUID.randomUUID();

    @Getter
    private final long capacity;
    @Getter
    private long stored = 0;
    @Setter
    private boolean input = false;
    private final EmcCap emcCap = new EmcCap();
    @Getter
    private final ItemStackHandler klein;

    public ProjectEEmcPortStorage(ProjectEEmcPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
        this.capacity = Math.max(0, model.capacity());
        this.klein = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                changed.call();
            }
        };
    }

    public boolean hasKleinSlot() {
        return model.kleinSlot();
    }

    private long kleinLimit(long amount) {
        return model.kleinRate() > 0 ? Math.min(amount, model.kleinRate()) : amount;
    }

    public void tickKlein() {
        if (!model.kleinSlot()) {
            return;
        }
        var stack = klein.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }
        var holder = stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
        if (holder == null) {
            return;
        }
        if (input) {
            long room = capacity - stored;
            if (room > 0) {
                receive(holder.extractEmc(stack, kleinLimit(room), IEmcStorage.EmcAction.EXECUTE), false);
            }
        } else if (stored > 0) {
            extract(holder.insertEmc(stack, kleinLimit(stored), IEmcStorage.EmcAction.EXECUTE), false);
        }
    }

    @Override
    public void setupContainer(AbstractContainerMenu container, Inventory inv, PortModel portModel) {
        if (model.kleinSlot()) {
            container.addSlot(new SlotItemHandler(klein, 0, KLEIN_SLOT_X + 1, KLEIN_SLOT_Y + 1));
        }
        IPortStorage.super.setupContainer(container, inv, portModel);
    }

    public long receive(long emc, boolean simulate) {
        var accepted = Math.min(capacity - stored, Math.max(0, emc));
        if (!simulate && accepted > 0) {
            stored += accepted;
            changed.call();
        }
        return accepted;
    }

    public long extract(long emc, boolean simulate) {
        var drained = Math.min(stored, Math.max(0, emc));
        if (!simulate && drained > 0) {
            stored -= drained;
            changed.call();
        }
        return drained;
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) emcCap;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return capability == ProjectECapabilities.EMC_STORAGE;
    }

    private class EmcCap implements IEmcStorage {

        @Override
        public long getStoredEmc() {
            return stored;
        }

        @Override
        public long getMaximumEmc() {
            return Math.max(1, capacity);
        }

        @Override
        public long insertEmc(long emc, EmcAction action) {
            if (emc < 0) {
                return extractEmc(-emc, action);
            }
            return input ? receive(emc, action.simulate()) : 0;
        }

        @Override
        public long extractEmc(long emc, EmcAction action) {
            if (emc < 0) {
                return insertEmc(-emc, action);
            }
            return input ? 0 : extract(emc, action.simulate());
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("stored", stored);
        tag.put("klein", LegacyNbt.save(klein, registries));
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stored = Math.min(capacity, tag.getLongOr("stored", 0L));
        if (tag.contains("klein")) {
            LegacyNbt.load(klein, registries, tag.getCompoundOrEmpty("klein"));
        }
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
    public List<PortContent> contents() {
        return List.of(PortContent.gauge(Component.translatable("port.mm.projecte_emc.name"), 0xE0B020, stored, capacity, "EMC"));
    }

    @Override
    public List<Component> describeContents() {
        return List.of(Component.translatable("gui.mm.port.projecte_emc.storage", stored, capacity));
    }

    @Override
    public double fillRatio() {
        return capacity <= 0 ? -1 : Math.min(1, (double) stored / capacity);
    }

    @Override
    public int fillColor() {
        return FILL_COLOR;
    }

    @Override
    public JsonObject debugDump() {
        var json = new JsonObject();
        json.addProperty("uid", uid.toString());
        json.addProperty("stored", stored);
        json.addProperty("capacity", capacity);
        json.addProperty("kleinSlot", model.kleinSlot());
        json.addProperty("kleinRate", model.kleinRate());
        return json;
    }
}
