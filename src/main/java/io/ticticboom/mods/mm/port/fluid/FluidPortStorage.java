package io.ticticboom.mods.mm.port.fluid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.UUID;

public class FluidPortStorage implements IPortStorage {
    private final FluidPortStorageModel model;

    @Getter
    private final FluidPortHandler handler;
    private final LazyOptional<FluidPortHandler> handlerLazyOptional;
    private final UUID uid = UUID.randomUUID();

    // Priority for outputs. Default 0. Range 0..10.
    private int priority = 0;


    public FluidPortStorage(FluidPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        handler = new FluidPortHandler(model.rows() * model.columns(), model.slotCapacity(), changed);
        handlerLazyOptional = LazyOptional.of(() -> handler);
    }

    public IFluidHandler getWrappedHandler() {
        return new WrappedFluidPortHandler(handler);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability) {
        if (hasCapability(capability)) {
            return handlerLazyOptional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public <T> boolean hasCapability(Capability<T> capability) {
        return MMCapabilities.FLUID == capability;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        var compoundTag = handler.serializeNBT();
        tag.put("handler", compoundTag);
        tag.putInt("Priority", this.priority);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        var compoundTag = tag.get("handler");
        handler.deserializeNBT(compoundTag);
        if (tag.contains("Priority")) {
            this.priority = Math.max(0, Math.min(10, tag.getInt("Priority")));
        } else {
            this.priority = 0;
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
    public JsonObject debugDump() {
        var json = new JsonObject();
        json.addProperty("uid", uid.toString());
        json.addProperty("slotCapacity", model.slotCapacity());
        json.addProperty("rows", model.rows());
        json.addProperty("columns", model.columns());
        json.addProperty("priority", this.priority);
        var tanks = new JsonArray();
        for (int i = 0; i < handler.getTanks(); i++) {
            var stack = handler.getFluidInTank(i);
            var res = JsonOps.INSTANCE.withEncoder(FluidStack.CODEC).apply(stack);
            if (res.result().isPresent()) {
                tanks.add(res.result().get());
            } else {
                tanks.add("Error Serializing Fluid Stack");
            }
        }
        return json;
    }

    public FluidStack getStackInSlot(int slot) {
        return handler.getFluidInTank(slot);
    }

    // Priority accessors
    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = Math.max(0, Math.min(10, priority));
    }
}
