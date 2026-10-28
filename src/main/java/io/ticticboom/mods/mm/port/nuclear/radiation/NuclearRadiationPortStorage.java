package io.ticticboom.mods.mm.port.nuclear.radiation;

import io.ticticboom.mods.mm.cap.transfer.TransferSnapshot;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.util.LegacyNbt;
import io.ticticboom.mods.mm.port.PortContent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import igentuman.nr.api.RadiationProfile;
import igentuman.nr.api.binding.RadiationBindings;
import igentuman.nr.api.binding.RadiationComponent;
import igentuman.nr.api.isotope.Isotope;
import igentuman.nr.api.isotope.IsotopeRegistry;
import igentuman.nr.api.isotope.IsotopeStack;
import igentuman.nr.util.Units;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import io.ticticboom.mods.mm.util.RadiationText;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NuclearRadiationPortStorage implements IPortStorage {

    public static final int SLOT_Y = 116;
    public static final int INPUT_SLOT_X = 80;
    public static final int CARRIER_SLOT_X = 62;
    public static final int LOADED_SLOT_X = 98;
    private static final double EMPTY_BQ = 1.0e-6;
    private static final int DECAY_INTERVAL = 20;
    private static final int GUI_LINES = 7;

    private final NuclearRadiationPortStorageModel model;
    private final INotifyChangeFunction changed;
    private final UUID uid = UUID.randomUUID();
    private final Map<String, IsotopeStack> stored = new LinkedHashMap<>();
    private final List<CompoundTag> unresolved = new ArrayList<>();
    private final Map<String, Double> displayBq = new LinkedHashMap<>();
    @Getter
    private final ItemStackHandler items;
    private final IItemHandler automation = new AutomationHandler();
    private boolean inputSide;
    private boolean loading;
    private long clock;
    private long lastDecay;
    private int priority;

    public NuclearRadiationPortStorage(NuclearRadiationPortStorageModel model, INotifyChangeFunction changed) {
        this.model = model;
        this.changed = changed;
        this.items = new ItemStackHandler(2) {
            @Override
            public boolean isItemValid(int index, ItemStack stack) {
                if (index == 0) {
                    return inputSide ? canAbsorb(stack) : canLoad(stack);
                }
                return !inputSide;
            }

            @Override
            protected int getStackLimit(int index, ItemStack stack) {
                int limit = super.getStackLimit(index, stack);
                return index == 0 && inputSide ? Math.min(limit, absorbableCount(stack)) : limit;
            }

            @Override
            protected void onContentsChanged(int index) {
                if (!loading) {
                    changed.call();
                }
            }
        };
    }

    public void setInputSide(boolean inputSide) {
        this.inputSide = inputSide;
    }

    public void tick(long gameTime) {
        clock = gameTime;
        if (gameTime - lastDecay >= DECAY_INTERVAL) {
            lastDecay = gameTime;
            decay(gameTime);
        }
        if (inputSide) {
            absorb();
        } else {
            load();
        }
    }

    private void decay(long gameTime) {
        boolean anyChange = false;
        var iterator = stored.values().iterator();
        while (iterator.hasNext()) {
            var stack = iterator.next();
            if (model.decay()) {
                if (stack.advanceDecay(gameTime) > 0) {
                    anyChange = true;
                }
            } else {
                stack.setTimestamp(gameTime);
            }
            if (stack.currentActivityBq() < EMPTY_BQ) {
                iterator.remove();
                anyChange = true;
            }
        }
        if (anyChange) {
            changed.call();
        }
    }

    private void absorb() {
        var item = items.getStackInSlot(0);
        if (item.isEmpty()) {
            return;
        }
        var profile = RadiationBindings.of(item);
        double perItem = perItemBq(profile);
        if (perItem <= 0) {
            return;
        }
        double space = model.capacity() - totalBq();
        int count = (int) Math.min(item.getCount(), Math.floor(space / perItem));
        if (count <= 0) {
            return;
        }
        for (var source : profile.stacks()) {
            var stack = stackFor(source.isotope());
            stack.setAtoms(stack.atoms() + source.atoms() * count);
        }
        items.extractItem(0, count, false);
        changed.call();
    }

    private void load() {
        var carrier = items.getStackInSlot(0);
        if (carrier.isEmpty() || !canLoad(carrier)) {
            return;
        }
        var existing = items.getStackInSlot(1);
        var existingComponent = existing.isEmpty() ? null : existing.get(RadiationComponent.TYPE.get());
        RadiationComponent component;
        var take = new LinkedHashMap<IsotopeStack, Double>();
        if (existingComponent != null && ItemStack.isSameItem(existing, carrier)) {
            component = existingComponent;
            for (var source : existingComponent.toProfile(clock).stacks()) {
                var stack = stored.get(source.isotope().id());
                double bq = source.currentActivityBq();
                if (stack == null || stack.currentActivityBq() < bq) {
                    return;
                }
                take.put(stack, bq);
            }
        } else {
            double perItem = model.loadPerItem();
            double total = totalBq();
            if (perItem <= 0 || total < perItem) {
                return;
            }
            var atoms = new LinkedHashMap<String, Double>();
            for (var entry : percentShares(total).entrySet()) {
                var stack = entry.getKey();
                double bq = perItem * entry.getValue() / 100.0;
                if (stack.currentActivityBq() < bq) {
                    return;
                }
                atoms.put(stack.isotope().id(), atomsFor(stack.isotope(), bq));
                take.put(stack, bq);
            }
            component = new RadiationComponent(atoms, clock);
        }
        if (take.isEmpty()) {
            return;
        }
        var loaded = carrier.copyWithCount(1);
        loaded.set(RadiationComponent.TYPE.get(), component);
        if (!items.insertItem(1, loaded, true).isEmpty()) {
            return;
        }
        items.insertItem(1, loaded, false);
        items.extractItem(0, 1, false);
        take.forEach(this::removeActivity);
        changed.call();
    }

    private Map<IsotopeStack, Integer> percentShares(double total) {
        var shares = new LinkedHashMap<IsotopeStack, Integer>();
        IsotopeStack largest = null;
        int assigned = 0;
        for (var stack : stored.values()) {
            double bq = stack.currentActivityBq();
            if (largest == null || bq > largest.currentActivityBq()) {
                largest = stack;
            }
            int percent = (int) Math.round(bq / total * 100);
            if (percent > 0) {
                shares.put(stack, percent);
                assigned += percent;
            }
        }
        if (largest != null && assigned != 100) {
            shares.merge(largest, 100 - assigned, Integer::sum);
        }
        shares.values().removeIf(percent -> percent <= 0);
        return shares;
    }

    private double perItemBq(RadiationProfile profile) {
        if (profile.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (var source : profile.stacks()) {
            if (source.isotope().halfLifeTicks() <= 0 || !model.accepts(source.isotope().id())) {
                return 0;
            }
            total += source.currentActivityBq();
        }
        return total;
    }

    public boolean canAbsorb(ItemStack stack) {
        return absorbableCount(stack) > 0;
    }

    public int absorbableCount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        double perItem = perItemBq(RadiationBindings.of(stack));
        if (perItem <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.floor((model.capacity() - totalBq()) / perItem));
    }

    public boolean canLoad(ItemStack stack) {
        return !stack.isEmpty() && isCarrier(stack) && !RadiationBindings.isRadioactive(stack);
    }

    private boolean isCarrier(ItemStack stack) {
        if (model.carriers().isEmpty()) {
            return true;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (String entry : model.carriers()) {
            if (entry.startsWith("#")) {
                var tag = Identifier.tryParse(entry.substring(1));
                if (tag != null && stack.is(TagKey.create(Registries.ITEM, tag))) {
                    return true;
                }
            } else if (entry.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private IsotopeStack stackFor(Isotope isotope) {
        return stored.computeIfAbsent(isotope.id(), id -> new IsotopeStack(isotope, 0, clock));
    }

    private static double atomsFor(Isotope isotope, double bq) {
        return bq * isotope.halfLifeTicks() * Units.SECONDS_PER_TICK / Units.LN2;
    }

    public double totalBq() {
        double total = 0;
        for (var stack : stored.values()) {
            total += stack.currentActivityBq();
        }
        return total;
    }

    public double storedBq(@Nullable String isotope) {
        if (isotope == null) {
            return totalBq();
        }
        var stack = stored.get(isotope);
        return stack == null ? 0 : stack.currentActivityBq();
    }

    public double extract(@Nullable String isotope, double bq, boolean simulate) {
        if (bq <= 0) {
            return 0;
        }
        if (isotope != null) {
            var stack = stored.get(isotope);
            if (stack == null) {
                return 0;
            }
            double taken = Math.min(bq, stack.currentActivityBq());
            if (!simulate && taken > 0) {
                removeActivity(stack, taken);
                changed.call();
            }
            return taken;
        }
        double total = totalBq();
        double taken = Math.min(bq, total);
        if (!simulate && taken > 0) {
            for (var stack : new ArrayList<>(stored.values())) {
                removeActivity(stack, stack.currentActivityBq() / total * taken);
            }
            changed.call();
        }
        return taken;
    }

    private void removeActivity(IsotopeStack stack, double bq) {
        stack.setAtoms(Math.max(0, stack.atoms() - atomsFor(stack.isotope(), bq)));
        if (stack.currentActivityBq() < EMPTY_BQ) {
            stored.remove(stack.isotope().id());
        }
    }

    public double insert(@Nullable String isotope, double bq, boolean simulate) {
        if (isotope == null || bq <= 0 || !model.accepts(isotope)) {
            return 0;
        }
        var resolved = IsotopeRegistry.get(isotope);
        if (resolved == null || resolved.halfLifeTicks() <= 0) {
            return 0;
        }
        double put = Math.min(bq, Math.max(0, model.capacity() - totalBq()));
        if (!simulate && put > 0) {
            var stack = stackFor(resolved);
            stack.setAtoms(stack.atoms() + atomsFor(resolved, put));
            changed.call();
        }
        return put;
    }

    public RadiationProfile profile(long gameTime) {
        var profile = new RadiationProfile();
        for (var stack : stored.values()) {
            profile.put(new IsotopeStack(stack.isotope(), stack.atoms(), gameTime));
        }
        return profile;
    }

    public List<Component> guiLines() {
        var lines = new ArrayList<Component>();
        if (displayBq.isEmpty()) {
            lines.add(Component.translatable("gui.mm.port.nuclear_radiation.empty"));
        } else {
            displayBq.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(GUI_LINES)
                    .forEach(entry -> lines.add(Component.translatable("gui.mm.port.nuclear_radiation.entry",
                            RadiationText.isotopeName(entry.getKey()), RadiationText.bq(entry.getValue()))));
        }
        double total = displayBq.values().stream().mapToDouble(Double::doubleValue).sum();
        lines.add(Component.translatable("gui.mm.port.nuclear_radiation.total",
                RadiationText.bq(total), RadiationText.bq(model.capacity())));
        return lines;
    }

    @Override
    public List<PortContent> contents() {
        return List.of(PortContent.gauge(Component.translatable("port.mm.nuclear_radiation.name"), 0x5DBB3A, (long) totalBq(), (long) model.capacity(), "Bq"));
    }

    @Override
    public List<Component> describeContents() {
        if (stored.isEmpty()) {
            return List.of(Component.translatable("jade.mm.port.empty"));
        }
        var lines = new ArrayList<Component>();
        stored.values().stream()
                .sorted(Comparator.comparingDouble(IsotopeStack::currentActivityBq).reversed())
                .forEach(stack -> lines.add(Component.translatable("jade.mm.port.radiation",
                        RadiationText.isotopeName(stack.isotope().id()), RadiationText.bq(stack.currentActivityBq()))));
        lines.add(Component.translatable("gui.mm.port.nuclear_radiation.total",
                RadiationText.bq(totalBq()), RadiationText.bq(model.capacity())));
        return lines;
    }

    @Override
    public void setupContainer(AbstractContainerMenu container, Inventory inv, PortModel portModel) {
        if (inputSide) {
            container.addSlot(new SlotItemHandler(items, 0, INPUT_SLOT_X + 1, SLOT_Y + 1));
        } else {
            container.addSlot(new SlotItemHandler(items, 0, CARRIER_SLOT_X + 1, SLOT_Y + 1));
            container.addSlot(new SlotItemHandler(items, 1, LOADED_SLOT_X + 1, SLOT_Y + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        IPortStorage.super.setupContainer(container, inv, portModel);
    }

    @Override
    public <T> @Nullable T getCapability(BlockCapability<T, ?> capability) {
        @SuppressWarnings("unchecked") var cast = (T) automation;
        return hasCapability(capability) ? cast : null;
    }

    @Override
    public <T> boolean hasCapability(BlockCapability<T, ?> capability) {
        return capability == MMCapabilities.ITEM;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        for (var stack : stored.values()) {
            var entry = new CompoundTag();
            entry.putString("Isotope", stack.isotope().id());
            entry.putDouble("Atoms", stack.atoms());
            entry.putLong("Time", stack.timestamp());
            entry.putDouble("Bq", stack.currentActivityBq());
            list.add(entry);
        }
        unresolved.forEach(entry -> list.add(entry.copy()));
        tag.put("Isotopes", list);
        tag.put("Slot", LegacyNbt.save(items, registries));
        tag.putInt("Priority", priority);
        return tag;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stored.clear();
        unresolved.clear();
        displayBq.clear();
        var list = tag.getListOrEmpty("Isotopes");
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompoundOrEmpty(i);
            var id = entry.getStringOr("Isotope", "");
            displayBq.merge(id, entry.getDoubleOr("Bq", 0), Double::sum);
            var isotope = IsotopeRegistry.get(id);
            if (isotope == null) {
                unresolved.add(entry.copy());
            } else {
                stored.put(id, new IsotopeStack(isotope, entry.getDoubleOr("Atoms", 0), entry.getLongOr("Time", 0L)));
            }
        }
        loading = true;
        try {
            for (int i = 0; i < items.getSlots(); i++) {
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
            if (tag.contains("Slot")) {
                var saved = new ItemStackHandler();
                LegacyNbt.load(saved, registries, tag.getCompoundOrEmpty("Slot"));
                for (int i = 0; i < Math.min(saved.getSlots(), items.getSlots()); i++) {
                    items.setStackInSlot(i, saved.getStackInSlot(i));
                }
            }
        } finally {
            loading = false;
        }
        priority = Math.max(0, Math.min(10, tag.getIntOr("Priority", 0)));
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
        dump.addProperty("totalBq", totalBq());
        var isotopes = new JsonArray();
        for (var stack : stored.values()) {
            var entry = new JsonObject();
            entry.addProperty("isotope", stack.isotope().id());
            entry.addProperty("atoms", stack.atoms());
            entry.addProperty("bq", stack.currentActivityBq());
            isotopes.add(entry);
        }
        dump.add("isotopes", isotopes);
        return dump;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = Math.max(0, Math.min(10, priority));
    }

    private class AutomationHandler implements IItemHandler, TransferSnapshot {
        @Override
        public Object createSnapshot() {
            var copies = new ItemStack[items.getSlots()];
            for (int i = 0; i < copies.length; i++) {
                copies[i] = items.getStackInSlot(i).copy();
            }
            return copies;
        }

        @Override
        public void restoreSnapshot(Object snapshot) {
            var copies = (ItemStack[]) snapshot;
            for (int i = 0; i < copies.length && i < items.getSlots(); i++) {
                items.setStackInSlot(i, copies[i].copy());
            }
        }

        @Override
        public int getSlots() {
            return inputSide ? 1 : 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? items.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return !inputSide && slot == 1 ? items.extractItem(1, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && items.isItemValid(0, stack);
        }
    }
}
