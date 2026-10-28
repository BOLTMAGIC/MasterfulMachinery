package io.ticticboom.mods.mm.port.replication.matter;

import io.ticticboom.mods.mm.port.PortContent;
import com.buuz135.replication.api.IMatterType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.ReplicationCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.IRecipeDemandListener;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReplicationMatterPortStorage implements IPortStorage, IRecipeDemandListener {

    private static final long DEMAND_TIMEOUT = 40;

    private final ReplicationMatterPortStorageModel model;
    @Getter
    private final ReplicationMatterPortHandler handler;
    private final UUID uid = UUID.randomUUID();

    private int priority;
    private RecipeModel demandRecipe;
    private long demandTick = Long.MIN_VALUE / 2;

    public ReplicationMatterPortStorage(ReplicationMatterPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.handler = new ReplicationMatterPortHandler(model, changed);
        this.priority = model.priority();
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) handler;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return ReplicationCapabilities.MATTER_HANDLER == capability;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var tanks = new ListTag();
        for (var tank : handler.tanks()) {
            tanks.add(tank.writeToNBT(new CompoundTag()));
        }
        tag.put("Tanks", tanks);
        tag.putInt("Priority", priority);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        var tanks = tag.getListOrEmpty("Tanks");
        var handlerTanks = handler.tanks();
        for (int i = 0; i < handlerTanks.size(); i++) {
            handlerTanks.get(i).readFromNBT(i < tanks.size() ? tanks.getCompound(i) : new CompoundTag());
        }
        priority = tag.contains("Priority")
                ? ReplicationMatterPortStorageModel.clampPriority(tag.getIntOr("Priority", 0))
                : model.priority();
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
        var dump = new JsonObject();
        dump.addProperty("uid", uid.toString());
        dump.addProperty("capacity", model.capacity());
        dump.addProperty("priority", priority);
        var tanks = new JsonArray();
        for (var tank : handler.tanks()) {
            var entry = new JsonObject();
            var id = MatterTypes.idOf(tank.getMatter().getMatterType());
            entry.addProperty("matter", id == null ? "empty" : id.toString());
            entry.addProperty("amount", tank.getMatterAmount());
            entry.addProperty("filter", tank.getFilter() == null ? null : tank.getFilter().toString());
            tanks.add(entry);
        }
        dump.add("tanks", tanks);
        return dump;
    }

    @Override
    public List<PortContent> contents() {
        var gauges = new ArrayList<PortContent>();
        for (var tank : handler.tanks()) {
            var matter = tank.getMatter();
            gauges.add(PortContent.gauge(matter.isEmpty() ? null : matter.getDisplayName(), MatterTypes.colorOf(matter.isEmpty() ? null : matter.getMatterType()),
                    (long) Math.floor(tank.getMatterAmount()), (long) tank.getCapacity(), ""));
        }
        return gauges;
    }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> describeContents() {
        var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        for (var tank : handler.tanks()) {
            if (tank.getMatter().isEmpty()) {
                lines.add(tank.getFilter() == null
                        ? net.minecraft.network.chat.Component.translatable("jade.mm.port.empty")
                        : net.minecraft.network.chat.Component.translatable("gui.mm.port.replication_matter.reserved", tank.getFilter().toString()));
            } else {
                lines.add(net.minecraft.network.chat.Component.translatable("jade.mm.port.amount",
                        tank.getMatter().getDisplayName(), (int) Math.floor(tank.getMatterAmount()), (int) tank.getCapacity()));
            }
        }
        return lines;
    }

    @Override
    public void setRecipeDemand(long gameTime, @Nullable RecipeModel recipe) {
        demandRecipe = recipe;
        demandTick = gameTime;
    }

    public boolean hasFreshDemand(long gameTime) {
        return gameTime - demandTick <= DEMAND_TIMEOUT;
    }

    public List<IMatterType> demandedMatterTypes() {
        if (demandRecipe == null) {
            return List.of();
        }
        var types = new ArrayList<IMatterType>();
        for (var entry : demandRecipe.inputs().inputs()) {
            if (entry instanceof ConsumeRecipeIngredientEntry consume
                    && consume.getIngredient() instanceof ReplicationMatterPortIngredient matter) {
                var type = MatterTypes.get(matter.getMatterId());
                if (type != null && !types.contains(type)) {
                    types.add(type);
                }
            }
        }
        return types;
    }

    public int internalExtract(IMatterType type, int amount, boolean simulate) {
        var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        return (int) Math.floor(handler.drain(type, amount, action).getAmount());
    }

    public int internalInsert(IMatterType type, int amount, boolean simulate) {
        var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        return (int) Math.floor(handler.fill(type, amount, action));
    }

    public int getStoredMatter(IMatterType type) {
        int stored = 0;
        for (var tank : handler.tanks()) {
            if (!tank.getMatter().isEmpty() && tank.getMatter().getMatterType() == type) {
                stored += (int) Math.floor(tank.getMatterAmount());
            }
        }
        return stored;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = ReplicationMatterPortStorageModel.clampPriority(priority);
    }
}
