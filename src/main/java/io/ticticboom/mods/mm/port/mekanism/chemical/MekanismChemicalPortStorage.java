package io.ticticboom.mods.mm.port.mekanism.chemical;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.ILockablePortStorage;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.Nullable;

import io.ticticboom.mods.mm.port.PortContent;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public abstract class MekanismChemicalPortStorage<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> implements IPortStorage, ILockablePortStorage {

    public IChemicalTank<CHEMICAL, STACK> chemicalTank;
    private final MekanismChemicalPortStorageModel model;
    private final LazyOptional<IChemicalTank<CHEMICAL, STACK>> handleL0;
    private final UUID uid = UUID.randomUUID();

    private boolean locked = false;
    // chemical remembered while locked, null = tank not locked to a chemical yet
    @Nullable
    private CHEMICAL lockedType = null;

    protected MekanismChemicalPortStorage(MekanismChemicalPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        chemicalTank = createTank(model.amount(), this::isChemicalAllowed, () -> {
            if (locked && lockedType == null && !chemicalTank.isEmpty()) {
                lockedType = chemicalTank.getType();
            }
            changed.call();
        });
        handleL0 = LazyOptional.of(() -> chemicalTank);
    }

    protected abstract IChemicalTank<CHEMICAL, STACK> createTank(long capacity, Predicate<CHEMICAL> validator, INotifyChangeFunction changed);
    protected abstract JsonObject debugStack(STACK stack);

    /**
     * @return the Mekanism capability neighbours expose for this chemical type
     */
    public abstract Capability<? extends IChemicalHandler<CHEMICAL, STACK>> getChemicalCapability();

    protected abstract IForgeRegistry<CHEMICAL> getChemicalRegistry();

    @SuppressWarnings("unchecked")
    public IChemicalHandler<CHEMICAL, STACK> getChemicalHandler() {
        // Mekanism's basic tanks are also single-tank handlers
        return (IChemicalHandler<CHEMICAL, STACK>) chemicalTank;
    }

    private boolean isChemicalAllowed(CHEMICAL chemical) {
        return lockedType == null || lockedType == chemical;
    }

    @Override
    public List<PortContent> contents() {
        STACK stack = chemicalTank.getStack();
        if (stack.isEmpty()) {
            return List.of(PortContent.chemical(null, null, 0xFFFFFFFF, 0, chemicalTank.getCapacity()));
        }
        CHEMICAL type = stack.getType();
        return List.of(PortContent.chemical(type.getTextComponent(), type.getIcon(), type.getTint(), stack.getAmount(), chemicalTank.getCapacity()));
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
        lockedType = locked && !chemicalTank.isEmpty() ? chemicalTank.getType() : null;
    }

    @Override
    public void dump() {
        chemicalTank.setEmpty();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability) {
        if (hasCapability(capability)) {
            return handleL0.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("handler", chemicalTank.serializeNBT());
        if (locked) {
            tag.putBoolean("Locked", true);
            ResourceLocation id = lockedType == null ? null : getChemicalRegistry().getKey(lockedType);
            if (id != null) {
                tag.putString("LockedType", id.toString());
            }
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        // restore the lock first so a stale locked type can't reject the saved contents
        locked = tag.getBoolean("Locked");
        lockedType = null;
        if (locked && tag.contains("LockedType")) {
            var id = ResourceLocation.tryParse(tag.getString("LockedType"));
            var chemical = id == null ? null : getChemicalRegistry().getValue(id);
            // unknown ids resolve to the registry default (the empty chemical), which means "not locked"
            lockedType = chemical == null || chemical.isEmptyType() ? null : chemical;
        }
        // Mekanism writes nothing for an empty tank and skips reading then, so clear first; otherwise
        // the client keeps showing the old contents after the tank is emptied (e.g. dumped)
        chemicalTank.setEmpty();
        chemicalTank.deserializeNBT(tag.getCompound("handler"));
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
        JsonObject json = new JsonObject();
        json.addProperty("uid", uid.toString());
        json.addProperty("amount", model.amount());
        var stack = debugStack(chemicalTank.getStack());
        json.add("stack", stack);
        return json;
    }

    public STACK extract(long amount, Action action) {
        return chemicalTank.extract(amount, action, AutomationType.INTERNAL);
    }

    public STACK insert(STACK stack, Action action) {
        var leftInStack = chemicalTank.insert(stack, action, AutomationType.INTERNAL);
        long remainingToInsert = stack.getAmount() - leftInStack.getAmount();
        stack.setAmount(remainingToInsert);
        return stack;
    }
}
