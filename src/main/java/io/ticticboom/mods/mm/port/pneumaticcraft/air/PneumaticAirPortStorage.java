package io.ticticboom.mods.mm.port.pneumaticcraft.air;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import lombok.Getter;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.pressure.PressureTier;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.common.capabilities.MachineAirHandler;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

public class PneumaticAirPortStorage implements IPortStorage {
    public final PneumaticAirPortStorageModel model;
    private final INotifyChangeFunction changed;

    @Getter
    private PneumaticAirPortHandler airhandler;
    public final Map<IAirHandlerMachine, List<Direction>> airHandlerMap = new IdentityHashMap();
    private final UUID uid = UUID.randomUUID();

    public PneumaticAirPortStorage(PneumaticAirPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
        airhandler = new PneumaticAirPortHandler(model.tier(), model.volume(), changed);
        this.onNeighborBlockUpdate();
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) airhandler;
        return hasCapability(capability) ? cast : null;
    }


    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return capability == PNCCapabilities.AIR_HANDLER_MACHINE;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Air", serializeNBT());
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        deserializeNBT(tag.get("Air"));
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
        JsonObject dump = new JsonObject();
        dump.addProperty("uid", uid.toString());
        return dump;
    }



    @Override
    public List<PortContent> contents() {
        long capacity = (long) (airhandler.getDangerPressure() * getVolume());
        return List.of(PortContent.gauge(Component.translatable("port.mm.pneumaticcraft_air.name"), 0x8FB8D8, getAir(), capacity, "mB"));
    }

    @Override
    public List<Component> describeContents() {
        return List.of(
                Component.translatable("gui.mm.port.pneumaticcraft_air.pressure", String.format(Locale.ROOT, "%.2f", getPressure())),
                Component.translatable("gui.mm.port.pneumaticcraft_air.air", getAir()));
    }

    public float getPressure(){
        return airhandler.getPressure();
    }

    public int getAir(){
        return airhandler.getAir();
    }

    public int getVolume(){
        return airhandler.getVolume();
    }

    public void addAir(int air){
        airhandler.addAir(air);
    }

    public Tag serializeNBT() {
        return IntTag.valueOf(this.airhandler.getAir());
    }
    public void deserializeNBT(Tag nbt) {
        if (nbt instanceof IntTag intNbt) {
            this.addAir(-(int)this.getAir());
            this.addAir(intNbt.getAsInt());
        } else {
            throw new IllegalArgumentException("Can not deserialize to an instance that isn't the default implementation");
        }

    }

    public void onNeighborBlockUpdate() {
        airHandlerMap.clear();
        for (Direction side : DirectionUtil.VALUES) {
            airHandlerMap.computeIfAbsent(airhandler, k -> new ArrayList<>()).add(side);
        }
        airHandlerMap.forEach(IAirHandlerMachine::setConnectableFaces);
    }



}
