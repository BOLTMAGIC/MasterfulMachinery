package io.ticticboom.mods.mm.port.mekanism.chemical;

import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.ILockablePortStorage;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.port.mekanism.NotifyChangeContentsListener;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Mekanism 1.21.1 merged the gas, slurry, pigment and infusion registries into a single Chemical
 * type with no discriminator, so this class is not generic over a chemical/stack pair and MM
 * exposes one mm:mekanism/chemical port rather than one per former kind.
 */
public class MekanismChemicalPortStorage implements IPortStorage, ILockablePortStorage {

    public IChemicalTank chemicalTank;
    private final MekanismChemicalPortStorageModel model;
    private final UUID uid = UUID.randomUUID();
    private boolean locked = false;
    @Nullable
    private Chemical lockedType = null;
    private int priority = 0;

    public MekanismChemicalPortStorage(MekanismChemicalPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.chemicalTank = BasicChemicalTank.create(model.amount(), BasicChemicalTank.alwaysTrueBi, BasicChemicalTank.alwaysTrueBi,
                this::isChemicalAllowed, ChemicalAttributeValidator.ALWAYS_ALLOW, new NotifyChangeContentsListener(() -> {
                    if (locked && lockedType == null && !chemicalTank.isEmpty()) {
                        lockedType = chemicalTank.getType();
                    }
                    changed.call();
                }));
    }

    private boolean isChemicalAllowed(Chemical chemical) {
        return lockedType == null || lockedType == chemical;
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

    protected JsonObject debugStack(ChemicalStack stack) {
        var json = new JsonObject();
        var key = MekanismAPI.CHEMICAL_REGISTRY.getKey(stack.getChemical());
        json.addProperty("chemical", key == null ? "null" : key.toString());
        json.addProperty("amount", stack.getAmount());
        return json;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        // BasicChemicalTank implements IChemicalHandler as well as IChemicalTank, so the tank
        // itself satisfies Mekanism's chemical capability.
        return hasCapability(capability) ? (T) chemicalTank : null;
    }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> describeContents() {
        var stack = chemicalTank.getStack();
        if (stack.isEmpty()) {
            return java.util.List.of(net.minecraft.network.chat.Component.translatable("jade.mm.port.empty"));
        }
        return java.util.List.of(net.minecraft.network.chat.Component.translatable("jade.mm.port.chemical",
                stack.getChemical().getTextComponent(), stack.getAmount(), chemicalTank.getCapacity()));
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return capability == Capabilities.CHEMICAL.block();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("handler", chemicalTank.serializeNBT(registries));
        tag.putInt("Priority", priority);
        if (locked) {
            tag.putBoolean("Locked", true);
            var id = lockedType == null ? null : MekanismAPI.CHEMICAL_REGISTRY.getKey(lockedType);
            if (id != null) {
                tag.putString("LockedType", id.toString());
            }
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        locked = tag.getBoolean("Locked");
        priority = Math.max(0, Math.min(10, tag.getInt("Priority")));
        lockedType = null;
        chemicalTank.deserializeNBT(registries, tag.getCompound("handler"));
        if (locked && tag.contains("LockedType")) {
            var id = ResourceLocation.tryParse(tag.getString("LockedType"));
            var chemical = id == null ? null : MekanismAPI.CHEMICAL_REGISTRY.get(id);
            lockedType = chemical == null || chemical.isEmptyType() ? null : chemical;
        }
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Math.max(0, Math.min(10, priority));
    }

    @Override
    public IPortStorageModel getStorageModel() {
        return model;
    }

    @Override
    public boolean carriedByPatterns() {
        return true;
    }

    @Override
    public IPortStorage simulationCopy(HolderLookup.Provider registries) {
        var copy = new MekanismChemicalPortStorage(model, () -> {});
        copy.load(save(new CompoundTag(), registries), registries);
        return copy;
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
        json.add("stack", debugStack(chemicalTank.getStack()));
        json.addProperty("priority", priority);
        return json;
    }

    public ChemicalStack extract(long amount, Action action) {
        return chemicalTank.extract(amount, action, AutomationType.INTERNAL);
    }

    public ChemicalStack insert(ChemicalStack stack, Action action) {
        var leftInStack = chemicalTank.insert(stack, action, AutomationType.INTERNAL);
        long remainingToInsert = stack.getAmount() - leftInStack.getAmount();
        stack.setAmount(remainingToInsert);
        return stack;
    }

    @Override
    public java.util.List<PortContent> contents() {
        var stack = chemicalTank.getStack();
        if (stack.isEmpty()) {
            return java.util.List.of(PortContent.chemical(null, null, 0xFFFFFFFF, 0, chemicalTank.getCapacity()));
        }
        var type = stack.getChemical();
        return java.util.List.of(PortContent.chemical(type.getTextComponent(), type.getIcon(), type.getTint(), stack.getAmount(), chemicalTank.getCapacity()));
    }
}
